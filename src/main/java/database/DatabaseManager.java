package database;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class DatabaseManager {
  private static DatabaseManager instance;
  private Connection connection;
  private Statement statement;

  public DatabaseManager() {
    // Private constructor for singleton pattern
  }

  public static DatabaseManager getInstance() {
    if (instance == null) {
      instance = new DatabaseManager();
    }
    return instance;
  }

  public boolean connect(String url, String user, String password) {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      connection = DriverManager.getConnection(url, user, password);
      statement = connection.createStatement();
      return true;
    } catch (ClassNotFoundException | SQLException e) {
      JOptionPane.showMessageDialog(null, "Connection error: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  public ResultSet executeQuery(String query) throws SQLException {
    return statement.executeQuery(query);
  }

  public int executeUpdate(String query) throws SQLException {
    return statement.executeUpdate(query);
  }

  public Connection getConnection() {
    return connection;
  }

  public void disconnect() {
    try {
      if (statement != null) statement.close();
      if (connection != null) connection.close();
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error disconnecting: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  // Helper method to populate a table model from a result set
  public void populateTableModel(ResultSet resultSet, DefaultTableModel tableModel) throws SQLException {
    // Clear the table model
    tableModel.setRowCount(0);
    tableModel.setColumnCount(0);

    // Get result set metadata
    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = metaData.getColumnCount();

    // Add column headers
    for (int i = 1; i <= columnCount; i++) {
      tableModel.addColumn(metaData.getColumnName(i));
    }

    // Add data rows
    while (resultSet.next()) {
      Vector<Object> row = new Vector<>();
      for (int i = 1; i <= columnCount; i++) {
        row.add(resultSet.getObject(i));
      }
      tableModel.addRow(row);
    }
  }
  public Connection connect() {
    String url = "jdbc:mysql://localhost:3306/visa_payment_network";
    String user = "root";
    String password = "test123";
    return connect(url, user, password) ? connection : null;
  }
}