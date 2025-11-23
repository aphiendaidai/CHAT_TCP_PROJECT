package DataAccess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Thay thế bằng thông tin CSDL của bạn
    private static final String URL = "jdbc:mysql://localhost:3306/chat_app_project";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Nạp driver
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy MySQL JDBC Driver!");
            e.printStackTrace();
        }
    }

    /**
     * Lấy một kết nối mới đến CSDL
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    public static void main(String[] args) {
        Connection conn = null;
        try {
            // Lấy kết nối
            conn = DatabaseConnection.getConnection();
            
            // Kiểm tra trạng thái kết nối
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Kết nối đến MySQL thành công!");
                System.out.println("Database: " + conn.getMetaData().getDatabaseProductName() 
                                   + " - Version: " + conn.getMetaData().getDatabaseProductVersion());
            } else {
                System.out.println("❌ Kết nối không thành công (có lỗi xảy ra nhưng không ném ra ngoại lệ SQL).");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi kết nối cơ sở dữ liệu!");
            System.out.println("Chi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            // Đóng kết nối để giải phóng tài nguyên
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Đã đóng kết nối.");
                } catch (SQLException e) {
                    System.err.println("Lỗi khi đóng kết nối:");
                    e.printStackTrace();
                }
            }
        }
    
}
}