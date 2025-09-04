package application.welcome;

import java.io.File;

/** Minimal navigation contract so UI can be domain-agnostic. */
public interface ScreenNavigator {
    void goToMain();
    void goToSettings();
    void importSave(File file);
    void exitApp();
}