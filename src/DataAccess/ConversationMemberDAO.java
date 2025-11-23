package DataAccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConversationMemberDAO {

    /**
     * Thêm một member vào conversation
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean addMember(int conversationId, int userId) throws SQLException {
        String sql = "INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("[ConversationMemberDAO] Added user " + userId 
                    + " to conversation " + conversationId);
                return true;
            } else {
                System.err.println("[ConversationMemberDAO] Failed to add user " + userId 
                    + " to conversation " + conversationId);
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("[ConversationMemberDAO] Error addMember: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Xóa một member khỏi conversation
     */
    public boolean removeMember(int conversationId, int userId) throws SQLException {
        String sql = "DELETE FROM conversation_members WHERE conversation_id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);
            
            int rows = stmt.executeUpdate();
            
            System.out.println("[ConversationMemberDAO] Removed user " + userId 
                + " from conversation " + conversationId + " (" + rows + " rows)");
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("[ConversationMemberDAO] Error removeMember: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy danh sách user IDs trong một conversation
     */
    public List<Integer> getMemberIds(int conversationId) throws SQLException {
        List<Integer> memberIds = new ArrayList<>();
        String sql = "SELECT user_id FROM conversation_members WHERE conversation_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conversationId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                memberIds.add(rs.getInt("user_id"));
            }
            
            System.out.println("[ConversationMemberDAO] Found " + memberIds.size() 
                + " members in conversation " + conversationId);
            
            return memberIds;
            
        } catch (SQLException e) {
            System.err.println("[ConversationMemberDAO] Error getMemberIds: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Kiểm tra user có phải member của conversation không
     */
    public boolean isMember(int conversationId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM conversation_members WHERE conversation_id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("[ConversationMemberDAO] Error isMember: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy danh sách conversation IDs mà user tham gia
     */
    public List<Integer> getConversationIdsByUserId(int userId) throws SQLException {
        List<Integer> conversationIds = new ArrayList<>();
        String sql = "SELECT conversation_id FROM conversation_members WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                conversationIds.add(rs.getInt("conversation_id"));
            }
            
            System.out.println("[ConversationMemberDAO] User " + userId 
                + " is in " + conversationIds.size() + " conversations");
            
            return conversationIds;
            
        } catch (SQLException e) {
            System.err.println("[ConversationMemberDAO] Error getConversationIdsByUserId: " + e.getMessage());
            throw e;
        }
    }
    
    public int getPartnerId(int conversationId, int currentUserId) throws SQLException {
        String sql = "SELECT user_id FROM conversation_members WHERE conversation_id = ? AND user_id != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conversationId);
            stmt.setInt(2, currentUserId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }
        return -1; // Không tìm thấy
    }
}