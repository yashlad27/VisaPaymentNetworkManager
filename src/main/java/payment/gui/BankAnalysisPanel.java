package payment.gui;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import payment.database.DatabaseManager;
import payment.database.QueryManager;

/**
 * Panel for analyzing bank performance and transaction data.
 */
public class BankAnalysisPanel extends JPanel {
  private final DatabaseManager dbManager;
  private final Connection connection;
  private int selectedBankId = -1; // Currently selected bank ID

  // UI Components
  private JTable bankTable;
  private JTable transactionTable;
  private JLabel bankNameLabel;
  private JLabel bankCodeLabel;
  private JLabel totalTransactionsLabel;
  private JLabel totalAmountLabel;
  private JLabel avgAmountLabel;
  private JLabel successRateLabel;
  private JLabel uniqueMerchantsLabel;

  // Table Models
  private DefaultTableModel issuingBankModel;
  private DefaultTableModel acquiringBankModel;
  private DefaultTableModel bankPerformanceModel;
  private DefaultTableModel transactionModel;

  // Formatters
  private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

  // Auto-refresh timer
  private Timer refreshTimer;
  private final int REFRESH_INTERVAL = 60000; // 1 minute

  /**
   * Constructor for the bank analysis panel.
   *
   * @param dbManager The database manager
   */
  public BankAnalysisPanel(DatabaseManager dbManager) {
    this.dbManager = dbManager;
    this.connection = dbManager.getConnection();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    // Initialize table models first
    initTableModels();
    
    // Initialize UI components
    initComponents();

    // Load initial data
    refreshData();

    // Set up auto-refresh timer
    setupRefreshTimer();
  }

  /**
   * Initialize UI components.
   */
  private void initComponents() {
    // Create main panel with card layout
    JPanel mainPanel = new JPanel(new CardLayout());

    // Create bank details panel
    JPanel bankDetailsPanel = createBankDetailsPanel();

    // Create transaction history panel
    JPanel transactionPanel = createTransactionPanel();

    // Add panels to main panel
    mainPanel.add(bankDetailsPanel, "bankDetails");
    mainPanel.add(transactionPanel, "transactions");

    // Add main panel to this panel
    add(mainPanel, BorderLayout.CENTER);
  }

