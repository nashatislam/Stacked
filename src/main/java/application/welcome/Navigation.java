// src/main/java/application/welcome/Navigation.java
package application.welcome;

import application.MainController;
import application.Realm;
import application.realm.RealmMapController;
import application.services.ClasspathRealmLayoutBridge;
import application.services.RealmLayoutService;
import application.theme.Responsive;     // <- import
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Navigation implements RealmMapController.NavDelegate {

    private final Stage stage;
    private final RealmLayoutService realmLayouts = new ClasspathRealmLayoutBridge();

    private static final String APP_CSS = "/application/theme/app.css";
    private static final String MAP_CSS = "/application/realm/map.css";

    public Navigation(Stage stage) {
        this.stage = stage;
    }

    private Scene ensureScene(Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        // keep screen maximized on every screen we show
        var b = Screen.getPrimary().getVisualBounds();
        stage.setX(b.getMinX());
        stage.setY(b.getMinY());
        stage.setWidth(b.getWidth());
        stage.setHeight(b.getHeight());
        stage.setMaximized(true);

        // re-attach responsive scaling after root replacement
        Responsive.attach(scene);
        return scene;
    }

    private void applyTheme(Scene scene, String... extras) {
        List<String> list = new ArrayList<>();
        URL app = getClass().getResource(APP_CSS);
        if (app != null) list.add(app.toExternalForm());
        if (extras != null) {
            for (String p : extras) {
                URL u = getClass().getResource(p);
                if (u != null) list.add(u.toExternalForm());
            }
        }
        scene.getStylesheets().setAll(list);
    }

    public void goToMain() {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/application/main.fxml"));
            Parent root = fx.load();
            Scene scene = ensureScene(root);
            applyTheme(scene);
            stage.setTitle("Stacked – CS Habits Gamified");
            MainController controller = fx.getController();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load /application/main.fxml", e);
        }
    }

    public void openRealm(Realm realm) {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/application/realm/RealmMap.fxml"));
            Parent root = fx.load();
            Scene scene = ensureScene(root);
            applyTheme(scene, MAP_CSS);
            stage.setTitle(realm.display() + " – Map");

            RealmMapController ctl = fx.getController();
            ctl.init(realm, realmLayouts, this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load realm map", e);
        }
    }

    @Override public void backToHub() { goToMain(); }
}
