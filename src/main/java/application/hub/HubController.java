package application.hub;

import application.Realm;
import application.services.ProfileService;
import application.services.ProgressService;
import application.services.QuickActionService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.EnumMap;
import java.util.Map;

public class HubController {
    @FXML private Label hiLabel;
    @FXML private Label levelPill;
    @FXML private FlowPane cardsPane;
    @FXML private VBox rightPanel;
    @FXML private Button quickStudyBtn;
    @FXML private Button quickHydrateBtn;
    @FXML private Button quickBreakBtn;

    private final HubViewModel vm = new HubViewModel();

    // Injected
    private ProfileService profileService;
    private ProgressService progressService;
    private QuickActionService quickActionService;
    private NavDelegate nav;

    public interface NavDelegate { void openRealm(Realm realm); }

    public void init(ProfileService ps, ProgressService prog, QuickActionService qa, NavDelegate n) {
        this.profileService = ps; this.progressService = prog; this.quickActionService = qa; this.nav = n;
        refresh();
    }

    @FXML private void initialize() {
        cardsPane.setHgap(14); cardsPane.setVgap(14);
        cardsPane.setPadding(new Insets(10));
        quickStudyBtn.setOnAction(e -> { if (quickActionService!=null){ quickActionService.study25(); refresh(); showToast("Logged Study 25m"); }});
        quickHydrateBtn.setOnAction(e -> { if (quickActionService!=null){ quickActionService.hydrateQuick(); refresh(); showToast("Hydration logged"); }});
        quickBreakBtn.setOnAction(e -> { if (quickActionService!=null){ quickActionService.breakQuick(); refresh(); showToast("Break logged"); }});
    }

    private void refresh() {
        if (profileService != null) hiLabel.setText("Hi, " + profileService.getUsername());
        cardsPane.getChildren().clear();
        if (progressService != null) {
            for (var s : progressService.getAllRealmSummaries()) {
                cardsPane.getChildren().add(createCard(s));
            }
        }
    }

    private Node createCard(ProgressService.RealmSummary s) {
        VBox card = new VBox(8);
        card.getStyleClass().add("realm-card");
        card.setPadding(new Insets(12));
        Label name = new Label(s.realm().display());
        name.getStyleClass().add("realm-name");
        ProgressIndicator ring = new ProgressIndicator();
        ring.setMaxSize(64,64);
        double denom = Math.max(1, s.xpInto()+s.xpToNext());
        ring.setProgress(Math.min(1.0, s.xpInto()/denom));
        ring.setTooltip(new Tooltip("Next unlock in " + s.xpToNext() + " XP"));
        Label lvl = new Label("Lv."+s.level());
        lvl.getStyleClass().add("realm-level");
        Button open = new Button("Open");
        open.getStyleClass().add("btn-primary");
        open.setOnAction(e -> { if (nav!=null) nav.openRealm(s.realm()); });
        card.getChildren().addAll(name, ring, lvl, open);
        return card;
    }

    private void showToast(String msg) {
        Label t = new Label(msg);
        t.getStyleClass().add("toast");
        rightPanel.getChildren().add(0, t);
        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.2)).setOnFinished(ev -> rightPanel.getChildren().remove(t));
        var pt = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.2));
        pt.setOnFinished(ev -> rightPanel.getChildren().remove(t));
        pt.play();
    }
}
