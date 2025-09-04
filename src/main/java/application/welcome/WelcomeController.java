// file: src/main/java/application/welcome/WelcomeController.java
package application.welcome;

import application.LocalStore;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class WelcomeController {
    @FXML private TextField nameField;
    @FXML private Button enterBtn;

    private Navigation navigation;
    public void setNavigation(Navigation navigation) { this.navigation = navigation; }

    @FXML private void initialize() {
        var p = LocalStore.getInstance().profile();
        if (p != null) nameField.setText(p.getUsername());

        enterBtn.setDefaultButton(true);
        enterBtn.setOnAction(e -> onEnter());
        nameField.setOnAction(e -> onEnter());
    }

    @FXML private void onEnter() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) name = "Player One";
        LocalStore.getInstance().profile().setUsername(name);
        LocalStore.getInstance().saveSafe();
        if (navigation != null) navigation.goToMain();
    }
}
