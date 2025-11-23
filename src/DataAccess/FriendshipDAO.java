package DataAccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Model.FriendshipStatus;
import Model.User;

public class FriendshipDAO {

    /**
     * Kiểm tra xem 2 người đã là bạn hoặc đang có lời mời pending chưa
     */
    public boolean areFriendsOrPending(int userId1, int userId2) {
        String sql = "SELECT COUNT(*) FROM friendships " +
                     "WHERE (requester_id = ? AND addressee_id = ?) " +
                     "   OR (requester_id = ? AND addressee_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return false;
    }

    /**
     * Lấy danh sách LỜI MỜI ĐANG CHỜ mà userId nhận được
     * ✅ SỬA: 'PENDING' → 'pending' (khớp với database)
     */
    public List<User> getPendingFriendRequests(int userId) {
        List<User> pending = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.password, u.phone_number, u.online, u.created_at " +
                     "FROM users u " +
                     "JOIN friendships f ON u.id = f.requester_id " +
                     "WHERE f.addressee_id = ? AND f.status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setPhoneNumber(rs.getString("phone_number"));
                u.setOnline(rs.getBoolean("online"));
                // ✅ Không cần set password và createdAt để bảo mật
                pending.add(u);
            }
            System.out.println("[FriendshipDAO] Found " + pending.size() + " pending requests for user " + userId);
        } catch (SQLException e) { 
            System.err.println("[FriendshipDAO] Error getPendingFriendRequests: " + e.getMessage());
            e.printStackTrace(); 
        }
        return pending;
    }

    /**
     * Lấy danh sách BẠN BÈ của userId (status = 'accepted')
     */
    public List<User> getFriendsByUserId(int userId) {
        List<User> friends = new ArrayList<>();
        String sql = "SELECT DISTINCT u.id, u.username, u.phone_number, u.online " +
                     "FROM users u " +
                     "JOIN friendships f ON " +
                     "  (f.requester_id = ? AND f.addressee_id = u.id AND f.status = 'accepted') OR " +
                     "  (f.addressee_id = ? AND f.requester_id = u.id AND f.status = 'accepted')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setPhoneNumber(rs.getString("phone_number"));
                u.setOnline(rs.getBoolean("online"));
                friends.add(u);
            }
            System.out.println("[FriendshipDAO] Found " + friends.size() + " friends for user " + userId);
        } catch (SQLException e) { 
            System.err.println("[FriendshipDAO] Error getFriendsByUserId: " + e.getMessage());
            e.printStackTrace(); 
        }
        return friends;
    }

    /**
     * Tạo lời mời kết bạn
     * ✅ SỬA: 'PENDING' → 'pending'
     */
    public void createFriendRequest(int requesterId, int addresseeId) throws SQLException {
        String sql = "INSERT INTO friendships (requester_id, addressee_id, status) VALUES (?, ?, 'pending')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterId);
            ps.setInt(2, addresseeId);
            ps.executeUpdate();
            System.out.println("[FriendshipDAO] Friend request created: " + requesterId + " -> " + addresseeId);
        } catch (SQLException e) {
            System.err.println("[FriendshipDAO] Lỗi createFriendRequest: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Chấp nhận lời mời kết bạn
     * ✅ SỬA: Dùng 'pending' và 'accepted' (chữ thường)
     */
    public void acceptFriendRequest(int requesterId, int addresseeId) throws SQLException {
        String sql = "UPDATE friendships SET status = 'accepted' " +
                     "WHERE requester_id = ? AND addressee_id = ? AND status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterId);
            ps.setInt(2, addresseeId);
            int rows = ps.executeUpdate();
            System.out.println("[FriendshipDAO] Friend request accepted: " + requesterId + " -> " + addresseeId + " (" + rows + " rows)");
        } catch (SQLException e) {
            System.err.println("[FriendshipDAO] Error acceptFriendRequest: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Cập nhật trạng thái friendship
     * ✅ SỬA: Chuyển status.name() thành chữ thường
     */
    public void updateFriendshipStatus(int requesterId, int addresseeId, FriendshipStatus status) {
        String sql = "UPDATE friendships SET status = ? WHERE requester_id = ? AND addressee_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name().toLowerCase()); // ✅ QUAN TRỌNG: chuyển thành chữ thường
            ps.setInt(2, requesterId);
            ps.setInt(3, addresseeId);
            int rows = ps.executeUpdate();
            System.out.println("[FriendshipDAO] Updated status to " + status + ": " + requesterId + " -> " + addresseeId + " (" + rows + " rows)");
        } catch (SQLException e) {
            System.err.println("[FriendshipDAO] Error updateFriendshipStatus: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Từ chối lời mời kết bạn
     */
    public void rejectFriendRequest(int requesterId, int addresseeId) {
        String sql = "DELETE FROM friendships " +
                     "WHERE requester_id = ? AND addressee_id = ? AND status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterId);
            ps.setInt(2, addresseeId);
            int rows = ps.executeUpdate();
            System.out.println("[FriendshipDAO] Friend request rejected: " + requesterId + " -> " + addresseeId + " (" + rows + " rows)");
        } catch (SQLException e) {
            System.err.println("[FriendshipDAO] Error rejectFriendRequest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Hủy kết bạn
     */
    public void unfriend(int userId1, int userId2) {
        String sql = "DELETE FROM friendships " +
                     "WHERE ((requester_id = ? AND addressee_id = ?) OR " +
                     "(requester_id = ? AND addressee_id = ?)) AND status = 'accepted'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);
            int rows = ps.executeUpdate();
            System.out.println("[FriendshipDAO] Unfriended: " + userId1 + " <-> " + userId2 + " (" + rows + " rows)");
        } catch (SQLException e) {
            System.err.println("[FriendshipDAO] Error unfriend: " + e.getMessage());
            e.printStackTrace();
        }
    }
}