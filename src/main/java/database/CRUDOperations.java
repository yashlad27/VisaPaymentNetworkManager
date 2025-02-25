package database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.Vector;

public class CRUDOperations {
  private DatabaseManager dbManager;

  public CRUDOperations() {
    this.dbManager = DatabaseManager.getInstance();
  }

  public void loadTableData(String tableName, DefaultTableModel tableModel) {
    if (dbManager.getConnection() == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    String query = "SELECT * FROM " + tableName;

    try {
      ResultSet resultSet = dbManager.executeQuery(query);
      dbManager.populateTableModel(resultSet, tableModel);
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error loading table data: " + e.getMessage(),
              "Query Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  public void insertRecord(String tableName, Vector<String> columnNames, Vector<String> values) {
    if (dbManager.getConnection() == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      // Build the INSERT query
      StringBuilder queryBuilder = new StringBuilder();
      queryBuilder.append("INSERT INTO ").append(tableName).append(" (");

      for (int i = 0; i < columnNames.size(); i++) {
        queryBuilder.append(columnNames.get(i));
        if (i < columnNames.size() - 1) {
          queryBuilder.append(", ");
        }
      }

      queryBuilder.append(") VALUES (");

      for (int i = 0; i < values.size(); i++) {
        queryBuilder.append("'").append(values.get(i)).append("'");
        if (i < values.size() - 1) {
          queryBuilder.append(", ");
        }
      }

      queryBuilder.append(")");

      // Execute the INSERT query
      int rowsAffected = dbManager.executeUpdate(queryBuilder.toString());

      JOptionPane.showMessageDialog(null, "Record inserted successfully",
              "Insert Success", JOptionPane.INFORMATION_MESSAGE);
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error inserting record: " + e.getMessage(),
              "Insert Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  public void updateRecord(String tableName, String primaryKeyColumn, String primaryKeyValue,
                           Vector<String> columnNames, Vector<String> values) {
    if (dbManager.getConnection() == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      // Build the UPDATE query
      StringBuilder queryBuilder = new StringBuilder();
      queryBuilder.append("UPDATE ").append(tableName).append(" SET ");

      for (int i = 0; i < columnNames.size(); i++) {
        queryBuilder.append(columnNames.get(i)).append(" = '").append(values.get(i)).append("'");
        if (i < columnNames.size() - 1) {
          queryBuilder.append(", ");
        }
      }

      queryBuilder.append(" WHERE ").append(primaryKeyColumn).append(" = '").append(primaryKeyValue).append("'");

      // Execute the UPDATE query
      int rowsAffected = dbManager.executeUpdate(queryBuilder.toString());

      JOptionPane.showMessageDialog(null, "Record updated successfully",
              "Update Success", JOptionPane.INFORMATION_MESSAGE);
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error updating record: " + e.getMessage(),
              "Update Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  public void deleteRecord(String tableName, String primaryKeyColumn, String primaryKeyValue) {
    if (dbManager.getConnection() == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      // Build the DELETE query
      String query = "DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + " = '" + primaryKeyValue + "'";

      // Execute the DELETE query
      int rowsAffected = dbManager.executeUpdate(query);

      JOptionPane.showMessageDialog(null, "Record deleted successfully",
              "Delete Success", JOptionPane.INFORMATION_MESSAGE);
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error deleting record: " + e.getMessage(),
              "Delete Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  public String[] getTableNames() {
    try {
      DatabaseMetaData metadata = dbManager.getConnection().getMetaData();
      ResultSet resultSet = metadata.getTables(null, null, "%", new String[]{"TABLE"});

      Vector<String> tableNames = new Vector<>();
      while (resultSet.next()) {
        tableNames.add(resultSet.getString("TABLE_NAME"));
      }

      return tableNames.toArray(new String[0]);
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error retrieving table names: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return new String[0];
    }
  }

  public String getPrimaryKeyColumn(String tableName) {
    try {
      DatabaseMetaData dbMetaData = dbManager.getConnection().getMetaData();
      ResultSet primaryKeyResultSet = dbMetaData.getPrimaryKeys(null, null, tableName);

      if (primaryKeyResultSet.next()) {
        return primaryKeyResultSet.getString("COLUMN_NAME");
      } else {
        return null;
      }
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error retrieving primary key: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }

  public Vector<String> getColumnNames(String tableName, boolean excludeAutoIncrement) {
    Vector<String> columnNames = new Vector<>();

    try {
      DatabaseMetaData dbMetaData = dbManager.getConnection().getMetaData();
      ResultSet columnsResultSet = dbMetaData.getColumns(null, null, tableName, null);

      while (columnsResultSet.next()) {
        String columnName = columnsResultSet.getString("COLUMN_NAME");
        String isAutoIncrement = columnsResultSet.getString("IS_AUTOINCREMENT");

        // Skip auto-increment columns if requested
        if (excludeAutoIncrement && isAutoIncrement != null && isAutoIncrement.equals("YES")) {
          continue;
        }

        columnNames.add(columnName);
      }
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error retrieving column names: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    return columnNames;
  }
}