package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection – JDBC connection factory for supermarket_db.
 *
 * Returns a FRESH connection on every call. Each DAO method receives
 * its own connection which is safely closed by the try-with-resources
 * block in the caller — no shared singleton that could be closed early.
 *
 * Compatible with MySQL Connector/J 8.x and above.
 */
public class DBConnection {

    // ----------------------------------------------------------------
    // Connection parameters – set PASSWORD before running.
    // ----------------------------------------------------------------
    private static final String HOST     = "localhost";
    private static final int    PORT     = 3306;
    private static final String DATABASE = "supermarket_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "password";   // <-- set your MySQL password here

    private static final String URL = String.format(
        "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true",
        HOST, PORT, DATABASE
    );

    // Private constructor – utility class, not instantiable.
    private DBConnection() { }

    // ----------------------------------------------------------------
    // getConnection() – opens and returns a new Connection each time.
    // The caller must close it (try-with-resources handles this).
    // ----------------------------------------------------------------

    /**
     * Opens and returns a new JDBC {@link Connection} to {@code supermarket_db}.
     *
     * <p>Each call creates a fresh connection. Callers are responsible for
     * closing it — all DAO methods use try-with-resources which handles
     * this automatically.</p>
     *
     * @return a new, open {@link Connection}
     * @throws RuntimeException wrapping {@link SQLException} if the connection fails
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Database connection failed.");
            System.err.println("SQL State : " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message   : " + e.getMessage());
            throw new RuntimeException("Cannot establish database connection.", e);
        }
    }
}
