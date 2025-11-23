package DataAccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import Model.Conversation;
import Model.ConversationType;

public class ConversationDAO {

    /**
     * Tìm một cuộc trò chuyện 1-1 (direct) đã tồn tại giữa 2 user.
     */
//    public Conversation findDirectConversation(int userId1, int userId2) {
//        String sql = "SELECT c.* FROM conversations c " +
//                     "JOIN conversation_members cm1 ON c.id = cm1.conversation_id " +
//                     "JOIN conversation_members cm2 ON c.id = cm2.conversation_id " +
//                     "WHERE c.type = 'DIRECT' AND cm1.user_id = ? AND cm2.user_id = ?";
//                     
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            
//            pstmt.setInt(1, userId1);
//            pstmt.setInt(2, userId2);
//            
//            try (ResultSet rs = pstmt.executeQuery()) {
//                if (rs.next()) {
//                    return mapResultSetToConversation(rs);
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return null; // Không tìm thấy
//    }
	
	public Conversation findDirectConversation(int userId1, int userId2) throws SQLException {
        String sql = "SELECT c.* FROM conversations c " +
                     "WHERE c.type = 'direct' AND c.id IN (" +
                     "  SELECT cm1.conversation_id FROM conversation_members cm1 " +
                     "  WHERE cm1.user_id = ? AND cm1.conversation_id IN (" +
                     "    SELECT cm2.conversation_id FROM conversation_members cm2 " +
                     "    WHERE cm2.user_id = ?" +
                     "  )" +
                     ")";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Conversation convo = new Conversation();
                convo.setId(rs.getInt("id"));
                convo.setName(rs.getString("name"));
                
                String typeStr = rs.getString("type");
                convo.setType(typeStr.equalsIgnoreCase("direct") 
                    ? ConversationType.DIRECT 
                    : ConversationType.GROUP);
                
//                convo.setCreatedAt(rs.getTimestamp("created_at"));
//                rs.getTimestamp("created_at").toLocalDateTime()
                convo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                
                System.out.println("[ConversationDAO] Found existing conversation ID: " + convo.getId());
                return convo;
            }
            
            System.out.println("[ConversationDAO] No direct conversation found between " 
                + userId1 + " and " + userId2);
            return null;
            
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error findDirectConversation: " + e.getMessage());
            throw e;
        }
    }
	public List<Conversation> getConversationsByUserId(int userId) throws SQLException {
        List<Conversation> conversations = new ArrayList<>();
        
        // Dùng JOIN để lấy thông tin Conversation từ bảng Member
        String sql = "SELECT c.* FROM conversations c " +
                     "JOIN conversation_members cm ON c.id = cm.conversation_id " +
                     "WHERE cm.user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                conversations.add(mapResultSetToConversation(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error getConversationsByUserId: " + e.getMessage());
            throw e;
        }
        
        return conversations;
    }
	
public String getDirectConversationName(int conversationId, int currentUserId) throws SQLException {
        
        // Câu lệnh này tìm user_id của người KHÔNG PHẢI là currentUserId
        // nhưng cùng ở trong conversationId đó.
        // Sau đó JOIN với bảng users để lấy username.
        String sql = "SELECT u.username FROM users u " +
                     "JOIN conversation_members cm ON u.id = cm.user_id " +
                     "WHERE cm.conversation_id = ? AND cm.user_id != ?";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conversationId);
            stmt.setInt(2, currentUserId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("username");
            } else {
                // Trường hợp hiếm: chat 1-1 nhưng chỉ có 1 mình bạn
                return "Lỗi: Không tìm thấy bạn chat";
            }
            
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error getDirectConversationName: " + e.getMessage());
            throw e;
        }
    }
    /**
     * Tạo một cuộc trò chuyện mới (cả 1-1 và nhóm)
     * Trả về Conversation đã tạo (với ID mới).
     */
//    public Conversation createConversation(String name, ConversationType type) {
//        String sql = "INSERT INTO conversations (name, type) VALUES (?, ?)";
//        
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//            
//            pstmt.setString(1, name);
//            pstmt.setString(2, type.name());
//            
//            int affectedRows = pstmt.executeUpdate();
//            if (affectedRows > 0) {
//                try (ResultSet rs = pstmt.getGeneratedKeys()) {
//                    if (rs.next()) {
//                        int newId = rs.getInt(1);
//                        // Lấy lại đối tượng đầy đủ
//                        return findConversationById(newId);
//                    }
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
    
    public Conversation createConversation(String name, ConversationType type) throws SQLException {
        String sql = "INSERT INTO conversations (name, type) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, name);
            stmt.setString(2, type.name().toLowerCase()); // 'direct' hoặc 'group'
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                System.err.println("[ConversationDAO] Creating conversation failed, no rows affected");
                return null;
            }
            
            // Lấy ID vừa tạo
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Conversation convo = new Conversation();
                    convo.setId(generatedKeys.getInt(1));
                    convo.setName(name);
                    convo.setType(type);
                    
                    System.out.println("[ConversationDAO] Created conversation ID: " + convo.getId());
                    return convo;
                } else {
                    System.err.println("[ConversationDAO] Creating conversation failed, no ID obtained");
                    return null;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error createConversation: " + e.getMessage());
            throw e;
        }
    }

    public Conversation findConversationById(int id) {
        // Viết hàm tương tự findDirectConversation...
        return null; // Tự implement
    }

//    private Conversation mapResultSetToConversation(ResultSet rs) throws SQLException {
//        return new Conversation(
//            rs.getInt("id"),
//            rs.getString("name"),
//            ConversationType.valueOf(rs.getString("type")),
//            rs.getTimestamp("created_at").toLocalDateTime()
//        );
//    }
    private Conversation mapResultSetToConversation(ResultSet rs) throws SQLException {
        Conversation convo = new Conversation();
        convo.setId(rs.getInt("id"));
        convo.setName(rs.getString("name"));
        
        // Xử lý 'direct' hoặc 'group'
        String typeStr = rs.getString("type");
        if (typeStr.equalsIgnoreCase("direct")) {
            convo.setType(ConversationType.DIRECT);
        } else {
            convo.setType(ConversationType.GROUP);
        }
        
        convo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return convo;
    }
}