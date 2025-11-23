package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private int id;
    private int conversationId;
    private int senderId;
    private String content;
    private LocalDateTime sentAt;
    private String senderName;

    private int receiverId;
    
    // Fields cho ảnh
    private String imageUrl;        // URL ảnh từ Cloudinary
    private String imageFileName;   // Tên file gốc
    private boolean hasImage;       // Flag để biết có ảnh
    private String imageBase64;     // Base64 tạm thời (để gửi qua socket)
    
    public Message() {}

    public Message(int id, int conversationId, int senderId, String content, LocalDateTime sentAt) {
		this.id = id;
		this.conversationId = conversationId;
		this.senderId = senderId;
		this.content = content;
		this.sentAt = sentAt;
	}

//	public Message(int id, int conversationId, int senderId, String content, LocalDateTime sentAt) {
//        this.id = id;
//        this.conversationId = conversationId;
//        this.senderId = senderId;
//        this.content = content;
//        this.sentAt = sentAt;
//    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }
    
    // Getters và Setters cho ảnh
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getImageFileName() {
        return imageFileName;
    }
    
    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }
    
    public boolean hasImage() {
        return hasImage;
    }
    
    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }
    
    public String getImageBase64() {
        return imageBase64;
    }
    
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}