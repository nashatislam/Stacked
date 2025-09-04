package application.theme;

import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.Region;


public final class Responsive {
    private Responsive() {}

    // Baseline window we designed against (tweak if you prefer)
    private static final double BASE_W = 1280.0;
    private static final double BASE_H = 800.0;
    private static final double BASE_FONT = 14.0;  // px at baseline

    private static final double MIN_FONT = 12.0;   // clamp so it never gets too small
    private static final double MAX_FONT = 22.0;   // nor too big

    public static void attach(Scene scene) {
        if (scene == null) return;

        ChangeListener<Number> relayout = (obs, a, b) -> apply(scene);
        scene.widthProperty().addListener(relayout);
        scene.heightProperty().addListener(relayout);
        apply(scene); // initial
    }

    private static void apply(Scene scene) {
        if (!(scene.getRoot() instanceof Region root)) return;

        double kx = scene.getWidth()  > 0 ? scene.getWidth()  / BASE_W : 1.0;
        double ky = scene.getHeight() > 0 ? scene.getHeight() / BASE_H : 1.0;
        double k  = Math.min(kx, ky); // keep aspect
        double px = clamp(BASE_FONT * k, MIN_FONT, MAX_FONT);


        root.setStyle(String.format("-fx-font-size: %.2fpx;", px));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
