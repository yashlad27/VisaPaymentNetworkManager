package payment.database;

import javax.swing.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton class to manage database connections for the Visa Payment Network Manager.
 * <p>
 * This class provides centralized database connectivity services using the Singleton pattern,
 * ensuring that only one database connection is created and maintained throughout the
 * application's lifecycle. It handles establishing connections to MySQL databases,
 * executing queries, and managing connection resources.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Connection management with automatic resource handling</li>
 *   <li>Query execution with proper error handling and logging</li>
 *   <li>Support for both statement and prepared statement operations</li>
 *   <li>User-friendly error messages via dialog boxes</li>
 *   <li>Comprehensive logging of database operations</li>
 * </ul>
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * // Get the database manager instance
 * DatabaseManager dbManager = DatabaseManager.getInstance();
 *
 * // Connect to the database
 * Connection conn = dbManager.connect();
 *
 * // Execute a query
 * try {
 *     ResultSet rs = dbManager.executeQuery("SELECT * FROM Transaction");
 *     // Process results...
 * } catch (SQLException e) {
 *     // Handle error...
 * }
 *
 * // Close connection when done
 * dbManager.disconnect();
 * </pre>
 * </p>
 */
public class DatabaseManager {
    /**
     * Logger for recording database operations and errors
     */
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /**
     * Singleton instance of the DatabaseManager
     */
    private static DatabaseManager instance;

    /**
     * The active database connection
     */
    private static Connection connection;

    /**
     * Default database URL for MySQL connection
     */
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/visa_final_spring";

    /**
     * Default database username
     */
    private static final String DEFAULT_USER = "root";

    /**
     * Default database password
     */
    private static final String DEFAULT_PASSWORD = "test123";

    /**
     * Private constructor to prevent instantiation outside of this class.
     * <p>
     * This enforces the Singleton pattern, ensuring only one instance of
     * DatabaseManager exists in the application.
     * </p>
     */
    DatabaseManager() {
        // Private constructor for singleton pattern
    }

    /**
     * Get the singleton instance of the DatabaseManager.
     * <p>
     * If an instance doesn't exist, one will be created. Otherwise, the
     * existing instance is returned. This method is thread-safe due to
     * the synchronized keyword.
     * </p>
     *
     * @return The singleton DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Connect to the database using default connection parameters.
     * <p>
     * This method attempts to establish a connection to the MySQL database
     * using the default URL, username, and password defined as constants
     * in this class.
     * </p>
     *
     * @return The active database connection if successful, or null if connection fails
     * @see #connect(String, String, String)
     */
    public Connection connect() {
        return connect(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    /**
     * Connect to the database using custom connection parameters.
     * <p>
     * This method attempts to establish a connection to a MySQL database
     * using the specified URL, username, and password. It handles loading
     * the JDBC driver and reports any errors encountered during the process.
     * </p>
     *
     * @param url      The JDBC URL for the database (e.g., "jdbc:mysql://localhost:3306/dbname")
     * @param user     The database username for authentication
     * @param password The database password for authentication
     * @return The active database connection if successful, or null if connection fails
     * @throws IllegalArgumentException if url or user is null or empty
     */
    public Connection connect(String url, String user, String password) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL cannot be null or empty");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("Database username cannot be null or empty");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(url, user, password);
            LOGGER.log(Level.INFO, "Database connection established successfully");
            return connection;
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC driver not found", e);
            JOptionPane.showMessageDialog(null,
                    "MySQL JDBC driver not found: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
            JOptionPane.showMessageDialog(null,
                    "Connection error: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * Get the current active database connection.
     * <p>
     * This method returns the current active database connection or null
     * if no connection has been established yet. It's useful for components
     * that need direct access to the connection object.
     * </p>
     *
     * @return The current active Connection object, or null if not connected
     */
    public static Connection getConnection() {
        return connection;
    }

    /**
     * Disconnect from the database and release resources.
     * <p>
     * This method closes the active database connection and sets the
     * connection reference to null. It should be called when the application
     * is shutting down or when the connection is no longer needed to
     * properly release database resources.
     * </p>
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                LOGGER.log(Level.INFO, "Database connection closed");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing database connection", e);
                JOptionPane.showMessageDialog(null,
                        "Error disconnecting: " + e.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Execute a SQL query and return the result set.
     * <p>
     * This method creates a Statement object and executes the provided SQL
     * query. It's suitable for SELECT statements that return result sets.
     * The caller is responsible for closing the ResultSet and Statement
     * objects after use.
     * </p>
     *
     * @param query The SQL query to execute (typically a SELECT statement)
     * @return The ResultSet containing the query results
     * @throws SQLException             If the connection is null or a database access error occurs
     * @throws IllegalArgumentException If the query is null or empty
     */
    public ResultSet executeQuery(String query) throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection not established");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }

        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    /**
     * Execute a SQL update statement (INSERT, UPDATE, DELETE, etc.).
     * <p>
     * This method creates a Statement object and executes the provided SQL
     * update statement. It's suitable for operations that modify data and
     * don't return result sets. The caller is responsible for closing the
     * Statement object after use.
     * </p>
     *
     * @param query The SQL update statement to execute
     * @return The number of rows affected by the SQL statement
     * @throws SQLException             If the connection is null or a database access error occurs
     * @throws IllegalArgumentException If the query is null or empty
     */
    public int executeUpdate(String query) throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection not established");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }

        Statement statement = connection.createStatement();
        return statement.executeUpdate(query);
    }

}