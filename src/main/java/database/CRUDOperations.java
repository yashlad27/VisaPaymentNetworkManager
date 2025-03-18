package database;

import database.DatabaseManager;

import java.sql.*;

public class CRUDOperations {
    private DatabaseManager databaseManager;

    public CRUDOperations(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // ✅ Add Customer
    public void addCustomer(String firstName, String email, String phone) throws SQLException {
        String sql = "INSERT INTO CardHolders (first_name, email, phone, created_at) VALUES (?, ?, ?, NOW())";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, firstName);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.executeUpdate();
        }
    }

    // ✅ Retrieve All Customers
    public ResultSet getCustomers() throws SQLException {
        if (databaseManager == null || databaseManager.getConnection() == null) {
            throw new SQLException("Database connection is not initialized.");
        }

        String sql = "SELECT cardholder_id, first_name, email, phone FROM CardHolders";
        return databaseManager.getConnection().createStatement().executeQuery(sql);
    }

    // ✅ Update Customer Info
    public void updateCustomer(int cardholderId, String newEmail, String newPhone) throws SQLException {
        String sql = "UPDATE CardHolders SET email = ?, phone = ? WHERE cardholder_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newEmail);
            stmt.setString(2, newPhone);
            stmt.setInt(3, cardholderId);
            stmt.executeUpdate();
        }
    }

    // ✅ Delete Customer
    public void deleteCustomer(int cardholderId) throws SQLException {
        String sql = "DELETE FROM CardHolders WHERE cardholder_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cardholderId);
            stmt.executeUpdate();
        }
    }

    // ✅ Add Merchant
    public void addMerchant(String name, String category, String terminalId, double processingFee, double settlementAmount) throws SQLException {
        String sql = "INSERT INTO Merchants (merchant_name, merchant_category_code, terminal_id, processing_fee, settlement_amount, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setString(3, terminalId);
            stmt.setDouble(4, processingFee);
            stmt.setDouble(5, settlementAmount);
            stmt.executeUpdate();
        }
    }

    // ✅ Retrieve All Merchants
    public ResultSet getMerchants() throws SQLException {
        String sql = "SELECT merchant_id, merchant_name, merchant_category_code, terminal_id FROM Merchants";
        Connection conn = databaseManager.getConnection();
        return conn.createStatement().executeQuery(sql);
    }

    // ✅ Delete Merchant
    public void deleteMerchant(int merchantId) throws SQLException {
        String sql = "DELETE FROM Merchants WHERE merchant_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, merchantId);
            stmt.executeUpdate();
        }
    }
}