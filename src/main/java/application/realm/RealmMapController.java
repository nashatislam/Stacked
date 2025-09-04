package application.realm;

import application.LocalStore;
import application.Realm;
import application.services.RealmLayoutService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleRole;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class RealmMapController {

    @FXML private Button backBtn;
    @FXML private Label titleLabel;
    @FXML private StackPane mapRoot;   // container
    @FXML private ImageView mapView;   // background map image
    @FXML private Canvas bgCanvas;     // gradient + vignette + noise
    @FXML private Canvas pathCanvas;   // dotted path
    @FXML private Pane nodesLayer;     // coins
    @FXML private Pane labelsLayer;    // label pills

    public interface NavDelegate { void backToHub(); }
    public interface NodeSelectionHandler { void onNodeSelected(String nodeId); }

    /** Optional: allow app to customize how links open. */
    public interface ResourceOpener { void open(String url); }

    private ResourceOpener opener = url -> {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                System.out.println("Open URL (no Desktop): " + url);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Couldn't open link:\n" + url);
            a.setHeaderText("Open Link Failed");
            a.showAndWait();
        }
    };

    public void setResourceOpener(ResourceOpener custom) {
        if (custom != null) this.opener = custom;
    }

    private static final boolean REDUCED_MOTION = Boolean.getBoolean("stacked.reducedMotion");
    private static final double MIN_CONTENT_SIZE = 50.0; // Minimum size for content area

    private RealmLayoutService layoutService;
    private NavDelegate nav;
    private NodeSelectionHandler onSelect = id -> System.out.println("NODE_SELECTED: " + id);

    private Image nodeImage;
    private final List<NodeHolder> nodeHolders = new ArrayList<>();
    private final Random noiseRng = new Random(42);
    private javafx.scene.image.Image noiseTile;
    private final Timeline dashAnim = new Timeline();
    private double dashOffset = 0;

    public void init(Realm realm, RealmLayoutService layoutService, NavDelegate nav) {
        this.layoutService = Objects.requireNonNull(layoutService);
        this.nav = nav;
        titleLabel.setText(realm.display() + " – Map");
        loadLayout(realm);
    }

    public void setSelectionHandler(NodeSelectionHandler handler) {
        if (handler != null) this.onSelect = handler;
    }

    @FXML
    private void initialize() {
        // Background map sizing (letterboxed, no distortion)
        mapView.setPreserveRatio(true);
        mapView.setSmooth(true);
        mapView.fitWidthProperty().bind(mapRoot.widthProperty());
        mapView.fitHeightProperty().bind(mapRoot.heightProperty());

        // Keep canvases sized with container
        bindCanvas(bgCanvas);
        bindCanvas(pathCanvas);

        backBtn.setOnAction(e -> { if (nav != null) nav.backToHub(); });

        // Repaint/relayout when anything meaningful changes.
        ChangeListener<Number> relayout = (obs, a, b) -> {
            layoutNodes();
            paintPath();
            paintBackground();
        };
        mapRoot.widthProperty().addListener(relayout);
        mapRoot.heightProperty().addListener(relayout);

        // When the ImageView's laid-out bounds change (includes first layout and letterboxing),
        // recompute using the actual displayed rectangle.
        mapView.boundsInParentProperty().addListener((o, a, b) -> {
            layoutNodes();
            paintPath();
        });

        // When the root gets a Scene, run once after first layout pass (fixes "starts in corner until resize").
        mapRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    layoutNodes();
                    paintBackground();
                    paintPath();
                    refreshNodeStatesByXp(); // ensure styles reflect current XP on first show
                });
            }
        });

        // Also react when layout bounds change (Tab collapse/expand, split pane drag, etc.)
        mapRoot.layoutBoundsProperty().addListener((o, a, b) -> {
            layoutNodes();
            paintPath();
            paintBackground();
        });

        // If the image itself is swapped/loaded later, relayout.
        mapView.imageProperty().addListener((obs, old, img) -> {
            layoutNodes();
            paintPath();
        });

        if (!REDUCED_MOTION) {
            dashAnim.getKeyFrames().setAll(new KeyFrame(Duration.seconds(0.016), e -> {
                dashOffset = (dashOffset + 0.9) % 20; // slow drift
                paintPath();
            }));
            dashAnim.setCycleCount(Animation.INDEFINITE);
            dashAnim.play();
        }
    }

    private void bindCanvas(Canvas c) {
        c.widthProperty().bind(mapRoot.widthProperty());
        c.heightProperty().bind(mapRoot.heightProperty());
        c.widthProperty().addListener((o,a,b)-> { if (c==bgCanvas) paintBackground(); else paintPath(); });
        c.heightProperty().addListener((o,a,b)-> { if (c==bgCanvas) paintBackground(); else paintPath(); });
    }

    private void loadLayout(Realm realm) {
        var layout = layoutService.loadLayout(realm);
        if (layout.mapUrl() != null) mapView.setImage(new Image(layout.mapUrl().toExternalForm(), true));
        nodeImage = layout.nodeUrl() != null ? new Image(layout.nodeUrl().toExternalForm(), true) : null;

        nodesLayer.getChildren().clear();
        labelsLayer.getChildren().clear();
        nodeHolders.clear();

        for (var spec : layout.nodes()) {
            NodeCoin coin = new NodeCoin(nodeImage);
            coin.getStyleClass().add(spec.styleClass() == null ? "node-unlocked" : spec.styleClass());
            coin.setAccessibleRole(AccessibleRole.BUTTON);
            coin.setAccessibleText(spec.title());

            // OPEN on click / keyboard
            NodeHolder holder = new NodeHolder(spec.id(), spec.x(), spec.y(), coin, spec);
            coin.setOnMouseClicked(e -> onActivate(holder));
            coin.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) onActivate(holder);
            });

            if (spec.title() != null)
                Tooltip.install(coin, new Tooltip(spec.title() + (spec.tooltip()!=null? "\n" + spec.tooltip() : "")));

            // Label pill (hover/focus)
            Label pill = new Label(spec.title() + (spec.tooltip()!=null? " – " + spec.tooltip() : ""));
            pill.getStyleClass().add("node-label");
            pill.setManaged(false);
            pill.setVisible(false);
            coin.hoverProperty().addListener((o,was,is)-> pill.setVisible(is || coin.isFocused()));
            coin.focusedProperty().addListener((o,was,is)-> pill.setVisible(is || coin.isHover()));

            nodesLayer.getChildren().add(coin);
            labelsLayer.getChildren().add(pill);
            holder.label = pill;
            nodeHolders.add(holder);
        }

        // Initial pass (in case everything is already laid out)
        Platform.runLater(() -> {
            layoutNodes();
            paintBackground();
            paintPath();
            refreshNodeStatesByXp();
        });
    }

    /** Open the resource if unlocked; otherwise show why it’s locked. Also forwards event to onSelect. */
    private void onActivate(NodeHolder h) {
        boolean unlocked = isUnlocked(h);
        if (!unlocked) {
            String msg = (h.spec.tooltip()!=null) ? h.spec.tooltip() : ("Unlock at " + h.spec.thresholdXp() + " XP");
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
            a.setHeaderText("Locked");
            a.showAndWait();
            return;
        }
        if (h.spec.url() != null && !h.spec.url().isBlank()) {
            opener.open(h.spec.url());
        }
        onSelect.onNodeSelected(h.id); // keep your callback behavior
    }

    /** Unlocked if visual state isn't locked OR XP >= threshold. */
    private boolean isUnlocked(NodeHolder h) {
        var sc = h.coin.getStyleClass();
        if (!sc.contains("node-locked") && !sc.contains("node-next")) return true;
        int xp = 0;
        try { xp = LocalStore.getInstance().profile().getTotalXp(); } catch (Throwable ignored) {}
        return xp >= h.spec.thresholdXp();
    }

    /** Refresh style classes based on current XP: first locked → node-next, others locked → node-locked, reached → node-unlocked. */
    private void refreshNodeStatesByXp() {
        int xp = 0;
        try { xp = LocalStore.getInstance().profile().getTotalXp(); } catch (Throwable ignored) {}

        boolean nextAssigned = false;
        for (var h : nodeHolders) {
            var sc = h.coin.getStyleClass();
            sc.removeAll("node-locked","node-next","node-unlocked","node-claimed");

            if (xp >= h.spec.thresholdXp()) {
                sc.add("node-unlocked");
            } else if (!nextAssigned) {
                sc.add("node-next");
                nextAssigned = true;
            } else {
                sc.add("node-locked");
            }
        }
        paintPath();
    }

    private void layoutNodes() {
        double[] rect = contentRect();
        if (rect == null) return;
        double offX = rect[0], offY = rect[1], cw = rect[2], ch = rect[3];

        for (var h : nodeHolders) {
            double nodeW = h.coin.getWidth()  > 0 ? h.coin.getWidth()  : h.coin.prefWidth(-1);
            double nodeH = h.coin.getHeight() > 0 ? h.coin.getHeight() : h.coin.prefHeight(-1);
            double x = offX + h.x * cw - nodeW / 2.0;
            double y = offY + h.y * ch - nodeH / 2.0;

            // Keep within visible bounds
            x = Math.max(0, Math.min(mapRoot.getWidth() - nodeW, x));
            y = Math.max(0, Math.min(mapRoot.getHeight() - nodeH, y));

            h.coin.relocate(x, y);

            // Label position (avoid clipping)
            if (h.label != null) {
                double labelX = x + nodeW/2 + 8;
                double labelY = y - 8;

                if (labelX + h.label.getWidth() > mapRoot.getWidth()) {
                    labelX = x - h.label.getWidth() - 8;
                }
                if (labelY < 0) {
                    labelY = y + nodeH + 8;
                }
                h.label.relocate(labelX, labelY);
            }
        }
    }

    private void paintBackground() {
        GraphicsContext g = bgCanvas.getGraphicsContext2D();
        double w = bgCanvas.getWidth(), h = bgCanvas.getHeight();
        g.clearRect(0,0,w,h);

        // Radial water gradient
        RadialGradient water = new RadialGradient(0, 0, 0.5, 0.5, 0.9, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0b1530")),
                new Stop(0.65, Color.web("#0e1a3a")),
                new Stop(1, Color.web("#0a1228")));
        g.setFill(water);
        g.fillRect(0,0,w,h);

        // Soft vignette
        RadialGradient vignette = new RadialGradient(0, 0, 0.5, 0.5, 1.0, true, CycleMethod.NO_CYCLE,
                new Stop(0.6, Color.TRANSPARENT),
                new Stop(1.0, Color.color(0,0,0,0.38)));
        g.setFill(vignette);
        g.fillRect(0,0,w,h);

        // Optional subtle noise
        if (noiseTile == null) noiseTile = makeNoiseTile(128, 128, noiseRng);
        g.setGlobalAlpha(0.08);
        for (int yy = 0; yy < h; yy += (int)noiseTile.getHeight())
            for (int xx = 0; xx < w; xx += (int)noiseTile.getWidth())
                g.drawImage(noiseTile, xx, yy);
        g.setGlobalAlpha(1);
    }

    private Image makeNoiseTile(int w, int h, Random rng) {
        var img = new javafx.scene.image.WritableImage(w, h);
        var pw = img.getPixelWriter();
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int v = 230 + rng.nextInt(25); // very light
            pw.setColor(x,y, Color.rgb(v,v,v));
        }
        return img;
    }

    private void paintPath() {
        GraphicsContext g = pathCanvas.getGraphicsContext2D();
        double w = pathCanvas.getWidth(), h = pathCanvas.getHeight();
        g.clearRect(0,0,w,h);
        if (nodeHolders.size() < 2) return;

        double[] rect = contentRect();
        if (rect == null) return;

        double offX = rect[0], offY = rect[1], cw = rect[2], ch = rect[3];

        g.setLineWidth(3);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineDashes(10, 10);

        for (int i=0; i<nodeHolders.size()-1; i++) {
            NodeHolder a = nodeHolders.get(i), b = nodeHolders.get(i+1);
            double x1 = offX + a.x * cw, y1 = offY + a.y * ch;
            double x2 = offX + b.x * cw, y2 = offY + b.y * ch;

            // Ensure path coordinates are within bounds
            x1 = Math.max(0, Math.min(w, x1));
            y1 = Math.max(0, Math.min(h, y1));
            x2 = Math.max(0, Math.min(w, x2));
            y2 = Math.max(0, Math.min(h, y2));

            var classes = b.coin.getStyleClass();
            if (classes.contains("node-next")) {
                g.setStroke(Color.web("#6D28D9", 0.95));
                g.save();
                g.setLineDashOffset(REDUCED_MOTION ? 0 : dashOffset);
                g.strokeLine(x1,y1,x2,y2);
                g.restore();
            } else if (classes.contains("node-locked")) {
                g.setStroke(Color.color(1,1,1,0.25));
                g.strokeLine(x1,y1,x2,y2);
            } else { // completed / unlocked / claimed
                g.setStroke(Color.color(1,1,1,0.70));
                g.strokeLine(x1,y1,x2,y2);
            }
        }
    }

    /**
     * Returns [offX, offY, contentW, contentH] of the actually displayed (letterboxed) map.
     * Using the ImageView's bounds fixes: initial layout, cut-offs on collapse/expand, live resizing.
     */
    private double[] contentRect() {
        Bounds b = mapView.getBoundsInParent();
        if (b == null || b.getWidth() <= 0 || b.getHeight() <= 0) return null;

        double m = safeMargin();
        double x = b.getMinX() + m;
        double y = b.getMinY() + m;
        double w = Math.max(0, b.getWidth()  - 2*m);
        double h = Math.max(0, b.getHeight() - 2*m);

        // If the container is extremely small, use minimum content size
        if (w < MIN_CONTENT_SIZE || h < MIN_CONTENT_SIZE) {
            double centerX = b.getMinX() + b.getWidth() / 2;
            double centerY = b.getMinY() + b.getHeight() / 2;
            return new double[]{
                    centerX - MIN_CONTENT_SIZE / 2,
                    centerY - MIN_CONTENT_SIZE / 2,
                    MIN_CONTENT_SIZE,
                    MIN_CONTENT_SIZE
            };
        }

        return new double[]{x, y, w, h};
    }

    private double safeMargin() {
        double coinSize = 0;
        if (!nodeHolders.isEmpty()) {
            var c = nodeHolders.get(0).coin;
            coinSize = c.getWidth() > 0 ? c.getWidth() : c.prefWidth(-1);
        }
        if (coinSize <= 0) coinSize = 48; // default visual size
        return Math.ceil(coinSize / 2.0 + 3); // glow/focus ring padding
    }

    private static final class NodeHolder {
        final String id; final double x; final double y; final NodeCoin coin;
        final RealmLayoutService.NodeSpec spec;
        Label label;
        NodeHolder(String id, double x, double y, NodeCoin coin, RealmLayoutService.NodeSpec spec) {
            this.id=id; this.x=x; this.y=y; this.coin=coin; this.spec = spec;
        }
    }
}
