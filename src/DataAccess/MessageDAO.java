package DataAccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Model.Message;

public class MessageDAO {

    /**
     * Lấy toàn bộ lịch sử tin nhắn của một cuộc trò chuyện
     */
//    public List<Message> findMessagesByConversationId(int conversationId) {
//        List<Message> messages = new ArrayList<>();
//        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC";
//        
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            
//            pstmt.setInt(1, conversationId);
//            
//            try (ResultSet rs = pstmt.executeQuery()) {
//                while (rs.next()) {
//                    messages.add(mapResultSetToMessage(rs));
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return messages;
//    }

	 public List<Message> findMessagesByConversationId(int conversationId) throws SQLException {
	        List<Message> messages = new ArrayList<>();
	        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC";
	        
	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement stmt = conn.prepareStatement(sql)) {
	            
	            stmt.setInt(1, conversationId);
	            ResultSet rs = stmt.executeQuery();
	            
	            while (rs.next()) {
	                Message msg = new Message();
	                msg.setId(rs.getInt("id"));
	                msg.setConversationId(rs.getInt("conversation_id"));
	                msg.setSenderId(rs.getInt("sender_id"));
	                msg.setContent(rs.getString("content"));
	                msg.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
	                
	                // Đọc image fields
	                String imageUrl = rs.getString("image_url");
	                String imageFileName = rs.getString("image_filename");
	                if (imageUrl != null && !imageUrl.isEmpty()) {
	                    msg.setImageUrl(imageUrl);
	                    msg.setImageFileName(imageFileName);
	                    msg.setHasImage(true);
	                }
	                
	                // ✅ QUAN TRỌNG: Set receiverId từ conversation_members
	                // Tìm người nhận (người còn lại trong conversation)
	                int receiverId = getOtherMemberInConversation(conversationId, msg.getSenderId());
	                msg.setReceiverId(receiverId);
	                
	                messages.add(msg);
	            }
	            
	            System.out.println("[MessageDAO] Found " + messages.size() + " messages in conversation " + conversationId);
	            return messages;
	            
	        } catch (SQLException e) {
	            System.err.println("[MessageDAO] Error findMessagesByConversationId: " + e.getMessage());
	            throw e;
	        }
	    }
	 
	 private int getOtherMemberInConversation(int conversationId, int senderId) throws SQLException {
	        String sql = "SELECT user_id FROM conversation_members " +
	                     "WHERE conversation_id = ? AND user_id != ?";
	        
	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement stmt = conn.prepareStatement(sql)) {
	            
	            stmt.setInt(1, conversationId);
	            stmt.setInt(2, senderId);
	            
	            ResultSet rs = stmt.executeQuery();
	            if (rs.next()) {
	                return rs.getInt("user_id");
	            }
	            
	            // Nếu không tìm thấy (không nên xảy ra), trả về 0
	            System.err.println("[MessageDAO] Could not find other member in conversation " + conversationId);
	            return 0;
	            
	        } catch (SQLException e) {
	            System.err.println("[MessageDAO] Error getOtherMemberInConversation: " + e.getMessage());
	            throw e;
	        }
	    }

	 
	 public Message createMessage(Message message) {
		    String sql = "INSERT INTO messages (conversation_id, sender_id, content, image_url, image_filename, has_image, sent_at) " +
		                 "VALUES (?, ?, ?, ?, ?, ?, ?)";
		    
		    try (Connection conn = DatabaseConnection.getConnection();
		         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
		        
		        ps.setInt(1, message.getConversationId());
		        ps.setInt(2, message.getSenderId());
		        ps.setString(3, message.getContent());
		        ps.setString(4, message.getImageUrl());
		        ps.setString(5, message.getImageFileName());
		        ps.setBoolean(6, message.hasImage());
		        ps.setTimestamp(7, Timestamp.valueOf(message.getSentAt() != null ? message.getSentAt() : LocalDateTime.now()));
		        
		        int affected = ps.executeUpdate();
		        if (affected > 0) {
		            ResultSet rs = ps.getGeneratedKeys();
		            if (rs.next()) {
		                message.setId(rs.getInt(1));
		            }
		            
		            // ✅ senderName đã được set từ server, không cần query lại
		            return message;
		        }
		        
		    } catch (SQLException e) {
		        System.err.println("[MessageDAO] Error creating message: " + e.getMessage());
		        e.printStackTrace();
		    }
		    
		    return null;
		}
    
    public Message findMessageById(int messageId) {
        String sql = "SELECT * FROM messages WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, messageId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessage(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
//    public Message createMessage(int conversationId, int senderId, String content) {
//        String sql = "INSERT INTO messages (conversation_id, sender_id, content) VALUES (?, ?, ?)";
//        
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//            
//            pstmt.setInt(1, conversationId);
//            pstmt.setInt(2, senderId);
//            pstmt.setString(3, content);
//            
//            int affectedRows = pstmt.executeUpdate();
//            if (affectedRows > 0) {
//                try (ResultSet rs = pstmt.getGeneratedKeys()) {
//                    if (rs.next()) {
//                        int newId = rs.getInt(1);
//                        // Lấy lại tin nhắn (giả sử có hàm findMessageById)
//                        // Hoặc tạo đối tượng ngay lập h
//                        Message msg = new Message();
//                        msg.setId(newId);
//                        msg.setConversationId(conversationId);
//                        msg.setSenderId(senderId);
//                        msg.setContent(content);
//                        // msg.setSentAt(...); // Cần lấy lại từ CSDL
//                        return msg;
//                    }
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message msg = new Message(
            rs.getInt("id"),
            rs.getInt("conversation_id"),
            rs.getInt("sender_id"),
            rs.getString("content"),
            rs.getTimestamp("sent_at").toLocalDateTime()
        );
        
        // Đọc image fields
        String imageUrl = rs.getString("image_url");
        String imageFileName = rs.getString("image_filename");
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            msg.setImageUrl(imageUrl);
            msg.setImageFileName(imageFileName);
            msg.setHasImage(true);
        }
        
        return msg;
    }
}