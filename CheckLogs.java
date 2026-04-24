import java.sql.*;

public class CheckLogs {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/ecommerce?serverTimezone=Asia/Ho_Chi_Minh";
        String user = "root";
        String password = "123"; // Adjust if needed

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM chat_interactions ORDER BY created_at DESC LIMIT 5")) {
            
            System.out.println("--- LAST 5 CHAT INTERACTIONS ---");
            while (rs.next()) {
                System.out.println("Time: " + rs.getTimestamp("created_at"));
                System.out.println("User: " + rs.getString("user_message"));
                System.out.println("AI: " + rs.getString("ai_response"));
                System.out.println("Intent: " + rs.getString("intent"));
                System.out.println("--------------------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
