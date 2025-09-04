package application.services;

import application.LocalStore;
import application.Realm;
import application.XpService;

import java.util.ArrayList;
import java.util.List;

/** Trivial bridge that reads progress from LocalStore/Profile. */
public class LocalProgressBridge implements ProgressService {
    @Override public int getTotalXp() {
        return LocalStore.getInstance().profile().getTotalXp();
    }

    @Override public int getRealmXp(Realm realm) {
        return LocalStore.getInstance().profile().getRealmXp(realm);
    }

    @Override public List<RealmSummary> getAllRealmSummaries() {
        List<RealmSummary> out = new ArrayList<>();
        for (Realm r : Realm.values()) {
            int xp = getRealmXp(r);
            int lvl = XpService.levelForXp(xp);
            int into = XpService.xpIntoLevel(xp);
            int toNext = XpService.xpToNextLevel(xp);
            out.add(new RealmSummary(r, xp, lvl, into, toNext));
        }
        return out;
    }
}