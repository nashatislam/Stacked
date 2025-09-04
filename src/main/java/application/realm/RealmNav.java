// file: src/main/java/application/realm/RealmNav.java
package application.realm;

import application.Realm;
import application.services.ClasspathRealmLayoutBridge;
import application.services.RealmLayoutService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/** Small, UI-only navigator for opening the Realm Map from anywhere in the app. */
public final class RealmNav {

    private RealmNav() { }

    public static void openMap(Stage stage, Realm realm) {
        try {
            var fxml = Objects.requireNonNull(
                    RealmNav.class.getResource("/application/realm/RealmMap.fxml"),
                    "RealmMap.fxml missing"
            );
            FXMLLoader fx = new FXMLLoader(fxml);
            Parent root = fx.load();

            RealmMapController controller = fx.getController();
            RealmLayoutService layout = new ClasspathRealmLayoutBridge();

            // Back goes to main.fxml
            RealmMapController.NavDelegate back = () -> {
                try {
                    var mainUrl = Objects.requireNonNull(
                            RealmNav.class.getResource("/application/main.fxml"),
                            "main.fxml missing"
                    );
                    FXMLLoader mainFx = new FXMLLoader(mainUrl);
                    Parent mainRoot = mainFx.load();
                    Scene mainScene = new Scene(mainRoot);
                    URL css = RealmNav.class.getResource("/application/styles.css");
                    if (css != null) mainScene.getStylesheets().add(css.toExternalForm());
                    stage.setScene(mainScene);
                    stage.centerOnScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            controller.init(realm, layout, back);

            Scene scene = new Scene(root);
            URL css = RealmNav.class.getResource("/application/realm/realm-map.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
