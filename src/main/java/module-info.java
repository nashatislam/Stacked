module application {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.desktop; // Desktop.browse for resource links

    // Expose base package
    opens application to javafx.fxml, com.google.gson;
    exports application;

    // FXML controllers
    opens application.welcome to javafx.fxml;
    opens application.realm to javafx.fxml;
    opens application.hub to javafx.fxml;

    // Public APIs for services and views
    exports application.services;
    exports application.welcome;
    exports application.realm;
    exports application.hub;
}