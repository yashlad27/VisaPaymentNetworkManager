package database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class QueryExecutor {
  private DatabaseManager dbManager;

  public QueryExecutor() {
    this.dbManager = DatabaseManager.getInstance();
  }

  public void executeQuery(String query, DefaultTableModel tableModel) {
    if (dbManager.getConnection() == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (query.isEmpty()) {
      JOptionPane.showMessageDialog(null, "Please enter a SQL query",
              "Query Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      // Check if the query is a SELECT query
      if (query.toLowerCase().startsWith("select")) {
        // Execute the query and display results
        ResultSet resultSet = dbManager.executeQuery(query);
        displayResults(resultSet, tableModel);
      } else {
        // Execute update query (INSERT, UPDATE, DELETE, etc.)
        int rowsAffected = dbManager.executeUpdate(query);
        JOptionPane.showMessageDialog(null, rowsAffected + " row(s) affected",
                "Query Result", JOptionPane.INFORMATION_MESSAGE);

        // Clear the table model
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
      }
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error executing query: " + e.getMessage(),
              "Query Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void displayResults(ResultSet resultSet, DefaultTableModel tableModel) throws SQLException {
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

  public void saveQueryToFile(String query, String filePath) {
    try {
      java.io.FileWriter writer = new java.io.FileWriter(filePath);
      writer.write(query);
      writer.close();
      JOptionPane.showMessageDialog(null, "Query saved successfully",
              "Save Query", JOptionPane.INFORMATION_MESSAGE);
    } catch (java.io.IOException e) {
      JOptionPane.showMessageDialog(null, "Error saving query: " + e.getMessage(),
              "Save Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  public String loadQueryFromFile(String filePath) {
    try {
      java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(filePath));
      StringBuilder queryBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        queryBuilder.append(line).append("\n");
      }
      reader.close();
      return queryBuilder.toString();
    } catch (java.io.IOException e) {
      JOptionPane.showMessageDialog(null, "Error loading query: " + e.getMessage(),
              "Load Error", JOptionPane.ERROR_MESSAGE);
      return "";
    }
  }
}