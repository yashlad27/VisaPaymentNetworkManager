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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Main dashboard panel showing key performance indicators and summary charts.
 */
public class DashboardPanel extends JPanel {
  private final DatabaseManager dbManager;
  private final Connection connection;

  // UI Components for KPI section
  private JLabel totalTransactionsLabel;
  private JLabel totalValueLabel;
  private JLabel todayCountLabel;
  private JLabel todayValueLabel;
  private JLabel successRateLabel;

  // UI Components for data tables
  private JTable topMerchantsTable;
  private DefaultTableModel merchantsTableModel;
  private JTable cardTypeTable;
  private DefaultTableModel cardTypeTableModel;

  // UI Components for visualizations
  private TransactionStatusPanel transactionStatusPanel;
  private HourlyVolumePanel hourlyVolumePanel;

  // Formatters
  private final DecimalFormat percentFormat = new DecimalFormat("#0.00%");
  private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

  // Auto-refresh timer
  private Timer refreshTimer;
  private final int REFRESH_INTERVAL = 60000; // 1 minute

  /**
   * Constructor for the dashboard panel.
   *
   * @param dbManager The database manager
   */
  public DashboardPanel(DatabaseManager dbManager) {
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

    // Create main content panel
    JPanel contentPanel = new JPanel(new GridBagLayout());

    // Create KPI panel (top section)
    JPanel kpiPanel = createKPIPanel();

    // Create charts panel (middle section)
    JPanel chartsPanel = createChartsPanel();

    // Add panels to content with GridBagLayout
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 0, 10, 0);
    contentPanel.add(kpiPanel, gbc);

    gbc.gridy = 1;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    contentPanel.add(chartsPanel, gbc);

    // Add content panel to main panel
    add(new JScrollPane(contentPanel), BorderLayout.CENTER);
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
    JLabel titleLabel = new JLabel("Payment Network Dashboard");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
    panel.add(titleLabel, BorderLayout.WEST);

