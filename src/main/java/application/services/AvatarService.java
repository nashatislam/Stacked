package application.services;

import java.net.URL;
import java.util.Set;

/** Selects which avatar image to show for the current state. */
public interface AvatarService {
    /**
     * Must return a non-null classpath URL to a PNG for the current state.
     * Implementations decide thresholds and badge rules.
     */
    URL selectAvatar(int totalXp, Set<String> unlockedBadgeIds);
}
