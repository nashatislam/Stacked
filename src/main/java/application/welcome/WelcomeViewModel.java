package application.welcome;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Minimal VM: holds username field and start-button enablement. */
public class WelcomeViewModel {
    private final StringProperty username = new SimpleStringProperty("");
    private final BooleanBinding canStart = Bindings.createBooleanBinding(
            () -> username.get() != null && !username.get().trim().isEmpty(),
            username
    );

    public StringProperty usernameProperty() { return username; }
    public String getUsername() { return username.get(); }
    public void setUsername(String value) { username.set(value); }
    public BooleanBinding canStartProperty() { return canStart; }
}