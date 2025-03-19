package payment.gui;

import payment.database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Panel for analyzing bank performance and transaction data.
 */
public class BankAnalysisPanel extends JPanel {
  private final DatabaseManager dbManager;
  private final Connection connection;

  // UI Components
  private JTabbedPane bankTypeTabs;
  private JTable issuingBankTable;
  private DefaultTableModel issuingBankModel;
  private JTable acquiringBankTable;
  private DefaultTableModel acquiringBankModel;
  private JTable bankPerformanceTable;
  private DefaultTableModel bankPerformanceModel;

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

    // Create UI Components
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
    // Create title and control panel at the top
    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);

    // Create main content panel with tabbed pane for bank types
    bankTypeTabs = new JTabbedPane();

    // Create issuing banks panel
    JPanel issuingBanksPanel = createIssuingBanksPanel();
    bankTypeTabs.addTab("Issuing Banks", issuingBanksPanel);

    // Create acquiring banks panel
    JPanel acquiringBanksPanel = createAcquiringBanksPanel();
    bankTypeTabs.addTab("Acquiring Banks", acquiringBanksPanel);

    // Create bank performance panel
    JPanel bankPerformancePanel = createBankPerformancePanel();
    bankTypeTabs.addTab("Bank Performance Metrics", bankPerformancePanel);

    // Add tabbed pane to main panel
    add(bankTypeTabs, BorderLayout.CENTER);
  }

  /**
   * Create the header panel with title and controls.
   *
   * @return The header panel
   */
  private JPanel createHeaderPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(0, 0, 10, 0));

    // Title label
    JLabel titleLabel = new JLabel("Bank Analysis");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
    panel.add(titleLabel, BorderLayout.WEST);

    // Controls panel
    JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    // Date range combobox
    String[] dateRanges = {"All Time", "Last 7 Days", "Last 30 Days", "Custom..."};
    JComboBox<String> dateRangeCombo = new JComboBox<>(dateRanges);
    dateRangeCombo.setPreferredSize(new Dimension(120, 25));

    // Refresh button
    JButton refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(this::refreshButtonClicked);

    controlsPanel.add(new JLabel("Date Range:"));
    controlsPanel.add(dateRangeCombo);
    controlsPanel.add(refreshButton);

    panel.add(controlsPanel, BorderLayout.EAST);

    return panel;
  }

  /**
   * Create the issuing banks panel.
   *
   * @return The issuing banks panel
   */
  private JPanel createIssuingBanksPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create table model and table
    String[] columns = {"Bank Name", "Bank Code", "Transaction Count", "Total Amount", "Average Transaction"};
    issuingBankModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Make all cells non-editable
      }
    };
    issuingBankTable = new JTable(issuingBankModel);
    issuingBankTable.getTableHeader().setReorderingAllowed(false);

    // Add description label
    JLabel descLabel = new JLabel("Transaction volume by issuing banks (card issuers)");
    descLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
    panel.add(descLabel, BorderLayout.NORTH);

    // Add table to panel with scroll pane
    panel.add(new JScrollPane(issuingBankTable), BorderLayout.CENTER);

    return panel;
  }

  /**
   * Create the acquiring banks panel.
   *
   * @return The acquiring banks panel
   */
  private JPanel createAcquiringBanksPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create table model and table
    String[] columns = {"Bank Name", "Bank Code", "Transaction Count", "Total Amount", "Average Transaction"};
    acquiringBankModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Make all cells non-editable
      }
    };
    acquiringBankTable = new JTable(acquiringBankModel);
    acquiringBankTable.getTableHeader().setReorderingAllowed(false);

    // Add description label
    JLabel descLabel = new JLabel("Transaction volume by acquiring banks (merchant banks)");
    descLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
    panel.add(descLabel, BorderLayout.NORTH);

    // Add table to panel with scroll pane
    panel.add(new JScrollPane(acquiringBankTable), BorderLayout.CENTER);

    return panel;
  }

  /**
   * Create the bank performance metrics panel.
   *
   * @return The bank performance panel
   */
  private JPanel createBankPerformancePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create table model and table
    String[] columns = {"Bank Name", "Transaction Count", "Approval Rate", "Avg Processing Time (sec)"};
    bankPerformanceModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Make all cells non-editable
      }
    };
    bankPerformanceTable = new JTable(bankPerformanceModel);
    bankPerformanceTable.getTableHeader().setReorderingAllowed(false);

    // Add description label
    JLabel descLabel = new JLabel("Performance metrics for issuing banks");
    descLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
    panel.add(descLabel, BorderLayout.NORTH);

    // Add table to panel with scroll pane
    panel.add(new JScrollPane(bankPerformanceTable), BorderLayout.CENTER);

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
   * Handle refresh button click.
   *
   * @param e The action event
   */
  private void refreshButtonClicked(ActionEvent e) {
    refreshData();
  }

  /**
   * Refresh all data from the database.
   */
  private void refreshData() {
    SwingWorker<Void, Void> worker = new SwingWorker<>() {
      @Override
      protected Void doInBackground() {
        try {
          updateIssuingBanks();
          updateAcquiringBanks();
          updateBankPerformance();
        } catch (SQLException ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(
                  BankAnalysisPanel.this,
                  "Error refreshing data: " + ex.getMessage(),
                  "Database Error",
                  JOptionPane.ERROR_MESSAGE
          );
        }
        return null;
      }
    };
    worker.execute();
  }

  /**
   * Update issuing banks table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateIssuingBanks() throws SQLException {
    String query = "SELECT " +
            "    ib.bank_name, " +
            "    ib.bank_code, " +
            "    COUNT(t.transaction_id) as transaction_count, " +
            "    SUM(t.amount) as total_amount, " +
            "    ROUND(AVG(t.amount), 2) as avg_transaction_amount " +
            "FROM Transaction t " +
            "JOIN Card c ON t.card_id = c.card_id " +
            "JOIN IssuingBank ib ON c.issuing_bank_id = ib.issuing_bank_id " +
            "GROUP BY ib.bank_name, ib.bank_code " +
            "ORDER BY transaction_count DESC";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      // Clear existing data
      SwingUtilities.invokeLater(() -> issuingBankModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        final String bankName = rs.getString("bank_name");
        final String bankCode = rs.getString("bank_code");
        final int count = rs.getInt("transaction_count");
        final double amount = rs.getDouble("total_amount");
        final double avgAmount = rs.getDouble("avg_transaction_amount");

        SwingUtilities.invokeLater(() ->
                issuingBankModel.addRow(new Object[] {
                        bankName,
                        bankCode,
                        String.format("%,d", count),
                        currencyFormat.format(amount),
                        currencyFormat.format(avgAmount)
                })
        );
      }
    }
  }

  /**
   * Update acquiring banks table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateAcquiringBanks() throws SQLException {
    String query = "SELECT " +
            "    ab.bank_name, " +
            "    ab.bank_code, " +
            "    COUNT(t.transaction_id) as transaction_count, " +
            "    SUM(t.amount) as total_amount, " +
            "    ROUND(AVG(t.amount), 2) as avg_transaction_amount " +
            "FROM Transaction t " +
            "JOIN AcquiringBank ab ON t.acquiring_bank_id = ab.acquiring_bank_id " +
            "GROUP BY ab.bank_name, ab.bank_code " +
            "ORDER BY transaction_count DESC";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      // Clear existing data
      SwingUtilities.invokeLater(() -> acquiringBankModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        final String bankName = rs.getString("bank_name");
        final String bankCode = rs.getString("bank_code");
        final int count = rs.getInt("transaction_count");
        final double amount = rs.getDouble("total_amount");
        final double avgAmount = rs.getDouble("avg_transaction_amount");

        SwingUtilities.invokeLater(() ->
                acquiringBankModel.addRow(new Object[] {
                        bankName,
                        bankCode,
                        String.format("%,d", count),
                        currencyFormat.format(amount),
                        currencyFormat.format(avgAmount)
                })
        );
      }
    }
  }

  /**
   * Update bank performance metrics table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateBankPerformance() throws SQLException {
    String query = "SELECT " +
            "    ib.bank_name, " +
            "    COUNT(t.transaction_id) as transaction_count, " +
            "    ROUND((COUNT(CASE WHEN t.status = 'Approved' THEN 1 END) / COUNT(*)) * 100, 2) as approval_rate, " +
            "    ROUND(AVG(TIMESTAMPDIFF(SECOND, t.timestamp, a.timestamp)), 2) as avg_processing_time_seconds " +
            "FROM Transaction t " +
            "JOIN Card c ON t.card_id = c.card_id " +
            "JOIN IssuingBank ib ON c.issuing_bank_id = ib.issuing_bank_id " +
            "JOIN Authorization a ON t.transaction_id = a.transaction_id " +
            "GROUP BY ib.bank_name " +
            "ORDER BY approval_rate DESC";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      // Clear existing data
      SwingUtilities.invokeLater(() -> bankPerformanceModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        final String bankName = rs.getString("bank_name");
        final int count = rs.getInt("transaction_count");
        final double approvalRate = rs.getDouble("approval_rate");
        final double processingTime = rs.getDouble("avg_processing_time_seconds");

        SwingUtilities.invokeLater(() ->
                bankPerformanceModel.addRow(new Object[] {
                        bankName,
                        String.format("%,d", count),
                        String.format("%.2f%%", approvalRate),
                        String.format("%.2f", processingTime)
                })
        );
      }
    }
  }
}