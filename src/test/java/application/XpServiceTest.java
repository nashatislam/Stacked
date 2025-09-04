package application;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class XpServiceTest {

    @Test void level_boundaries_exact_thresholds() {
        assertEquals(1, XpService.levelForXp(0));
        assertEquals(2, XpService.levelForXp(100));
        assertEquals(3, XpService.levelForXp(250));
        assertEquals(10, XpService.levelForXp(2900));
    }

    @Test void level_boundaries_minus_one() {
        assertEquals(1, XpService.levelForXp(99));
        assertEquals(2, XpService.levelForXp(249));
        assertEquals(9, XpService.levelForXp(2899));
    }

    @Test void level_boundaries_plus_one() {
        assertEquals(2, XpService.levelForXp(101));
        assertEquals(3, XpService.levelForXp(251));
        assertEquals(10, XpService.levelForXp(2901));
    }

    @Test void negative_and_huge_values() {
        assertEquals(1, XpService.levelForXp(-5));
        assertEquals(0, XpService.xpIntoLevel(-123));
        assertEquals(10, XpService.levelForXp(Integer.MAX_VALUE));
        assertEquals(0, XpService.xpToNextLevel(Integer.MAX_VALUE));
    }

    @Test void xp_into_and_to_next_consistency() {
        int x = 775; // between 700 and 1000
        assertEquals(5, XpService.levelForXp(x));
        assertEquals(75, XpService.xpIntoLevel(x));
        assertEquals(1000 - 775, XpService.xpToNextLevel(x));
        assertEquals(XpService.xpIntoLevel(x) + XpService.xpToNextLevel(x), XpService.levelWindowSize(x));
    }

    @Test void tuned_quick_actions_match_custom_logs() {
        XpService svc = new XpService();
        TaskEntry s25 = new TaskEntry(TaskType.STUDY, 25, java.time.LocalDate.now().atStartOfDay());
        TaskEntry s50 = new TaskEntry(TaskType.STUDY, 50, java.time.LocalDate.now().atStartOfDay());
        TaskEntry b5 = new TaskEntry(TaskType.BREAK, 5, java.time.LocalDate.now().atStartOfDay());
        TaskEntry h250 = new TaskEntry(TaskType.HYDRATION, 250, java.time.LocalDate.now().atStartOfDay());

        assertEquals(25, svc.baseXpForEntry(s25));
        assertEquals(55, svc.baseXpForEntry(s50));
        assertEquals(4, svc.baseXpForEntry(b5));
        assertEquals(5, svc.baseXpForEntry(h250));

        // sanity for non-tuned amounts
        TaskEntry s23 = new TaskEntry(TaskType.STUDY, 23, java.time.LocalDate.now().atStartOfDay());
        TaskEntry b7 = new TaskEntry(TaskType.BREAK, 7, java.time.LocalDate.now().atStartOfDay());
        TaskEntry h300 = new TaskEntry(TaskType.HYDRATION, 300, java.time.LocalDate.now().atStartOfDay());
        assertEquals(23, svc.baseXpForEntry(s23));
        assertEquals(1, svc.baseXpForEntry(b7));
        assertEquals(3, svc.baseXpForEntry(h300));
    }
}