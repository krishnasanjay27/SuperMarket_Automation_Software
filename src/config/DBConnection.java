package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection – Singleton-style JDBC connection manager.
 *
 * Usage:
 *   Connection conn = DBConnection.getConnection();
 *
 * Compatible with MySQL Connector/J 8.x and above.
 * The JDBC driver is loaded automatically via the Service Provider
 * Interface (SPI) – no Class.forName() call is required.
 */
public class DBConnection {

    // ----------------------------------------------------------------
    // Connection parameters – update PASSWORD before running.
    // ----------------------------------------------------------------
    private static final String HOST     = "localhost";
    private static final int    PORT     = 3306;
    private static final String DATABASE = "supermarket_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "password";          // <-- set your password here

    /**
     * JDBC URL for MySQL Connector/J 8+.
     * serverTimezone and useSSL are explicitly set to prevent
     * common warnings / errors in JDBC 8 environments.
     */
    private static final String URL = String.format(
        "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true",
        HOST, PORT, DATABASE
    );

    // Cached connection instance (Singleton).
    private static Connection connection = null;

    // ----------------------------------------------------------------
    // Private constructor – prevents external instantiation.
    // ----------------------------------------------------------------
    private DBConnection() { }

    // ----------------------------------------------------------------
    // getConnection() – returns an open, reusable Connection object.
    // ----------------------------------------------------------------

    /**
     * Returns a live JDBC {@link Connection} to {@code supermarket_db}.
     *
     * <p>The connection is created on the first call and reused on every
     * subsequent call, provided it is still open. If the cached connection
     * has been closed (e.g., after a timeout), a new one is established.</p>
     *
     * @return a valid {@link Connection}, or {@code null} if the connection
     *         could not be established.
     */
    public static Connection getConnection() {
        try {
            // Reuse existing connection only if it is still open.
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully.");

        } catch (SQLException e) {
            System.out.println("Database connection failed.");
            System.err.println("SQL State : " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message   : " + e.getMessage());
        }

        return connection;
    }

    // ----------------------------------------------------------------
    // closeConnection() – gracefully closes the cached connection.
    // Call this once when the application is shutting down.
    // ----------------------------------------------------------------

    /**
     * Closes the cached {@link Connection} and resets the singleton.
     * Safe to call even if the connection is already {@code null} or closed.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("Error while closing connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
