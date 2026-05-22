import java.sql.*;
public class QueryUsers {
    public static void main(String[] args) throws Exception {
        String db = "jdbc:sqlite:data/auction.db";
        try (Connection conn = DriverManager.getConnection(db)) {
            try (Statement st = conn.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT username, role, created_at FROM users")) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3));
                    }
                    System.out.println("TOTAL=" + count);
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
