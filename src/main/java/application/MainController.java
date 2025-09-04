// file: src/main/java/application/MainController.java
package application;

import application.realm.RealmNav;
import application.services.AvatarService;
import application.services.BadgeService;
import application.services.ClasspathAvatarService;
import application.services.LocalBadgeBridge;
import javafx.animation.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;



public class MainController {

    // Header
    @FXML private Label usernameLabel;
    @FXML private Label levelLabel;
    @FXML private ProgressBar headerXpBar;      // new ids in main.fxml
    @FXML private Label headerXpLabel;

    // Stats tab progress (separate controls)
    @FXML private ProgressBar statsXpBar;
    @FXML private Label statsXpLabel;

    // Left column
    @FXML private FlowPane realmsPane;
    @FXML private ComboBox<Realm> realmPickSide;
    @FXML private Button openMapBtnSide;

    // Task toolbar (right column)
    @FXML private ComboBox<TaskType> taskTypeBox;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private Button addBtn;

    // Table
    @FXML private TableView<TaskEntry> historyTable;
    @FXML private TableColumn<TaskEntry, String> colType;
    @FXML private TableColumn<TaskEntry, Integer> colAmount;
    @FXML private TableColumn<TaskEntry, String> colWhen;

    // Badges & Avatar
    @FXML private ListView<String> badgesList;
    @FXML private VBox avatarBox;
    @FXML private ImageView avatarView;

    // Optional streak labels
    @FXML private Label streakDaysLabel;
    @FXML private Label streakMultiplierLabel;
    @FXML private Label streakNextLabel;

    private final ObservableList<TaskEntry> entriesObs = FXCollections.observableArrayList();
    private final XpService xpService = new XpService();

    // Services
    private AvatarService avatarService = new ClasspathAvatarService();
    private BadgeService badgeService = new LocalBadgeBridge();

    private static final DateTimeFormatter WHEN_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    // smooth bar animation guard
    private double lastHeaderProgress = 0.0;

    /** Optional DI from Navigation */
    public void init(AvatarService avatarService, BadgeService badgeService) {
        if (avatarService != null) this.avatarService = avatarService;
        if (badgeService != null) this.badgeService = badgeService;
        refreshAvatar();
    }

