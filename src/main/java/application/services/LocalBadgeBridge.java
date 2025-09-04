// file: src/main/java/application/services/LocalBadgeBridge.java
package application.services;

import application.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LocalBadgeBridge implements BadgeService {

    /* --------------------------- Badge catalog --------------------------- */
    private static final List<Badge> CATALOG = List.of(
            // Progress
            new Badge("first_study",           "First Study",          "Log your first study session.", false),
            new Badge("level_up_5",            "Level Up!",            "Reach level 5 (any realm or global).", false),
            new Badge("deep_diver_10",         "Deep Diver",           "Reach level 10 in a single realm.", false),
            new Badge("realm_explorer_3",      "Realm Explorer",       "Unlock 3 different realms.", false),

            // Streak
            new Badge("streak_7",              "One Week Wonder",      "Log tasks 7 days in a row.", false),
            new Badge("streak_30",             "Consistency Crown",    "Log tasks 30 days in a row.", false),
            new Badge("comeback_kid",          "Comeback Kid",         "Resume logging after a 7+ day break.", false),

            // Task variety
            new Badge("hydration_master_10",   "Hydration Master",     "Log hydration 10 times.", false),
            new Badge("break_boss_20",         "Break Boss",           "Log 20 breaks.", false),
            new Badge("balanced_hero_day",     "Balanced Hero",        "Study + Hydration + Break in the same day.", false),

            // Milestones
            new Badge("century_club_100",      "Century Club",         "Accumulate 100 total tasks.", false),
            new Badge("marathon_scholar_1000m","Marathon Scholar",     "Accumulate 1,000 study minutes.", false),
            new Badge("xp_hoarder_1000",       "XP Hoarder",           "Earn 1,000 XP total.", false),

            // Fun/personality
            new Badge("night_owl",             "Night Owl",            "Study after midnight.", false),
            new Badge("early_bird",            "Early Bird",           "Log a task before 7 AM.", false),
            new Badge("double_study_back2back","Double Session",       "Two study tasks back-to-back.", false),

            // Special challenges
            new Badge("focus_master_60",       "Focus Master",         "Single 60+ minute study session.", false),
            new Badge("hydration_hero_1l",     "Hydration Hero",       "Drink 1000 ml in a day.", false),
            new Badge("hydration_over_2l",     "Hydration Overachiever","Drink 2,000 ml in a day.", false),
            new Badge("badge_collector_10",    "Badge Collector",      "Unlock 10 badges.", false)
    );

    /* --------------------------- Public API --------------------------- */

    @Override
    public void ensureDefaults() {
        var store = LocalStore.getInstance();
        Map<String,Badge> existing = store.badges().stream()
                .collect(Collectors.toMap(Badge::getId, Function.identity(), (a,b)->a, LinkedHashMap::new));

        for (Badge b : CATALOG) {
            if (!existing.containsKey(b.getId())) {
                // copy to preserve order & defaults
                store.badges().add(new Badge(b.getId(), b.getName(), b.getDescription(), false));
            }
        }
        // Optional: prune old/unknown IDs here if you want strict sync.
    }

    @Override
    public void evaluateAll() {
        var store = LocalStore.getInstance();
        ensureDefaults(); // make sure all catalog items exist
        var entries = new ArrayList<>(store.entries());
        entries.sort(Comparator.comparing(TaskEntry::getWhen));

        var profile = store.profile();
        var streakSvc = StreakService.getInstance();
        int streakDays = store.streakDays();

        // Per-day rollups
        Map<LocalDate, List<TaskEntry>> byDay = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getWhen().toLocalDate()));

        // Helpers
        boolean anyStudyEver = entries.stream().anyMatch(e -> e.getType()==TaskType.STUDY);
        int studyMinutesTotal = entries.stream()
                .filter(e -> e.getType()==TaskType.STUDY)
                .mapToInt(TaskEntry::getAmount).sum();
        int hydrationLogsTotal = (int) entries.stream().filter(e -> e.getType()==TaskType.HYDRATION).count();
        int breakLogsTotal = (int) entries.stream().filter(e -> e.getType()==TaskType.BREAK).count();
        boolean nightOwl = entries.stream().anyMatch(e ->
                e.getType()==TaskType.STUDY && e.getWhen().getHour() >= 0 && e.getWhen().getHour() < 5);
        boolean earlyBird = entries.stream().anyMatch(e -> e.getWhen().getHour() < 7);
        boolean focus60 = entries.stream().anyMatch(e ->
                e.getType()==TaskType.STUDY && e.getAmount() >= 60);
        boolean doubleStudy = hasBackToBackStudy(entries);

        boolean dayWithBalanced = byDay.values().stream().anyMatch(list -> {
            boolean s = list.stream().anyMatch(e->e.getType()==TaskType.STUDY);
            boolean h = list.stream().anyMatch(e->e.getType()==TaskType.HYDRATION);
            boolean b = list.stream().anyMatch(e->e.getType()==TaskType.BREAK);
            return s && h && b;
        });
        boolean day1L = byDay.values().stream().anyMatch(list ->
                list.stream().filter(e->e.getType()==TaskType.HYDRATION)
                        .mapToInt(TaskEntry::getAmount).sum() >= 1000);
        boolean day2L = byDay.values().stream().anyMatch(list ->
                list.stream().filter(e->e.getType()==TaskType.HYDRATION)
                        .mapToInt(TaskEntry::getAmount).sum() >= 2000);

        boolean comeback = hasComeback(entries); // ≥7 day gap followed by a log

        // Apply rules
        unlock("first_study",            anyStudyEver);
        unlock("level_up_5",             profile.level() >= 5);
        unlock("deep_diver_10",          hasRealmLevelAtLeast(10)); // if you later track per-realm levels, wire here
        unlock("realm_explorer_3",       hasUnlockedAtLeastNRealms(3)); // placeholder hook

        unlock("streak_7",               streakDays >= 7);
        unlock("streak_30",              streakDays >= 30);
        unlock("comeback_kid",           comeback);

        unlock("hydration_master_10",    hydrationLogsTotal >= 10);
        unlock("break_boss_20",          breakLogsTotal >= 20);
        unlock("balanced_hero_day",      dayWithBalanced);

        unlock("century_club_100",       entries.size() >= 100);
        unlock("marathon_scholar_1000m", studyMinutesTotal >= 1000);
        unlock("xp_hoarder_1000",        profile.getTotalXp() >= 1000);

        unlock("night_owl",              nightOwl);
        unlock("early_bird",             earlyBird);
        unlock("double_study_back2back", doubleStudy);

        unlock("focus_master_60",        focus60);
        unlock("hydration_hero_1l",      day1L);
        unlock("hydration_over_2l",      day2L);

        // Meta
        long unlockedCount = LocalStore.getInstance().badges().stream().filter(Badge::isUnlocked).count();
        unlock("badge_collector_10", unlockedCount >= 10);
    }

    /* --------------------------- Helpers --------------------------- */

    private static boolean hasBackToBackStudy(List<TaskEntry> entries) {
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i-1).getType()==TaskType.STUDY &&
                    entries.get(i).getType()==TaskType.STUDY) return true;
        }
        return false;
    }

    private static boolean hasComeback(List<TaskEntry> entries) {
        if (entries.size() < 2) return false;
        LocalDate prev = entries.get(0).getWhen().toLocalDate();
        for (int i=1;i<entries.size();i++){
            LocalDate d = entries.get(i).getWhen().toLocalDate();
            if (Duration.between(prev.atStartOfDay(), d.atStartOfDay()).toDays() >= 7) {
                // There's a gap ≥7 and we logged again => comeback
                return true;
            }
            if (d.isAfter(prev)) prev = d;
        }
        return false;
    }

    private static boolean hasRealmLevelAtLeast(int level) {
        // If you later keep per-realm levels in LocalStore, check them here.
        // For now, piggyback on global level so it never falsely blocks:
        return LocalStore.getInstance().profile().level() >= level;
    }

    private static boolean hasUnlockedAtLeastNRealms(int n) {
        // Hook where you can compute “unlocked realms” (e.g., any realm XP > 0).
        // For now, approximate from entries touching ≥ n distinct realms if you record that,
        // otherwise return false to avoid accidental unlocks.
        return false;
    }

    private static void unlock(String id, boolean condition) {
        if (!condition) return;
        LocalStore.getInstance().badges().stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .ifPresent(b -> { if (!b.isUnlocked()) b.setUnlocked(true); });
    }
}
