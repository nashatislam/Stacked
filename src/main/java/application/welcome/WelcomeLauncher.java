// src/main/java/application/welcome/WelcomeLauncher.java
package application.welcome;

import application.LocalStore;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/** Alternate launcher that also loads Welcome and wires Navigation. */
public class WelcomeLauncher extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try { LocalStore.getInstance().loadSafe(); } catch (Throwable ignore) { }

        var fxml = Objects.requireNonNull(
                getClass().getResource("/application/welcome/Welcome.fxml"),
                "Welcome.fxml missing"
        );
        FXMLLoader fx = new FXMLLoader(fxml);
        Parent root = fx.load();

        Scene scene = new Scene(root);
        var css = getClass().getResource("/application/welcome/welcome.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("Stacked – Welcome");
        stage.setScene(scene);
        stage.show();

        // NEW: 1-arg Navigation
        var nav = new Navigation(stage);
        Object controller = fx.getController();
        try {
            controller.getClass().getMethod("setNavigation", Navigation.class).invoke(controller, nav);
        } catch (NoSuchMethodException nsme) {
            try { controller.getClass().getMethod("setNavigator", Navigation.class).invoke(controller, nav); }
            catch (NoSuchMethodException ignored) { /* no-op */ }
        }
    }

    public static void main(String[] args) { launch(args); }
}
