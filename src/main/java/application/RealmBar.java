package application;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class RealmBar extends HBox {
    private final Realm realm;
    private final Label name = new Label();
    private final ProgressBar bar = new ProgressBar(0);
    private final Label level = new Label();

    public RealmBar(Realm realm) {
        this.realm = realm;
        setSpacing(10);
        setPadding(new Insets(6, 10, 6, 10));
        getStyleClass().add("realm-bar");
        name.setText("" + switch (realm) {
            case MEMORY -> "🧠 " + realm.display();
            case STACK -> "📚 " + realm.display();
            case COOLING -> "💧 " + realm.display();
            case ALGORITHMS -> "🧩 " + realm.display();
            case SYSTEMS -> "🛠️ " + realm.display();
        });
        bar.setPrefWidth(220);
        getChildren().addAll(name, bar, level);
        refresh();
    }

    public void refresh() {
        int xp = LocalStore.getInstance().profile().getRealmXp(realm);
        int lvl = XpService.levelForXp(xp);
        int into = XpService.xpIntoLevel(xp);
        int toNext = XpService.xpToNextLevel(xp);
        int denom = Math.max(1, into + toNext);
        level.setText(toNext == 0 ? ("Lv." + lvl + " (MAX)") : ("Lv." + lvl + " (" + into + "/" + denom + ")"));
        double pct = Math.min(1.0, denom == 0 ? 1.0 : (into / (double) denom));
        bar.setProgress(pct);
        FadeTransition ft = new FadeTransition(Duration.millis(300), bar);
        ft.setFromValue(0.6); ft.setToValue(1.0); ft.play();
    }
}