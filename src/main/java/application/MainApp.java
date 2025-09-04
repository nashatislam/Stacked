// src/main/java/application/MainApp.java
package application;

import application.theme.Responsive;
import application.welcome.Navigation;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Screen;   // <-- needed
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class MainApp extends Application {

    /** Attach global styles to any scene (idempotent). */
    private static void applyTheme(Scene scene) {
        URL appCss = MainApp.class.getResource("/application/theme/app.css");
        if (appCss != null) {
            String css = appCss.toExternalForm();
            if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
        }
        // Keep realm styles present so map screens look right when navigated to.
        URL realmCss = MainApp.class.getResource("/application/realm/realm.css");
        if (realmCss != null) {
            String css = realmCss.toExternalForm();
            if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            Font.loadFont(getClass().getResourceAsStream("/fonts/HelveticaNeue-Medium.otf"), 12);
            Font.loadFont(getClass().getResourceAsStream("/fonts/HelveticaNeue-Light.otf"), 12);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Medium.otf"), 12);
        } catch (Throwable ignored) {}

        try { LocalStore.getInstance().loadSafe(); } catch (Throwable ignore) { }

        var fxml = Objects.requireNonNull(
                MainApp.class.getResource("/application/welcome/Welcome.fxml"),
                "Welcome.fxml not found at /application/welcome/Welcome.fxml");
        var loader = new FXMLLoader(fxml);
        Parent root = loader.load();

        Scene scene = new Scene(root);
        applyTheme(scene);

        // --- Responsive + sensible initial size ---
        Responsive.attach(scene); // make it responsive
        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true); // start maximized

        stage.setTitle("Stacked – CS Habits Gamified");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.show();

        var nav = new Navigation(stage);
        Object controller = loader.getController();
        try {
            controller.getClass().getMethod("setNavigation", Navigation.class)
                    .invoke(controller, nav);
        } catch (NoSuchMethodException nsme) {
            try {
                controller.getClass().getMethod("setNavigator", Navigation.class)
                        .invoke(controller, nav);
            } catch (NoSuchMethodException ignored) {}
        }
    }

    @Override
    public void stop() {
        try { LocalStore.getInstance().saveSafe(); } catch (Throwable ignore) { }
    }

    public static void main(String[] args) { launch(args); }
}