    // Controls panel (date range, refresh)
    JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    // Date range combobox
    String[] dateRanges = {"Today", "Last 7 Days", "Last 30 Days", "Custom..."};
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
   * Create the Key Performance Indicators panel.
   *
   * @return The KPI panel
   */
  private JPanel createKPIPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 5, 10, 0));
    panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Key Performance Indicators",
            TitledBorder.LEFT,
            TitledBorder.TOP
    ));

    // Create KPI cards
    panel.add(createKPICard("Total Transactions", "", totalTransactionsLabel = new JLabel("...")));
    panel.add(createKPICard("Total Value", "", totalValueLabel = new JLabel("...")));
    panel.add(createKPICard("Today's Count", "vs Yesterday", todayCountLabel = new JLabel("...")));
    panel.add(createKPICard("Today's Value", "vs Yesterday", todayValueLabel = new JLabel("...")));
    panel.add(createKPICard("Success Rate", "", successRateLabel = new JLabel("...")));

    return panel;
  }

  /**
   * Create a KPI card with title, subtitle, and value.
   *
   * @param title The card title
   * @param subtitle The card subtitle
   * @param valueLabel The label to display the value
   * @return The card panel
   */
  private JPanel createKPICard(String title, String subtitle, JLabel valueLabel) {
    JPanel card = new JPanel(new BorderLayout());
    card.setBackground(Color.WHITE);
    card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
    ));

    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

    JLabel subtitleLabel = new JLabel(subtitle);
    subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
    subtitleLabel.setForeground(Color.GRAY);

    valueLabel.setFont(new Font("Arial", Font.BOLD, 20));
    valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

    JPanel headerPanel = new JPanel(new GridLayout(2, 1));
    headerPanel.setOpaque(false);
    headerPanel.add(titleLabel);

    if (!subtitle.isEmpty()) {
      headerPanel.add(subtitleLabel);
    }

    card.add(headerPanel, BorderLayout.NORTH);
    card.add(valueLabel, BorderLayout.CENTER);

    return card;
  }

  /**
   * Create the charts panel with various visualizations.
   *
   * @return The charts panel
   */
  private JPanel createChartsPanel() {
    JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));

    // Card type distribution panel
    JPanel cardTypePanel = new JPanel(new BorderLayout());
    cardTypePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Card Type Distribution",
            TitledBorder.LEFT,
            TitledBorder.TOP
    ));

    // Create card type table
    String[] cardTypeColumns = {"Card Type", "Transaction Count", "Total Amount", "% of Total"};
    cardTypeTableModel = new DefaultTableModel(cardTypeColumns, 0);
    cardTypeTable = new JTable(cardTypeTableModel);
    cardTypePanel.add(new JScrollPane(cardTypeTable), BorderLayout.CENTER);

    // Transaction status panel - Using the new TransactionStatusPanel
    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Transaction Status",
            TitledBorder.LEFT,
            TitledBorder.TOP
    ));

    // Create and add the new TransactionStatusPanel
    transactionStatusPanel = new TransactionStatusPanel(dbManager);
    statusPanel.add(transactionStatusPanel, BorderLayout.CENTER);

    // Hourly volume panel - Using the new HourlyVolumePanel
    JPanel hourlyPanel = new JPanel(new BorderLayout());
    hourlyPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Hourly Transaction Volume",
            TitledBorder.LEFT,
            TitledBorder.TOP
    ));

    // Create and add the new HourlyVolumePanel
    hourlyVolumePanel = new HourlyVolumePanel(dbManager, true);
    hourlyPanel.add(hourlyVolumePanel, BorderLayout.CENTER);

    // Top merchants panel
    JPanel merchantsPanel = new JPanel(new BorderLayout());
    merchantsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Top Merchants",
            TitledBorder.LEFT,
            TitledBorder.TOP
    ));

    // Create merchants table
    String[] merchantColumns = {"Merchant", "Category", "Transactions", "Total Amount"};
    merchantsTableModel = new DefaultTableModel(merchantColumns, 0);
    topMerchantsTable = new JTable(merchantsTableModel);
    merchantsPanel.add(new JScrollPane(topMerchantsTable), BorderLayout.CENTER);

    // Add all panels
    panel.add(cardTypePanel);
    panel.add(statusPanel);
    panel.add(hourlyPanel);
    panel.add(merchantsPanel);

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
   * Refresh all dashboard data from the database.
   */
  private void refreshData() {
    SwingWorker<Void, Void> worker = new SwingWorker<>() {
      @Override
      protected Void doInBackground() {
        try {
          updateKPIs();
          updateCardTypeDistribution();
          updateTopMerchants();

          // Refresh the transaction status and hourly volume panels
          transactionStatusPanel.refreshData();
          hourlyVolumePanel.refreshData();

        } catch (SQLException ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(
                  DashboardPanel.this,
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
   * Update Key Performance Indicators.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateKPIs() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      // Total transactions
      try (ResultSet rs = stmt.executeQuery(
              "SELECT COUNT(*) as total_transactions FROM Transaction")) {
        if (rs.next()) {
          int total = rs.getInt("total_transactions");
          SwingUtilities.invokeLater(() ->
                  totalTransactionsLabel.setText(String.format("%,d", total)));
        }
      }

      // Total value
      try (ResultSet rs = stmt.executeQuery(
              "SELECT SUM(amount) as total_value FROM Transaction")) {
        if (rs.next()) {
          double total = rs.getDouble("total_value");
          SwingUtilities.invokeLater(() ->
                  totalValueLabel.setText(currencyFormat.format(total)));
        }
      }

      // Today's count vs yesterday
      try (ResultSet rs = stmt.executeQuery(
              "SELECT " +
                      "  (SELECT COUNT(*) FROM Transaction WHERE DATE(timestamp) = CURDATE()) as today_count, " +
                      "  (SELECT COUNT(*) FROM Transaction WHERE DATE(timestamp) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)) as yesterday_count, " +
                      "  IFNULL(ROUND(((SELECT COUNT(*) FROM Transaction WHERE DATE(timestamp) = CURDATE()) / " +
                      "         NULLIF((SELECT COUNT(*) FROM Transaction WHERE DATE(timestamp) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)), 0) - 1) * 100, 2), 0) as percent_change")) {
        if (rs.next()) {
          int todayCount = rs.getInt("today_count");
          double percentChange = rs.getDouble("percent_change");
          String trend = percentChange >= 0 ? "▲" : "▼";
          String color = percentChange >= 0 ? "green" : "red";

          SwingUtilities.invokeLater(() ->
                  todayCountLabel.setText(String.format("<html>%,d <span style='color:%s'>%s %.2f%%</span></html>",
                          todayCount, color, trend, Math.abs(percentChange))));
        }
      }

      // Today's value vs yesterday
      try (ResultSet rs = stmt.executeQuery(
              "SELECT " +
                      "  (SELECT SUM(amount) FROM Transaction WHERE DATE(timestamp) = CURDATE()) as today_amount, " +
                      "  (SELECT SUM(amount) FROM Transaction WHERE DATE(timestamp) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)) as yesterday_amount, " +
                      "  IFNULL(ROUND(((SELECT SUM(amount) FROM Transaction WHERE DATE(timestamp) = CURDATE()) / " +
                      "         NULLIF((SELECT SUM(amount) FROM Transaction WHERE DATE(timestamp) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)), 0) - 1) * 100, 2), 0) as percent_change")) {
        if (rs.next()) {
          double todayAmount = rs.getDouble("today_amount");
          double percentChange = rs.getDouble("percent_change");
          String trend = percentChange >= 0 ? "▲" : "▼";
          String color = percentChange >= 0 ? "green" : "red";

          SwingUtilities.invokeLater(() ->
                  todayValueLabel.setText(String.format("<html>%s <span style='color:%s'>%s %.2f%%</span></html>",
                          currencyFormat.format(todayAmount), color, trend, Math.abs(percentChange))));
        }
      }

      // Success rate
      try (ResultSet rs = stmt.executeQuery(
              "SELECT " +
                      "ROUND((COUNT(CASE WHEN status = 'Approved' THEN 1 END) / COUNT(*)) * 100, 2) as approval_rate " +
                      "FROM Transaction")) {
        if (rs.next()) {
          double rate = rs.getDouble("approval_rate");
          SwingUtilities.invokeLater(() ->
                  successRateLabel.setText(String.format("%.2f%%", rate)));
        }
      }
    }
  }

  /**
   * Update card type distribution table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateCardTypeDistribution() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      ResultSet rs = stmt.executeQuery(
              "SELECT " +
                      "    c.card_type, " +
                      "    COUNT(t.transaction_id) as transaction_count, " +
                      "    SUM(t.amount) as total_amount, " +
                      "    ROUND((COUNT(t.transaction_id) / (SELECT COUNT(*) FROM Transaction)) * 100, 2) as percent_of_total " +
                      "FROM Transaction t " +
                      "JOIN Card c ON t.card_id = c.card_id " +
                      "GROUP BY c.card_type " +
                      "ORDER BY transaction_count DESC");

      // Clear existing data
      SwingUtilities.invokeLater(() -> cardTypeTableModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        String cardType = rs.getString("card_type");
        int count = rs.getInt("transaction_count");
        double amount = rs.getDouble("total_amount");
        double percent = rs.getDouble("percent_of_total");

        SwingUtilities.invokeLater(() ->
                cardTypeTableModel.addRow(new Object[] {
                        cardType,
                        count,
                        currencyFormat.format(amount),
                        String.format("%.2f%%", percent)
                })
        );
      }
    }
  }

  /**
   * Update top merchants table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateTopMerchants() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      ResultSet rs = stmt.executeQuery(
              "SELECT " +
                      "    pm.merchant_name, " +
                      "    pm.merchant_category, " +
                      "    COUNT(t.transaction_id) as transaction_count, " +
                      "    SUM(t.amount) as total_amount " +
                      "FROM Transaction t " +
                      "JOIN PaymentMerchant pm ON t.merchant_id = pm.merchant_id " +
                      "GROUP BY pm.merchant_id, pm.merchant_name, pm.merchant_category " +
                      "ORDER BY transaction_count DESC " +
                      "LIMIT 10");

      // Clear existing data
      SwingUtilities.invokeLater(() -> merchantsTableModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        String name = rs.getString("merchant_name");
        String category = rs.getString("merchant_category");
        int count = rs.getInt("transaction_count");
        double amount = rs.getDouble("total_amount");

        SwingUtilities.invokeLater(() ->
                merchantsTableModel.addRow(new Object[] {
                        name,
                        category,
                        count,
                        currencyFormat.format(amount)
                })
        );
      }
    }
  }
}