    @FXML
    public void initialize() {
        // Build realm chips
        for (Realm r : Realm.values()) {
            RealmBar chip = new RealmBar(r);
            chip.getStyleClass().add("realm-chip");
            realmsPane.getChildren().add(chip);
        }
        realmsPane.setHgap(12);
        realmsPane.setVgap(12);

        // Left-side realm picker + open
        if (realmPickSide != null) {
            realmPickSide.getItems().setAll(Realm.values());
            realmPickSide.getSelectionModel().select(Realm.ALGORITHMS);
        }
        if (openMapBtnSide != null) {
            openMapBtnSide.setOnAction(e -> openSelectedRealmMap());
        }

        // Task controls
        taskTypeBox.getItems().setAll(TaskType.values());
        taskTypeBox.getSelectionModel().selectFirst();
        datePicker.setValue(LocalDate.now());
        amountField.setText("30");
        addBtn.getStyleClass().addAll("btn", "fab");
        addBtn.setText("+");
        addBtn.setOnAction(e -> onAddTask());

        // Table wiring
        colType.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getType().name()));
        colAmount.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getAmount()));
        colWhen.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(WHEN_FMT.format(data.getValue().getWhen())));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setText(null); return; }
                String icon = switch (type) {
                    case "STUDY"     -> "📚";
                    case "HYDRATION" -> "💧";
                    case "BREAK"     -> "☕";
                    default -> "•";
                };
                setText(icon + "  " + type);
            }
        });
        historyTable.setItems(entriesObs);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.getStyleClass().add("dense-table");
        historyTable.setPlaceholder(new Label("No activity yet — log your first task!"));

        // Load data
        entriesObs.setAll(LocalStore.getInstance().entries());

        // Ensure all catalog badges exist and evaluate once against history
        badgeService.ensureDefaults();
        badgeService.evaluateAll();
        refreshBadges();

        // Initial UI
        refreshHeader();
        refreshRealms();
        ensureAvatarView();
        refreshAvatar();
        refreshStreakUiSafe();

        // keep theme applied if scene swaps
        usernameLabel.sceneProperty().addListener((obs, o, s) -> { if (s != null) ensureAppTheme(s); });
        if (usernameLabel.getScene() != null) ensureAppTheme(usernameLabel.getScene());
    }

    /* -------------------- Actions -------------------- */

    private void onAddTask() {
        try {
            int amount = Integer.parseInt(amountField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();
            LocalDate date = datePicker.getValue();
            LocalTime now   = LocalTime.now().withSecond(0).withNano(0);
            LocalDateTime when = LocalDateTime.of(date, now);
            TaskEntry entry = new TaskEntry(taskTypeBox.getValue(), amount, when);


            LocalStore.getInstance().addEntry(entry);
            entriesObs.add(entry);


            xpService.applyEntry(entry);
            badgeService.evaluateAll();

            refreshHeader();
            refreshRealms();
            refreshAvatar();
            refreshBadges();
            bounceAvatar();

            LocalStore.getInstance().saveSafe();
        } catch (NumberFormatException ex) {
            showAlert("Please enter a positive number for amount.");
        }
    }

    private void openSelectedRealmMap() {
        Realm realm = (realmPickSide != null && realmPickSide.getValue() != null)
                ? realmPickSide.getValue() : Realm.ALGORITHMS;
        Stage stage = (Stage) usernameLabel.getScene().getWindow();
        RealmNav.openMap(stage, realm);
    }

    /* -------------------- Refresh helpers -------------------- */

    private void refreshHeader() {
        Profile p = LocalStore.getInstance().profile();

        int into   = Math.max(0, XpService.xpIntoLevel(p.getTotalXp()));
        int toNext = Math.max(0, XpService.xpToNextLevel(p.getTotalXp()));
        int denom  = Math.max(1, into + (toNext > 0 ? toNext : 1));
        double progress = Math.min(1.0, Math.max(0.0, (double) into / denom));

        usernameLabel.setText(p.getUsername());
        levelLabel.setText("Level " + p.level());

        headerXpLabel.setText(into + "/" + denom + " XP");
        animateProgress(headerXpBar, lastHeaderProgress, progress);
        lastHeaderProgress = progress;

        // mirror to Stats tab controls if present
        if (statsXpBar != null && statsXpLabel != null) {
            statsXpBar.setProgress(progress);
            statsXpLabel.setText(into + "/" + denom + " XP");
        }
    }

    private void refreshRealms() {
        if (realmsPane == null) return;
        for (var node : realmsPane.getChildren()) {
            if (node instanceof RealmBar rb) rb.refresh();
        }
    }

    private void ensureAvatarView() {
        if (avatarView == null) {
            avatarView = new ImageView();
            avatarView.setPreserveRatio(true);
            avatarView.setFitWidth(180);
            avatarView.setFitHeight(180);
        }
        if (avatarBox != null && !avatarBox.getChildren().contains(avatarView)) {
            avatarBox.getChildren().clear();
            avatarBox.getChildren().add(avatarView);
        }
    }

    private void refreshAvatar() {
        ensureAvatarView();
        if (avatarView == null || avatarService == null || avatarBox == null) return;

        Profile p = LocalStore.getInstance().profile();
        int totalXp = p.getTotalXp();

        Set<String> unlocked = LocalStore.getInstance().badges().stream()
                .filter(Badge::isUnlocked)
                .map(Badge::getId)
                .collect(Collectors.toSet());

        try {
            URL url = avatarService.selectAvatar(totalXp, unlocked);
            if (url != null) avatarView.setImage(new Image(url.toExternalForm(), true));
        } catch (Throwable ignored) { }

        StackPane card = new StackPane();
        card.getStyleClass().add("avatar-card");
        card.setMinHeight(200);
        card.setPadding(new Insets(16));
        StackPane.setAlignment(avatarView, Pos.CENTER);

        Label badge = new Label("Lv." + p.level());
        badge.getStyleClass().add("level-badge");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(8, 8, 0, 0));

        card.getChildren().setAll(avatarView, badge);
        avatarBox.getChildren().setAll(card);
    }

    private void refreshBadges() {
        badgesList.getItems().clear();
        for (var b : LocalStore.getInstance().badges()) {
            String s = (b.isUnlocked() ? "🏅 " : "🔒 ") + b.getName() + " – " + b.getDescription();
            badgesList.getItems().add(s);
        }
    }

    private void refreshStreakUiSafe() {
        if (streakDaysLabel == null) return;
        try {
            var svc = StreakService.getInstance();
            streakDaysLabel.setText("Days: " + LocalStore.getInstance().streakDays());
            streakMultiplierLabel.setText(String.format("Multiplier: x%.2f", svc.getMultiplier()));
            streakNextLabel.setText("Next Milestone: " + String.valueOf(svc.getNextMilestoneDate()));
        } catch (Throwable ignore) { }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /* -------------------- Tiny animations -------------------- */

    private void animateProgress(ProgressBar bar, double from, double to) {
        if (Double.isNaN(from) || from < 0) from = 0.0;
        if (Double.isNaN(to)   || to   < 0) to   = 0.0;
        if (from > 1.0) from = 1.0;
        if (to   > 1.0) to   = 1.0;

        if (Math.abs(to - from) < 1e-6) { bar.setProgress(to); return; }
        bar.setProgress(from);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), from, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(420), new KeyValue(bar.progressProperty(), to,   Interpolator.EASE_BOTH))
        );
        tl.play();
    }

    private void bounceAvatar() {
        if (avatarBox == null || avatarBox.getChildren().isEmpty()) return;
        Node node = avatarBox.getChildren().get(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(220), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.06);  st.setToY(1.06);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();
    }

    /* -------------------- Theme guard -------------------- */
    private void ensureAppTheme(Scene scene) {
        try {
            String css = getClass().getResource("/application/theme/app.css").toExternalForm();
            var list = scene.getStylesheets();
            if (!list.contains(css)) list.add(0, css);
        } catch (Exception ignored) { }
    }
}
