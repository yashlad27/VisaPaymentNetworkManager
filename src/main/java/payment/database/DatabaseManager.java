package main.java.payment.database;

import javax.swing.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton class to manage database connections.
 * This class handles connections to the MySQL database for the Visa Payment Network Manager.
 */
public class DatabaseManager {
  private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
  private static DatabaseManager instance;
  private static Connection connection;

  // Database connection parameters
  private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/visa_final_spring";
  private static final String DEFAULT_USER = "root";
  private static final String DEFAULT_PASSWORD = "test123";

  /**
   * Private constructor to prevent instantiation outside of this class.
   */
  DatabaseManager() {
    // Private constructor for singleton pattern
  }

  /**
   * Get the singleton instance of the DatabaseManager.
   *
   * @return The DatabaseManager instance
   */
  public static synchronized DatabaseManager getInstance() {
    if (instance == null) {
      instance = new DatabaseManager();
    }
    return instance;
  }

  /**
   * Connect to the database with default parameters.
   *
   * @return The database connection or null if connection fails
   */
  public Connection connect() {
    return connect(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
  }

  /**
   * Connect to the database with custom parameters.
   *
   * @param url The database URL
   * @param user The database username
   * @param password The database password
   * @return The database connection or null if connection fails
   */
  public Connection connect(String url, String user, String password) {
    try {
      // Load the MySQL JDBC driver
      Class.forName("com.mysql.cj.jdbc.Driver");

      // Establish connection
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
   * Get the current database connection.
   *
   * @return The current connection or null if not connected
   */
  public static Connection getConnection() {
    return connection;
  }

  /**
   * Disconnect from the database.
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
   *
   * @param query The SQL query to execute
   * @return The result set or null if the query failed
   * @throws SQLException If a database access error occurs
   */
  public ResultSet executeQuery(String query) throws SQLException {
    if (connection == null) {
      throw new SQLException("Database connection not established");
    }

    Statement statement = connection.createStatement();
    return statement.executeQuery(query);
  }

  /**
   * Execute a SQL update statement (INSERT, UPDATE, DELETE, etc.).
   *
   * @param query The SQL update statement to execute
   * @return The number of rows affected
   * @throws SQLException If a database access error occurs
   */
  public int executeUpdate(String query) throws SQLException {
    if (connection == null) {
      throw new SQLException("Database connection not established");
    }

    Statement statement = connection.createStatement();
    return statement.executeUpdate(query);
  }

  /**
   * Create a prepared statement for the given SQL query.
   *
   * @param sql The SQL query
   * @return The prepared statement
   * @throws SQLException If a database access error occurs
   */
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    if (connection == null) {
      throw new SQLException("Database connection not established");
    }

    return connection.prepareStatement(sql);
  }
}