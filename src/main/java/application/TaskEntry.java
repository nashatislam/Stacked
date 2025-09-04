package application;

import java.time.LocalDateTime;
import java.util.UUID;

public class TaskEntry {
    private String id;
    private TaskType type;
    private int amount;
    private LocalDateTime when;

    public TaskEntry() { /* for Gson */ }
    public TaskEntry(TaskType type, int amount, LocalDateTime when) {
        this.id = UUID.randomUUID().toString();
        this.type = type; this.amount = amount; this.when = when;
    }

    public void ensureId() { if (id == null || id.isBlank()) id = UUID.randomUUID().toString(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public java.time.LocalDateTime getWhen() { return when; }
    public void setWhen(java.time.LocalDateTime when) { this.when = when; }
}
