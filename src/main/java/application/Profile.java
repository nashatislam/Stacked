package application;

import java.util.EnumMap;
import java.util.Map;

public class Profile {
    private String username = "Player One";
    private int totalXp = 0;
    private final Map<Realm, Integer> realmXp = new EnumMap<>(Realm.class);

    public Profile() {
        for (Realm r : Realm.values()) realmXp.put(r, 0);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getTotalXp() { return totalXp; }
    public void addTotalXp(int delta) { this.totalXp = Math.max(0, totalXp + Math.max(0, delta)); }
    public int getRealmXp(Realm r) { return realmXp.getOrDefault(r, 0); }
    public void addRealmXp(Realm r, int delta) { realmXp.put(r, Math.max(0, getRealmXp(r) + Math.max(0, delta))); }
    public void addXp(int xp) {
        addTotalXp(xp);
    }
    // Deterministic level API
    public int level() { return XpService.levelForXp(getTotalXp()); }
    public int xpIntoLevel() { return XpService.xpIntoLevel(getTotalXp()); }
}