package Controll; // Hoặc package chung Shared

import java.io.Serializable;

public class Packet implements Serializable {
    // Đảm bảo cả 2 bên (Client/Server) dùng chung 1 phiên bản
    private static final long serialVersionUID = 1L; 
    
    private MessageType type; // Loại tin nhắn
    private Object payload;   // Dữ liệu kèm theo
    
    // --- CÁC TRƯỜNG MỚI CẦN THÊM ---
    private int senderId;     // ID người gửi
    private int receiverId;   // ID người nhận (để Server biết cần chuyển cho ai)
    private String senderName; // Tên người gửi (để hiển thị nhanh, đỡ phải query lại)

    // Constructor 1: Dùng cho các lệnh đơn giản (giữ nguyên code cũ cho đỡ lỗi)
    public Packet(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    // Constructor 2: Dùng cho định tuyến (Gửi cho cụ thể ai đó)
    public Packet(MessageType type, Object payload, int receiverId) {
        this.type = type;
        this.payload = payload;
        this.receiverId = receiverId;
    }

    // --- Getters & Setters ---

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}