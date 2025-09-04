package application;

public enum Realm {
    MEMORY("Memory"),
    STACK("Stack"),
    COOLING("Cooling"),
    ALGORITHMS("Algorithms"),
    SYSTEMS("Systems");

    private final String display;
    Realm(String display) { this.display = display; }
    public String display() { return display; }
}
