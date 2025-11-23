package Model;

import java.time.LocalDateTime;

public class ConversationMember {
    private int conversationId;
    private int userId;
    private LocalDateTime joinedAt;

    // Constructor, Getters, Setters
    
    public ConversationMember() {}

    public ConversationMember(int conversationId, int userId, LocalDateTime joinedAt) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.joinedAt = joinedAt;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}