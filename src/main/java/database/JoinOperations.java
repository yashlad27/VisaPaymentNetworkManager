package database;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class JoinOperations {
  private DatabaseManager dbManager;
  private Map<String, String> predefinedJoins;

  public JoinOperations() {
    this.dbManager = DatabaseManager.getInstance();
    initializePredefinedJoins();
  }

  private void initializePredefinedJoins() {
    predefinedJoins = new HashMap<>();

    // Cardholder and their cards join
    predefinedJoins.put("Cardholders and their Cards",
            "SELECT c.cardholder_id, c.first_name, c.last_name, c.email, " +
                    "cd.card_id, cd.card_type, cd.expiry_date, cd.is_active " +
                    "FROM cardholders c " +
                    "JOIN cards cd ON c.cardholder_id = cd.cardholder_id");

    // Merchants and their transactions join
    predefinedJoins.put("Merchants and their Transactions",
            "SELECT m.merchant_id, m.merchant_name, m.merchant_category_code, " +
                    "t.transaction_id, t.amount, t.currency, t.transaction_timestamp, t.transaction_status " +
                    "FROM merchants m " +
                    "JOIN transactions t ON m.merchant_id = t.merchant_id");

    // Transactions with authorization details join
    predefinedJoins.put("Transactions with Authorization Details",
            "SELECT t.transaction_id, t.amount, t.transaction_status, " +
                    "a.authorization_id, a.auth_code, a.status AS auth_status, " +
                    "ar.response_id, ar.response_code, ar.response_message " +
                    "FROM transactions t " +
                    "JOIN authorizations a ON t.transaction_id = a.transaction_id " +
                    "JOIN authorization_responses ar ON a.authorization_id = ar.authorization_id");

    // Cards with issuing bank details join
    predefinedJoins.put("Cards with Issuing Bank Details",
            "SELECT c.card_id, c.card_type, c.expiry_date, " +
                    "i.issuing_bank_id, i.bank_name, i.bank_code " +
                    "FROM cards c " +
                    "JOIN issuing_banks i ON c.issuing_bank_id = i.issuing_bank_id");

    // Merchants with acquiring bank details join
    predefinedJoins.put("Merchants with Acquiring Bank Details",
            "SELECT m.merchant_id, m.merchant_name, m.merchant_category_code, " +
                    "a.acquiring_bank_id, a.bank_name, a.bank_code, a.settlement_account " +
                    "FROM merchants m " +
                    "JOIN acquiring_banks a ON m.acquiring_bank_id = a.acquiring_bank_id");

    // Complete transaction details join
    predefinedJoins.put("Complete Transaction Details",
            "SELECT t.transaction_id, t.amount, t.currency, t.transaction_timestamp, t.transaction_status, " +
                    "ch.first_name AS cardholder_first_name, ch.last_name AS cardholder_last_name, " +
                    "c.card_type, ib.bank_name AS issuing_bank, " +
                    "m.merchant_name, m.merchant_category_code, ab.bank_name AS acquiring_bank, " +
                    "a.auth_code, a.status AS auth_status, " +
                    "ar.response_code, ar.response_message " +
                    "FROM transactions t " +
                    "JOIN cards c ON t.card_id = c.card_id " +
                    "JOIN cardholders ch ON c.cardholder_id = ch.cardholder_id " +
                    "JOIN issuing_banks ib ON c.issuing_bank_id = ib.issuing_bank_id " +
                    "JOIN merchants m ON t.merchant_id = m.merchant_id " +
                    "JOIN acquiring_banks ab ON m.acquiring_bank_id = ab.acquiring_bank_id " +
                    "LEFT JOIN authorizations a ON t.transaction_id = a.transaction_id " +
                    "LEFT JOIN authorization_responses ar ON a.authorization_id = ar.authorization_id");

    // Settlement details join
    predefinedJoins.put("Settlement Details",
            "SELECT s.settlement_id, s.total_amount, s.currency, s.status, s.settlement_date, " +
                    "m.merchant_name, m.merchant_category_code, " +
                    "ab.bank_name AS acquiring_bank, ab.settlement_account " +
                    "FROM settlements s " +
                    "JOIN merchants m ON s.merchant_id = m.merchant_id " +
                    "JOIN acquiring_banks ab ON m.acquiring_bank_id = ab.acquiring_bank_id");
  }

  public String[] getPredefinedJoinNames() {
    return predefinedJoins.keySet().toArray(new String[0]);
  }

  public void executeJoin(String joinName, DefaultTableModel tableModel) {
    if (dbManager.getConnection() == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    String query = predefinedJoins.get(joinName);
    if (query == null) {
      JOptionPane.showMessageDialog(null, "Join query not found",
              "Query Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      ResultSet resultSet = dbManager.executeQuery(query);
      displayResults(resultSet, tableModel);
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error executing join: " + e.getMessage(),
              "Join Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  public void executeCustomJoin(String query, DefaultTableModel tableModel) {
    if (dbManager.getConnection() == null) {
      JOptionPane.showMessageDialog(null, "Please connect to the database first",
              "Database Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      ResultSet resultSet = dbManager.executeQuery(query);
      displayResults(resultSet, tableModel);
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error executing custom join: " + e.getMessage(),
              "Join Error", JOptionPane.ERROR_MESSAGE);
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
      Object[] row = new Object[columnCount];
      for (int i = 1; i <= columnCount; i++) {
        row[i - 1] = resultSet.getObject(i);
      }
      tableModel.addRow(row);
    }
  }

  // Method to get the structure of tables for join builder
  public Map<String, String[]> getTableStructures() {
    Map<String, String[]> tableStructures = new HashMap<>();

    try {
      DatabaseMetaData metaData = dbManager.getConnection().getMetaData();
      ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

      while (tables.next()) {
        String tableName = tables.getString("TABLE_NAME");
        ResultSet columns = metaData.getColumns(null, null, tableName, null);

        java.util.List<String> columnList = new java.util.ArrayList<>();
        while (columns.next()) {
          columnList.add(columns.getString("COLUMN_NAME"));
        }

        tableStructures.put(tableName, columnList.toArray(new String[0]));
      }
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, "Error getting table structures: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    return tableStructures;
  }
}