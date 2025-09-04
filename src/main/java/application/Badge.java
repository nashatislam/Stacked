package application;

public class Badge {
    private String id;
    private String name;
    private String description;
    private boolean unlocked;

    public Badge() { }
    public Badge(String id, String name, String description, boolean unlocked) {
        this.id = id; this.name = name; this.description = description; this.unlocked = unlocked;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}