  private void initTableModels() {
    // Bank table model
    String[] bankColumns = {"Bank Name", "Total Transactions", "Success Rate", "Total Amount"};
    bankTable = new JTable(new DefaultTableModel(bankColumns, 0));

    // Transaction table model
    String[] transactionColumns = {
            "Timestamp", "Merchant", "Card Type", "Amount",
            "Status", "Auth Code", "Response Code"
    };
    transactionModel = new DefaultTableModel(transactionColumns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    transactionTable = new JTable(transactionModel);
  }

  private JPanel createBankDetailsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create labels
    bankNameLabel = new JLabel("Bank: ");
    bankCodeLabel = new JLabel("Code: ");
    totalTransactionsLabel = new JLabel("Total Transactions: ");
    totalAmountLabel = new JLabel("Total Amount: ");
    avgAmountLabel = new JLabel("Average Amount: ");
    successRateLabel = new JLabel("Success Rate: ");
    uniqueMerchantsLabel = new JLabel("Unique Merchants: ");

    // Create info panel
    JPanel infoPanel = new JPanel(new GridLayout(7, 1, 5, 5));
    infoPanel.add(bankNameLabel);
    infoPanel.add(bankCodeLabel);
    infoPanel.add(totalTransactionsLabel);
    infoPanel.add(totalAmountLabel);
    infoPanel.add(avgAmountLabel);
    infoPanel.add(successRateLabel);
    infoPanel.add(uniqueMerchantsLabel);

    // Add bank table
    JScrollPane tableScroll = new JScrollPane(bankTable);
    bankTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && bankTable.getSelectedRow() != -1) {
        selectedBankId = (int) bankTable.getValueAt(bankTable.getSelectedRow(), 0);
        loadBankDetails();
        loadTransactionHistory();
      }
    });

    // Add components to panel
    panel.add(infoPanel, BorderLayout.NORTH);
    panel.add(tableScroll, BorderLayout.CENTER);

    return panel;
  }

  private JPanel createTransactionPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Add transaction table
    JScrollPane tableScroll = new JScrollPane(transactionTable);
    panel.add(tableScroll, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Set up the refresh timer.
   */
  private void setupRefreshTimer() {
    refreshTimer = new Timer(REFRESH_INTERVAL, e -> refreshData());
    refreshTimer.start();
  }

  /**
   * Refresh all data from the database.
   */
  private void refreshData() {
    SwingWorker<Void, Void> worker = new SwingWorker<>() {
      @Override
      protected Void doInBackground() {
        try {
          loadBankPerformanceData();
        } catch (Exception e) {
          e.printStackTrace();
          SwingUtilities.invokeLater(() ->
                  JOptionPane.showMessageDialog(
                          BankAnalysisPanel.this,
                          "Error refreshing data: " + e.getMessage(),
                          "Database Error",
                          JOptionPane.ERROR_MESSAGE
                  )
          );
        }
        return null;
      }
    };
    worker.execute();
  }

  private void loadBankPerformanceData() {
    try {
      Connection conn = DatabaseManager.getConnection();
      ResultSet rs = QueryManager.executePredefinedQuery(
              QueryManager.TOP_BANKS_QUERY,
              conn
      );

      List<Map<String, Object>> data = QueryManager.resultSetToList(rs);

      // Update your existing table model with the new data
      DefaultTableModel model = (DefaultTableModel) bankTable.getModel();
      model.setRowCount(0);

      for (Map<String, Object> row : data) {
        // Safely convert BigDecimal to double without direct casting
        Object successRateObj = row.get("success_rate");
        double successRate = 0.0;
        if (successRateObj instanceof BigDecimal) {
          successRate = ((BigDecimal) successRateObj).doubleValue();
        } else if (successRateObj instanceof Double) {
          successRate = (Double) successRateObj;
        } else if (successRateObj != null) {
          successRate = Double.parseDouble(successRateObj.toString());
        }
        
        Object totalAmountObj = row.get("total_amount");
        double totalAmount = 0.0;
        if (totalAmountObj instanceof BigDecimal) {
          totalAmount = ((BigDecimal) totalAmountObj).doubleValue();
        } else if (totalAmountObj instanceof Double) {
          totalAmount = (Double) totalAmountObj;
        } else if (totalAmountObj != null) {
          totalAmount = Double.parseDouble(totalAmountObj.toString());
        }

        model.addRow(new Object[]{
                row.get("bank_name"),
                row.get("total_transactions"),
                String.format("%.2f%%", successRate),
                String.format("$%.2f", totalAmount)
        });
      }

    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this,
              "Error loading bank performance data: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }

  private void calculateBankSuccessRate() {
    try {
      // Example: Calculate success rate for the last 30 days
      Date endDate = new Date(System.currentTimeMillis());
      Date startDate = new Date(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000));

      String query = "SELECT " +
              "(SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*)) * 100 as success_rate " +
              "FROM Transaction " +
              "WHERE acquiring_bank_id = ? " +
              "AND timestamp BETWEEN ? AND ?";

      Connection conn = DatabaseManager.getConnection();
      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, selectedBankId);
      stmt.setDate(2, new java.sql.Date(startDate.getTime()));
      stmt.setDate(3, new java.sql.Date(endDate.getTime()));

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        double successRate = rs.getDouble("success_rate");
        successRateLabel.setText(String.format("Success Rate: %.2f%%", successRate));
      }

    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this,
              "Error calculating success rate: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }

  private void loadBankDetails() {
    try {
      String query = "SELECT ab.*, " +
              "COUNT(t.transaction_id) as total_transactions, " +
              "SUM(t.amount) as total_amount, " +
              "AVG(t.amount) as avg_amount, " +
              "COUNT(DISTINCT t.merchant_id) as unique_merchants " +
              "FROM AcquiringBank ab " +
              "LEFT JOIN Transaction t ON ab.acquiring_bank_id = t.acquiring_bank_id " +
              "WHERE ab.acquiring_bank_id = ? " +
              "GROUP BY ab.acquiring_bank_id";

      Connection conn = DatabaseManager.getConnection();
      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, selectedBankId);

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        bankNameLabel.setText("Bank: " + rs.getString("bank_name"));
        bankCodeLabel.setText("Code: " + rs.getString("bank_code"));
        totalTransactionsLabel.setText("Total Transactions: " + rs.getInt("total_transactions"));
        totalAmountLabel.setText("Total Amount: $" + String.format("%.2f", rs.getDouble("total_amount")));
        avgAmountLabel.setText("Average Amount: $" + String.format("%.2f", rs.getDouble("avg_amount")));
      }

    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this,
              "Error loading bank details: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }

  private void loadTransactionHistory() {
    try {
      String query = "SELECT t.*, c.card_type, pm.merchant_name, " +
              "a.auth_code, ar.response_code, ar.response_message " +
              "FROM Transaction t " +
              "JOIN Card c ON t.card_id = c.card_id " +
              "JOIN PaymentMerchant pm ON t.merchant_id = pm.merchant_id " +
              "LEFT JOIN Authorization a ON t.transaction_id = a.transaction_id " +
              "LEFT JOIN AuthResponse ar ON a.auth_id = ar.auth_id " +
              "WHERE t.acquiring_bank_id = ? " +
              "ORDER BY t.timestamp DESC " +
              "LIMIT 100";

      Connection conn = DatabaseManager.getConnection();
      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, selectedBankId);

      ResultSet rs = stmt.executeQuery();
      DefaultTableModel model = (DefaultTableModel) transactionTable.getModel();
      model.setRowCount(0);

      while (rs.next()) {
        model.addRow(new Object[]{
                rs.getTimestamp("timestamp"),
                rs.getString("merchant_name"),
                rs.getString("card_type"),
                String.format("$%.2f", rs.getDouble("amount")),
                rs.getString("status"),
                rs.getString("auth_code"),
                rs.getString("response_code")
        });
      }

    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this,
              "Error loading transaction history: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }
}