package application;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class XpService {
    // Why: Deterministic, non-linear progression curve; cap gameplay at level 10.
    public static final int MAX_LEVEL = 10;
    public static final int[] LEVEL_THRESHOLDS = new int[]{
            0, 100, 250, 450, 700, 1000, 1400, 1850, 2350, 2900
    }; // indices 0..9 represent L1..L10 starting XP

    private static int clampXp(int xp) { return Math.max(0, xp); }

    /** Highest level with xp >= threshold[level-1], capped at MAX_LEVEL. */
    public static int levelForXp(int xp) {
        int x = clampXp(xp);
        int level = 1;
        for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
            if (x >= LEVEL_THRESHOLDS[i]) level = i + 1; else break;
        }
        return Math.min(level, MAX_LEVEL);
    }

    /** XP earned since the start of current level. 0 at L1. */
    public static int xpIntoLevel(int xp) {
        int x = clampXp(xp);
        int lvl = levelForXp(x);
        int base = LEVEL_THRESHOLDS[lvl - 1];
        return Math.max(0, x - base);
    }

    /** XP needed to reach next level. Returns 0 at MAX_LEVEL. */
    public static int xpToNextLevel(int xp) {
        int x = clampXp(xp);
        int lvl = levelForXp(x);
        if (lvl >= MAX_LEVEL) return 0;
        int next = LEVEL_THRESHOLDS[lvl];
        return Math.max(0, next - x);
    }

    /** Denominator for progress bars within current level. */
    public static int levelWindowSize(int xp) {
        int x = clampXp(xp);
        int lvl = levelForXp(x);
        if (lvl >= MAX_LEVEL) return Math.max(1, xpIntoLevel(x)); // full bar at max
        int base = LEVEL_THRESHOLDS[lvl - 1];
        int next = LEVEL_THRESHOLDS[lvl];
        return Math.max(1, next - base);
    }

    // ----------------- Centralized XP rules -----------------
    // Tuned quick-action XP mapping; ensures custom logs match these exact amounts.
    private static final Map<String, Integer> TUNED_DEFAULTS = new HashMap<>();
    static {
        // Keys are TYPE:AMOUNT (minutes or ml)
        TUNED_DEFAULTS.put("STUDY:25", 25);
        TUNED_DEFAULTS.put("STUDY:50", 55);
        TUNED_DEFAULTS.put("BREAK:5", 4);
        TUNED_DEFAULTS.put("BREAK:10", 8);
        TUNED_DEFAULTS.put("HYDRATION:250", 5);
        TUNED_DEFAULTS.put("HYDRATION:500", 12);
    }

    /** Base XP before multipliers, honoring tuned quick-actions. */
    public int baseXpForEntry(TaskEntry entry) {
        if (entry == null || entry.getType() == null) return 0;
        String key = entry.getType().name() + ":" + entry.getAmount();
        Integer tuned = TUNED_DEFAULTS.get(key);
        if (tuned != null) return Math.max(0, tuned);

        return switch (entry.getType()) {
            case STUDY -> Math.max(0, entry.getAmount());
            case HYDRATION -> Math.max(0, entry.getAmount() / 100);
            case BREAK -> Math.max(0, entry.getAmount() / 5);
        };
    }

    /** Streak multiplier applies only for logs dated today. */
    public double activeMultiplierFor(LocalDate date) {
        if (date != null && date.equals(LocalDate.now())) {
            return StreakService.getInstance().getMultiplier();
        }
        return 1.0;
    }

    /** Final XP after multiplier; floor, min 0. */
    public int finalXp(TaskEntry entry) {
        int base = baseXpForEntry(entry);
        LocalDate logDay = (entry.getWhen() != null) ? entry.getWhen().toLocalDate() : LocalDate.now();
        double mult = activeMultiplierFor(logDay);
        int out = (int) Math.floor(base * mult);
        return Math.max(0, out);
    }

    /** Applies XP to profile and realm atomically, idempotent per taskId. */
    public void applyEntry(TaskEntry entry) {
        if (entry == null) return;
        if (entry.getId() == null || entry.getId().isBlank()) entry.ensureId();

        LocalStore store = LocalStore.getInstance();
        synchronized (store) {
            if (store.awardedTaskIds().contains(entry.getId())) return; // already awarded

            LocalDate logDay = (entry.getWhen() != null) ? entry.getWhen().toLocalDate() : LocalDate.now();
            if (logDay.equals(LocalDate.now())) {
                // Update streak on the first successful award of today
                StreakService.getInstance().recordActivity(logDay);
            }

            int gained = finalXp(entry);
            Profile profile = store.profile();
            profile.addTotalXp(gained);
            profile.addRealmXp(entry.getType().realm(), gained);

            store.awardedTaskIds().add(entry.getId());
            store.saveSafe();
        }
    }
}