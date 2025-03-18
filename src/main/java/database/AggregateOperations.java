package database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.*;

public class AggregateOperations {
    private DatabaseManager dbManager;

    public AggregateOperations() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Get a list of all tables in the database
     */
    public String[] getTableNames() {
        try {
            DatabaseMetaData metadata = dbManager.getConnection().getMetaData();
            ResultSet resultSet = metadata.getTables(null, null, "%", new String[]{"TABLE"});

            List<String> tableNames = new ArrayList<>();
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

    /**
     * Get a list of columns for the selected table
     */
    public String[] getColumnNames(String tableName) {
        try {
            DatabaseMetaData metadata = dbManager.getConnection().getMetaData();
            ResultSet columns = metadata.getColumns(null, null, tableName, null);

            List<String> columnNames = new ArrayList<>();
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME"));
            }

            return columnNames.toArray(new String[0]);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving column names: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return new String[0];
        }
    }

    /**
     * Execute an aggregate function and populate the table model with results
     */
    public void executeAggregate(String tableName, String function, String column,
                                 String whereClause, String groupBy, DefaultTableModel tableModel) {
        if (dbManager.getConnection() == null) {
            JOptionPane.showMessageDialog(null, "Please connect to the database first",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT ");

            // Add group by columns to SELECT clause if provided
            if (groupBy != null && !groupBy.isEmpty()) {
                queryBuilder.append(groupBy).append(", ");
            }

            // Add the aggregate function
            if (function.equalsIgnoreCase("COUNT(*)")) {
                queryBuilder.append("COUNT(*) AS count_all");
            } else {
                queryBuilder.append(function).append("(").append(column).append(") AS ")
                        .append(function.toLowerCase()).append("_").append(column);
            }

            // Add FROM clause
            queryBuilder.append(" FROM ").append(tableName);

            // Add WHERE clause if provided
            if (whereClause != null && !whereClause.isEmpty()) {
                queryBuilder.append(" WHERE ").append(whereClause);
            }

            // Add GROUP BY clause if provided
            if (groupBy != null && !groupBy.isEmpty()) {
                queryBuilder.append(" GROUP BY ").append(groupBy);
            }

            // Execute the query
            Statement stmt = dbManager.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(queryBuilder.toString());

            // Populate the table model with results
            populateTableModel(rs, tableModel);

            stmt.close();
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error executing aggregate query: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Execute a predefined aggregate query and populate the table model
     */
    public void executePredefinedAggregate(String queryName, DefaultTableModel tableModel) {
        if (dbManager.getConnection() == null) {
            JOptionPane.showMessageDialog(null, "Please connect to the database first",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = getPredefinedQuery(queryName);
        if (query == null || query.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Predefined query not found",
                    "Query Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Statement stmt = dbManager.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(query);

            // Populate the table model with results
            populateTableModel(rs, tableModel);

            stmt.close();
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error executing predefined query: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get the predefined aggregate query by name
     */
    private String getPredefinedQuery(String queryName) {
        Map<String, String> predefinedQueries = new HashMap<>();

        // Average transaction amount by merchant
        predefinedQueries.put("Average Transaction by Merchant",
                "SELECT m.merchant_name, AVG(t.amount) AS average_amount " +
                        "FROM transactions t " +
                        "JOIN merchants m ON t.merchant_id = m.merchant_id " +
                        "GROUP BY m.merchant_name " +
                        "ORDER BY average_amount DESC");

        // Transaction count by card type
        predefinedQueries.put("Transaction Count by Card Type",
                "SELECT c.card_type, COUNT(t.transaction_id) AS transaction_count " +
                        "FROM transactions t " +
                        "JOIN cards c ON t.card_id = c.card_id " +
                        "GROUP BY c.card_type " +
                        "ORDER BY transaction_count DESC");

        // Total settlement amount by merchant
        predefinedQueries.put("Total Settlement by Merchant",
                "SELECT m.merchant_name, SUM(s.total_amount) AS total_settlement " +
                        "FROM settlements s " +
                        "JOIN merchants m ON s.merchant_id = m.merchant_id " +
                        "GROUP BY m.merchant_name " +
                        "ORDER BY total_settlement DESC");

        // Transaction status counts
        predefinedQueries.put("Transaction Status Summary",
                "SELECT transaction_status, COUNT(*) AS count " +
                        "FROM transactions " +
                        "GROUP BY transaction_status");

        // Transaction volume by month
        predefinedQueries.put("Monthly Transaction Volume",
                "SELECT DATE_FORMAT(transaction_timestamp, '%Y-%m') AS month, " +
                        "COUNT(*) AS transaction_count, " +
                        "SUM(amount) AS total_amount " +
                        "FROM transactions " +
                        "GROUP BY month " +
                        "ORDER BY month");

        // Average, minimum, and maximum transaction amounts
        predefinedQueries.put("Transaction Amount Statistics",
                "SELECT AVG(amount) AS average_amount, " +
                        "MIN(amount) AS minimum_amount, " +
                        "MAX(amount) AS maximum_amount " +
                        "FROM transactions");

        return predefinedQueries.get(queryName);
    }

    /**
     * Get a list of all predefined aggregate query names
     */
    public String[] getPredefinedQueryNames() {
        return new String[] {
                "Average Transaction by Merchant",
                "Transaction Count by Card Type",
                "Total Settlement by Merchant",
                "Transaction Status Summary",
                "Monthly Transaction Volume",
                "Transaction Amount Statistics"
        };
    }

    /**
     * Populate a table model with the results of a query
     */
    private void populateTableModel(ResultSet rs, DefaultTableModel tableModel) throws SQLException {
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
    }
}