package application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SaveData {
    Profile profile = new Profile();
    List<TaskEntry> entries = new ArrayList<>();
    List<Badge> badges = new ArrayList<>();

    // XP/idempotence
    Set<String> awardedTaskIds = new HashSet<>();

    // Realm map progress
    Set<String> claimedNodeIds = new HashSet<>(); // keys: "REALM:nodeId"

    // Streak (persisted)
    LocalDate lastActiveDate = null;
    int streakDays = 0;
}