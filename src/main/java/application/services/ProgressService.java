package application.services;

import application.Realm;
import java.util.List;

/** Read-only progress API consumed by UI layers (Hub, Realm Map, etc.). */
public interface ProgressService {
    /** Overall XP (profile total). */
    int getTotalXp();

    /** Realm-specific XP (default 0 if not implemented). */
    default int getRealmXp(Realm realm) { return 0; }

    /** Lightweight summary used by Hub; keep UI-friendly and stable. */
    record RealmSummary(Realm realm, int realmXp, int level, int xpInto, int xpToNext) {}

    /** Summaries for all realms; default empty for non-Hub contexts. */
    default List<RealmSummary> getAllRealmSummaries() { return List.of(); }
}