package application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class StreakServiceTest {

    @BeforeEach
    void reset() {
        LocalStore.getInstance().resetForTests();
    }

    @Test
    void same_day_multiple_logs_no_increment() {
        StreakService svc = StreakService.getInstance();
        LocalDate d = LocalDate.of(2025, 9, 1);
        svc.recordActivity(d);
        assertEquals(1, svc.getStreakDays());
        svc.recordActivity(d);
        assertEquals(1, svc.getStreakDays());
        assertEquals(1.00, svc.getMultiplier(), 0.00001);
        assertEquals(LocalDate.of(2025, 9, 3), svc.getNextMilestoneDate()); // 3-day tier
    }

    @Test
    void consecutive_days_increment() {
        StreakService svc = StreakService.getInstance();
        svc.recordActivity(LocalDate.of(2025, 9, 1));
        svc.recordActivity(LocalDate.of(2025, 9, 2));
        svc.recordActivity(LocalDate.of(2025, 9, 3));
        assertEquals(3, svc.getStreakDays());
        assertEquals(1.05, svc.getMultiplier(), 0.00001);
    }

    @Test
    void gaps_reset_streak() {
        StreakService svc = StreakService.getInstance();
        svc.recordActivity(LocalDate.of(2025, 9, 1));
        svc.recordActivity(LocalDate.of(2025, 9, 3)); // gap of 1 day
        assertEquals(1, svc.getStreakDays());
        assertEquals(1.00, svc.getMultiplier(), 0.00001);
    }

    @Test
    void month_boundary_counts_as_consecutive() {
        StreakService svc = StreakService.getInstance();
        svc.recordActivity(LocalDate.of(2025, 1, 31));
        svc.recordActivity(LocalDate.of(2025, 2, 1));
        assertEquals(2, svc.getStreakDays());
    }

    @Test
    void year_boundary_counts_as_consecutive() {
        StreakService svc = StreakService.getInstance();
        svc.recordActivity(LocalDate.of(2024, 12, 31));
        svc.recordActivity(LocalDate.of(2025, 1, 1));
        assertEquals(2, svc.getStreakDays());
    }

    @Test
    void multiplier_schedule_and_cap() {
        StreakService svc = StreakService.getInstance();
        LocalDate start = LocalDate.of(2025, 9, 1);
        // reach 7 days
        for (int i = 0; i < 7; i++) svc.recordActivity(start.plusDays(i));
        assertEquals(7, svc.getStreakDays());
        assertEquals(1.10, svc.getMultiplier(), 0.00001);
        // reach 14 days
        for (int i = 7; i < 14; i++) svc.recordActivity(start.plusDays(i));
        assertEquals(14, svc.getStreakDays());
        assertEquals(1.15, svc.getMultiplier(), 0.00001);
        // extend far beyond; cap should hold
        for (int i = 14; i < 40; i++) svc.recordActivity(start.plusDays(i));
        assertTrue(svc.getStreakDays() >= 40);
        assertTrue(svc.getMultiplier() <= 1.20 + 1e-9);
    }

    @Test
    void next_milestone_dates() {
        StreakService svc = StreakService.getInstance();
        LocalDate start = LocalDate.of(2025, 9, 1);
        svc.recordActivity(start); // day 1 → next milestone day 3
        assertEquals(start.plusDays(2), svc.getNextMilestoneDate());
        svc.recordActivity(start.plusDays(1)); // day 2 → next milestone day 3 (tomorrow)
        assertEquals(start.plusDays(2), svc.getNextMilestoneDate());
        // reach 7 → next is 14
        for (int i = 2; i < 7; i++) svc.recordActivity(start.plusDays(i));
        assertEquals(start.plusDays(7 + (14 - 7) - 1), svc.getNextMilestoneDate()); // last=day7 date; +7 days
        // reach >=14 → next is tomorrow
        for (int i = 7; i < 14; i++) svc.recordActivity(start.plusDays(i));
        assertEquals(LocalStore.getInstance().lastActiveDate().plusDays(1), svc.getNextMilestoneDate());
    }
}