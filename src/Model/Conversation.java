package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Conversation implements Serializable  {
    private int id;
    private String name; // Có thể null nếu là chat 1-1
    private ConversationType type; // Sử dụng Enum ở trên
    private LocalDateTime createdAt;

    // Constructor, Getters, Setters
    private boolean isOnline;
    	
    public Conversation() {}

    public Conversation(int id, String name, ConversationType type, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }
}