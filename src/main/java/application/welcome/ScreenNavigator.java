package application.welcome;

import java.io.File;


public interface ScreenNavigator {
    void goToMain();
    void goToSettings();
    void importSave(File file);
    void exitApp();
}