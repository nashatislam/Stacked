package application.realm;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;


public final class NodeCoin extends StackPane {
    private static final double DIAMETER    = 56.0;  // visual size
    private static final double RING_STROKE = 3.0;

    private final Circle base  = new Circle();
    private final Circle ring  = new Circle();
    private final ImageView glyph;

    public NodeCoin(Image glyphImage) {
        getStyleClass().add("node-coin");
        setFocusTraversable(true);
        setPickOnBounds(false);


        base.setFill(Color.web("#ffffff"));
        base.setEffect(new DropShadow(8, Color.color(0,0,0,0.25)));


        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#6D28D9"));
        ring.setStrokeWidth(RING_STROKE);


        glyph = new ImageView(glyphImage);
        glyph.setPreserveRatio(true);
        glyph.setSmooth(true);
        glyph.setMouseTransparent(true); // clicks fall through to the coin

        getChildren().addAll(base, ring, glyph);

        setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setPrefSize(DIAMETER, DIAMETER);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        widthProperty().addListener((o,a,b)->requestLayout());
        heightProperty().addListener((o,a,b)->requestLayout());
    }

    @Override
    protected void layoutChildren() {
        double d  = Math.min(getWidth(), getHeight());   // coin diameter on screen
        double cx = d / 2.0, cy = d / 2.0;


        ring.setRadius((d - RING_STROKE) / 2.0);
        ring.setCenterX(cx); ring.setCenterY(cy);


        base.setRadius(ring.getRadius() - 3);
        base.setCenterX(cx); base.setCenterY(cy);


        double baseDiameter = base.getRadius() * 2.0;
        double inset        = Math.max(2.0, d * 0.02);      // ~2% or at least 2px
        double glyphSize    = Math.max(0, baseDiameter - inset * 2);

        glyph.setFitWidth(glyphSize);
        glyph.setFitHeight(glyphSize);

        layoutInArea(glyph,
                (d - glyphSize) / 2.0,
                (d - glyphSize) / 2.0,
                glyphSize, glyphSize, 0,
                HPos.CENTER, VPos.CENTER);
    }
}
