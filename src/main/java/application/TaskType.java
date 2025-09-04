package application;

public enum TaskType {
    STUDY("Study (min)", Realm.ALGORITHMS),
    HYDRATION("Hydration (ml)", Realm.COOLING),
    BREAK("Break (min)", Realm.MEMORY);

    private final String label;
    private final Realm realm;
    TaskType(String label, Realm realm) { this.label = label; this.realm = realm; }
    public String label() { return label; }
    public Realm realm() { return realm; }
}