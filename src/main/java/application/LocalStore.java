package application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Minimal local save system. Writes JSON under user home directory.
 */
public class LocalStore {
    private static final LocalStore INSTANCE = new LocalStore();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .setPrettyPrinting()
            .create();

    private final Path baseDir = Paths.get(System.getProperty("user.home"), ".stacked");
    private final Path dataFile = baseDir.resolve("save.json");

    private SaveData data = new SaveData();

    public static LocalStore getInstance() { return INSTANCE; }

    public synchronized void loadSafe() {
        try {
            if (Files.exists(dataFile)) {
                String json = Files.readString(dataFile);
                SaveData loaded = gson.fromJson(json, SaveData.class);
                if (loaded != null) data = loaded;
            } else {
                initDefaults();
                saveSafe();
            }
        } catch (Exception ex) {
            backupCorrupt();
            initDefaults();
        }
    }

    public synchronized void saveSafe() {
        try {
            Files.createDirectories(baseDir);
            Files.writeString(dataFile, gson.toJson(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void backupCorrupt() {
        try {
            Files.createDirectories(baseDir);
            if (Files.exists(dataFile)) {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                Files.move(dataFile, baseDir.resolve("save_corrupt_" + ts + ".json"));
            }
        } catch (IOException ignored) { }
    }

    private void initDefaults() {
        data = new SaveData();
        if (data.badges.isEmpty()) {
            data.badges.add(new Badge("first_study", "First Study", "Log your first study session", false));
            data.badges.add(new Badge("hydration_1l", "Hydration Hero", "Drink 1000 ml in a day", false));
            data.badges.add(new Badge("consistency_3", "Three Day Streak", "Log tasks 3 days in a row", false));
        }
    }

    // --- existing getters ---
    public synchronized Profile profile() { return data.profile; }
    public synchronized java.util.List<TaskEntry> entries() { return data.entries; }
    public synchronized java.util.List<Badge> badges() { return data.badges; }
    public synchronized Set<String> awardedTaskIds() { return data.awardedTaskIds; }
    public synchronized Set<String> claimedNodeIds() { return data.claimedNodeIds; }
    public synchronized void addEntry(TaskEntry e) { if (e != null) data.entries.add(e); }

    // --- streak persistence ---
    public synchronized LocalDate lastActiveDate() { return data.lastActiveDate; }
    public synchronized int streakDays() { return data.streakDays; }
    public synchronized void setStreak(LocalDate lastActive, int days) {
        data.lastActiveDate = lastActive;
        data.streakDays = Math.max(0, days);
        saveSafe();
    }
}