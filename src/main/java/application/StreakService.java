package application;

import java.time.LocalDate;


public final class StreakService {
    private static final StreakService INSTANCE = new StreakService();
    private StreakService() { }
    public static StreakService getInstance() { return INSTANCE; }

    /** Updates streak state for the given activity day. Idempotent for same day. */
    public synchronized void recordActivity(LocalDate day) {
        if (day == null) return;
        LocalStore store = LocalStore.getInstance();
        LocalDate last = store.lastActiveDate();
        int days = store.streakDays();

        if (last == null) {
            store.setStreak(day, 1);
            store.saveSafe();
            return;
        }
        if (day.isBefore(last)) {
            return;
        }
        if (day.isEqual(last)) {
            return;
        }
        if (day.isEqual(last.plusDays(1))) {
            store.setStreak(day, days + 1);
        } else { // gap
            store.setStreak(day, 1);
        }
        store.saveSafe();
    }

    public int getStreakDays() {
        return Math.max(0, LocalStore.getInstance().streakDays());
    }

    public double getMultiplier() {
        int d = getStreakDays();
        double m = 1.0;
        if (d >= 14) m = 1.15;
        else if (d >= 7) m = 1.10;
        else if (d >= 3) m = 1.05;
        return Math.min(1.20, m);
    }


    public LocalDate getNextMilestoneDate() {
        LocalDate last = LocalStore.getInstance().lastActiveDate();
        int d = getStreakDays();
        if (last == null) return LocalDate.now();
        int target = (d < 3) ? 3 : (d < 7) ? 7 : (d < 14) ? 14 : d + 1;
        int delta = Math.max(1, target - d);
        return last.plusDays(delta);
    }
}
