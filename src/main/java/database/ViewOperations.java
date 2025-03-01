package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ViewOperations {
  private DatabaseManager dbManager;

  public ViewOperations() {
    this.dbManager = DatabaseManager.getInstance();
  }

  /**
   * Get a list of all views in the database
   */
  public String[] getViewList() {
    List<String> viewList = new ArrayList<>();
    Connection conn = dbManager.getConnection();

    if (conn == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return new String[0];
    }

    try {
      // Get database metadata
      DatabaseMetaData metaData = conn.getMetaData();

      // Get all views (MySQL views are returned as tables with type "VIEW")
      ResultSet rs = metaData.getTables(null, null, "%", new String[]{"VIEW"});

      while (rs.next()) {
        viewList.add(rs.getString("TABLE_NAME"));
      }

      rs.close();
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error retrieving views: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    return viewList.toArray(new String[0]);
  }

  /**
   * Get the SQL definition of a view
   */
  public String getViewDefinition(String viewName) {
    StringBuilder definition = new StringBuilder();
    Connection conn = dbManager.getConnection();

    if (conn == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return "";
    }

    try {
      // In MySQL, you can get the view definition from INFORMATION_SCHEMA.VIEWS
      String query = "SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS " +
              "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";

      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setString(1, viewName);
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        definition.append(rs.getString("VIEW_DEFINITION"));
      } else {
        definition.append("/* View definition not found */");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error retrieving view definition: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
      definition.append("/* Error retrieving view definition */");
    }

    return definition.toString();
  }

  /**
   * Execute a view (SELECT * FROM view_name) and populate the table model
   */
  public void executeView(String viewName, DefaultTableModel tableModel) {
    Connection conn = dbManager.getConnection();

    if (conn == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      // Build query to select from the view
      String query = "SELECT * FROM " + viewName;

      // Execute query and populate table model
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);

      // Clear existing data
      tableModel.setRowCount(0);
      tableModel.setColumnCount(0);

      // Get metadata
      ResultSetMetaData metaData = rs.getMetaData();
      int columnCount = metaData.getColumnCount();

      // Add columns
      for (int i = 1; i <= columnCount; i++) {
        tableModel.addColumn(metaData.getColumnName(i));
      }

      // Add rows
      while (rs.next()) {
        Object[] row = new Object[columnCount];
        for (int i = 1; i <= columnCount; i++) {
          row[i-1] = rs.getObject(i);
        }
        tableModel.addRow(row);
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error executing view: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Create a new view using the provided SQL statement
   */
  public boolean createView(String createViewSQL) {
    Connection conn = dbManager.getConnection();

    if (conn == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    try {
      // Validate that this is a CREATE VIEW statement
      String sql = createViewSQL.trim().toUpperCase();
      if (!sql.startsWith("CREATE") && !sql.contains("VIEW")) {
        JOptionPane.showMessageDialog(null, "Please enter a valid CREATE VIEW statement",
                "SQL Error", JOptionPane.ERROR_MESSAGE);
        return false;
      }

      // Execute the CREATE VIEW statement
      Statement stmt = conn.createStatement();
      stmt.execute(createViewSQL);
      stmt.close();

      return true;
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error creating view: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  /**
   * Drop an existing view
   */
  public boolean dropView(String viewName) {
    Connection conn = dbManager.getConnection();

    if (conn == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    try {
      // Build and execute DROP VIEW statement
      String dropSQL = "DROP VIEW " + viewName;
      Statement stmt = conn.createStatement();
      stmt.execute(dropSQL);
      stmt.close();

      return true;
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error dropping view: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }
}