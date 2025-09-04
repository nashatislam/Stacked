
package application.services;

import java.net.URL;
import java.util.List;
import java.util.Set;

public final class ClasspathAvatarService implements AvatarService {
    private static final String ROOT = "/application/avatar/";
    private record Tier(int minXp, String file) {}


    private static final List<Tier> TIERS = List.of(
            new Tier(0,    "avatar_00.png"),
            new Tier(300,  "avatar_01.png"),
            new Tier(700,  "avatar_02.png"),
            new Tier(1200, "avatar_03.png"),
            new Tier(1800, "avatar_04.png"),
            new Tier(2500, "avatar_05.png"),
            new Tier(3300, "avatar_06.png"),
            new Tier(4200, "avatar_07.png")
    );

    @Override
    public URL selectAvatar(int totalXp, Set<String> unlockedBadges) {

        if (unlockedBadges != null) {
            if (unlockedBadges.contains("winter-scarf")) {
                URL u = get(ROOT + "avatar_scarf.png");
                if (u != null) return u;
            }
            if (unlockedBadges.contains("legend")) {
                URL u = get(ROOT + "avatar_legend.png");
                if (u != null) return u;
            }
        }


        URL best = null;
        for (Tier t : TIERS) {
            if (totalXp >= t.minXp()) {
                URL u = get(ROOT + t.file());
                if (u != null) best = u;
            } else break;
        }
        return best != null ? best : get(ROOT + TIERS.get(0).file());
    }

    private URL get(String path) { return getClass().getResource(path); }
}
