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

        // DBConnection.getConnection() now returns a fresh connection each time.
        // try-with-resources closes it automatically at the end of the block.
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("Database connected successfully.");

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
            System.exit(1);
        } catch (RuntimeException e) {
            // getConnection() wraps SQLException in RuntimeException on failure
            System.err.println("Connection failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
