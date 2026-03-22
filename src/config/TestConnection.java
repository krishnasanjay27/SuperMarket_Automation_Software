package config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * TestConnection – quick smoke-test for DBConnection.
 *
 * Compile & run (from the project root):
 *
 *   Windows:
 *     javac -cp "lib/*;src" -d out src/config/DBConnection.java src/config/TestConnection.java
 *     java  -cp "lib/*;out"  config.TestConnection
 *
 *   Linux / macOS:
 *     javac -cp "lib/*:src" -d out src/config/DBConnection.java src/config/TestConnection.java
 *     java  -cp "lib/*:out"  config.TestConnection
 */
public class TestConnection {

    public static void main(String[] args) {

        System.out.println("=== SAS – JDBC Connection Test ===");

        Connection conn = DBConnection.getConnection();

        if (conn != null) {
            try {
                // Print server metadata to confirm the right DB is reached.
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("Driver  : " + meta.getDriverName()
                                   + " v" + meta.getDriverVersion());
                System.out.println("Server  : " + meta.getDatabaseProductName()
                                   + " " + meta.getDatabaseProductVersion());
                System.out.println("URL     : " + meta.getURL());
                System.out.println("User    : " + meta.getUserName());
                System.out.println("AutoCommit : " + conn.getAutoCommit());
                System.out.println("\nAll checks passed. JDBC connection is working correctly.");

            } catch (SQLException e) {
                System.err.println("Error reading metadata: " + e.getMessage());
            } finally {
                // Close the shared connection after the test.
                DBConnection.closeConnection();
            }
        } else {
            System.out.println("Connection is null – check your credentials or MySQL server status.");
            System.exit(1);
        }
    }
}
