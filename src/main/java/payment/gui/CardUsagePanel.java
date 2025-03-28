package main.java.payment.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import main.java.payment.database.DatabaseManager;

/**
 * Panel for analyzing card usage patterns.
 */
public class CardUsagePanel extends JPanel {
  private final DatabaseManager dbManager;
  private final Connection connection;

  // UI Components
  private JTable cardDistributionTable;
  private JTable cardTransactionTable;
  private JLabel totalCardsLabel;
  private JLabel activeCardsLabel;
  private JLabel totalTransactionsLabel;
  private JLabel totalAmountLabel;
  private JLabel avgTransactionLabel;
  private JLabel successRateLabel;

  // Table Models
  private DefaultTableModel cardDistributionModel;
  private DefaultTableModel cardTransactionModel;

  // Charts
  private ChartPanel cardTypeChart;
  private ChartPanel transactionVolumeChart;
  private ChartPanel successRateChart;

  // Formatters
  private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

  // Auto-refresh timer
  private Timer refreshTimer;
  private final int REFRESH_INTERVAL = 60000; // 1 minute

  /**
   * Constructor for the card usage panel.
   *
   * @param dbManager The database manager
   */
  public CardUsagePanel(DatabaseManager dbManager) {
    this.dbManager = dbManager;
    this.connection = dbManager.getConnection();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    // Initialize UI components
    initComponents();

    // Initialize table models
    initTableModels();

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

    // Create card usage panel
    JPanel cardUsagePanel = createCardUsagePanel();

    // Create transaction details panel
    JPanel transactionPanel = createTransactionPanel();

    // Add panels to main panel
    mainPanel.add(cardUsagePanel, "cardUsage");
    mainPanel.add(transactionPanel, "transactions");

    // Add main panel to this panel
    add(mainPanel, BorderLayout.CENTER);
  }

  private void initTableModels() {
    // Card distribution table model
    String[] distributionColumns = {
            "Card Type", "Total Cards", "Active Cards",
            "Total Transactions", "Total Amount", "Success Rate"
    };
    cardDistributionModel = new DefaultTableModel(distributionColumns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    cardDistributionTable = new JTable(cardDistributionModel);

    // Card transaction table model
    String[] transactionColumns = {
            "Timestamp", "Cardholder", "Amount", "Status",
            "Merchant", "Auth Code", "Response"
    };
    cardTransactionModel = new DefaultTableModel(transactionColumns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    cardTransactionTable = new JTable(cardTransactionModel);
  }

  /**
   * Create the card usage panel.
   *
   * @return The card usage panel
   */
  private JPanel createCardUsagePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create summary labels
    totalCardsLabel = new JLabel("Total Cards: ");
    activeCardsLabel = new JLabel("Active Cards: ");
    totalTransactionsLabel = new JLabel("Total Transactions: ");
    totalAmountLabel = new JLabel("Total Amount: ");
    avgTransactionLabel = new JLabel("Average Transaction: ");
    successRateLabel = new JLabel("Success Rate: ");

    // Create info panel
    JPanel infoPanel = new JPanel(new GridLayout(6, 1, 5, 5));
    infoPanel.add(totalCardsLabel);
    infoPanel.add(activeCardsLabel);
    infoPanel.add(totalTransactionsLabel);
    infoPanel.add(totalAmountLabel);
    infoPanel.add(avgTransactionLabel);
    infoPanel.add(successRateLabel);

    // Create charts panel
    JPanel chartsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
    cardTypeChart = new ChartPanel(null);
    transactionVolumeChart = new ChartPanel(null);
    successRateChart = new ChartPanel(null);
    chartsPanel.add(cardTypeChart);
    chartsPanel.add(transactionVolumeChart);
    chartsPanel.add(successRateChart);

    // Add card distribution table
    JScrollPane tableScroll = new JScrollPane(cardDistributionTable);
    cardDistributionTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && cardDistributionTable.getSelectedRow() != -1) {
        String cardType = (String) cardDistributionTable.getValueAt(
                cardDistributionTable.getSelectedRow(), 0);
        loadCardTransactions(cardType);
      }
    });

    // Add components to panel
    panel.add(infoPanel, BorderLayout.NORTH);
    panel.add(chartsPanel, BorderLayout.CENTER);
    panel.add(tableScroll, BorderLayout.SOUTH);

    return panel;
  }

  /**
   * Create the transaction details panel.
   *
   * @return The transaction details panel
   */
  private JPanel createTransactionPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Add transaction table
    JScrollPane tableScroll = new JScrollPane(cardTransactionTable);
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
          loadCardUsageData();
          updateCharts();
        } catch (Exception e) {
          e.printStackTrace();
          SwingUtilities.invokeLater(() ->
                  JOptionPane.showMessageDialog(
                          CardUsagePanel.this,
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

  /**
   * Load card usage data from the database.
   */
  private void loadCardUsageData() {
    try {
      String query = "SELECT " +
              "c.card_type, " +
              "COUNT(DISTINCT c.card_id) as total_cards, " +
              "COUNT(DISTINCT CASE WHEN c.is_active = true THEN c.card_id END) as active_cards, " +
              "COUNT(t.transaction_id) as total_transactions, " +
              "SUM(t.amount) as total_amount, " +
              "ROUND((COUNT(CASE WHEN t.status = 'SUCCESS' THEN 1 END) / COUNT(*)) * 100, 2) as success_rate " +
              "FROM Card c " +
              "LEFT JOIN Transaction t ON c.card_id = t.card_id " +
              "GROUP BY c.card_type";

      Connection conn = DatabaseManager.getConnection();
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);

      cardDistributionModel.setRowCount(0);
      while (rs.next()) {
        cardDistributionModel.addRow(new Object[]{
                rs.getString("card_type"),
                rs.getInt("total_cards"),
                rs.getInt("active_cards"),
                rs.getInt("total_transactions"),
                currencyFormat.format(rs.getDouble("total_amount")),
                String.format("%.2f%%", rs.getDouble("success_rate"))
        });
      }

      // Update summary labels
      updateSummaryLabels();

    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this,
              "Error loading card usage data: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Update summary labels.
   */
  private void updateSummaryLabels() {
    try {
      String query = "SELECT " +
              "COUNT(DISTINCT c.card_id) as total_cards, " +
              "COUNT(DISTINCT CASE WHEN c.is_active = true THEN c.card_id END) as active_cards, " +
              "COUNT(t.transaction_id) as total_transactions, " +
              "SUM(t.amount) as total_amount, " +
              "AVG(t.amount) as avg_amount, " +
              "ROUND((COUNT(CASE WHEN t.status = 'SUCCESS' THEN 1 END) / COUNT(*)) * 100, 2) as success_rate " +
              "FROM Card c " +
              "LEFT JOIN Transaction t ON c.card_id = t.card_id";

      Connection conn = DatabaseManager.getConnection();
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);

      if (rs.next()) {
        totalCardsLabel.setText("Total Cards: " + rs.getInt("total_cards"));
        activeCardsLabel.setText("Active Cards: " + rs.getInt("active_cards"));
        totalTransactionsLabel.setText("Total Transactions: " + rs.getInt("total_transactions"));
        totalAmountLabel.setText("Total Amount: " + currencyFormat.format(rs.getDouble("total_amount")));
        avgTransactionLabel.setText("Average Transaction: " + currencyFormat.format(rs.getDouble("avg_amount")));
        successRateLabel.setText("Success Rate: " + String.format("%.2f%%", rs.getDouble("success_rate")));
      }

    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this,
              "Error updating summary labels: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Load card transactions from the database.
   *
   * @param cardType The card type
   */
  private void loadCardTransactions(String cardType) {
    try {
      String query = "SELECT " +
              "t.timestamp, " +
              "CONCAT(ch.first_name, ' ', ch.last_name) as cardholder, " +
              "t.amount, " +
              "t.status, " +
              "pm.merchant_name, " +
              "a.auth_code, " +
              "ar.response_message " +
              "FROM Transaction t " +
              "JOIN Card c ON t.card_id = c.card_id " +
              "JOIN Cardholder ch ON c.cardholder_id = ch.cardholder_id " +
              "JOIN PaymentMerchant pm ON t.merchant_id = pm.merchant_id " +
              "LEFT JOIN Authorization a ON t.transaction_id = a.transaction_id " +
              "LEFT JOIN AuthResponse ar ON a.auth_id = ar.auth_id " +
              "WHERE c.card_type = ? " +
              "ORDER BY t.timestamp DESC " +
              "LIMIT 100";

      Connection conn = DatabaseManager.getConnection();
      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setString(1, cardType);

      ResultSet rs = stmt.executeQuery();
      cardTransactionModel.setRowCount(0);

      while (rs.next()) {
        cardTransactionModel.addRow(new Object[]{
                rs.getTimestamp("timestamp"),
                rs.getString("cardholder"),
                currencyFormat.format(rs.getDouble("amount")),
                rs.getString("status"),
                rs.getString("merchant_name"),
                rs.getString("auth_code"),
                rs.getString("response_message")
        });
      }

    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this,
              "Error loading card transactions: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Update charts.
   */
  private void updateCharts() {
    try {
      // Card Type Distribution Chart
      DefaultPieDataset cardTypeDataset = new DefaultPieDataset();
      String query = "SELECT card_type, COUNT(*) as count FROM Card GROUP BY card_type";
      Connection conn = DatabaseManager.getConnection();
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      while (rs.next()) {
        cardTypeDataset.setValue(
                rs.getString("card_type"),
                rs.getInt("count")
        );
      }
      JFreeChart cardTypeChart = ChartFactory.createPieChart(
              "Card Type Distribution",
              cardTypeDataset,
              true, true, false
      );
      this.cardTypeChart.setChart(cardTypeChart);

      // Transaction Volume Chart
      DefaultCategoryDataset volumeDataset = new DefaultCategoryDataset();
      query = "SELECT DATE(timestamp) as date, COUNT(*) as count " +
              "FROM Transaction " +
              "WHERE timestamp >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
              "GROUP BY DATE(timestamp) " +
              "ORDER BY date";
      rs = stmt.executeQuery(query);
      while (rs.next()) {
        volumeDataset.addValue(
                rs.getInt("count"),
                "Transactions",
                rs.getDate("date")
        );
      }
      JFreeChart volumeChart = ChartFactory.createLineChart(
              "Transaction Volume (Last 7 Days)",
              "Date",
              "Number of Transactions",
              volumeDataset,
              PlotOrientation.VERTICAL,
              true, true, false
      );
      this.transactionVolumeChart.setChart(volumeChart);

      // Success Rate Chart
      DefaultCategoryDataset successDataset = new DefaultCategoryDataset();
      query = "SELECT card_type, " +
              "ROUND((COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END) / COUNT(*)) * 100, 2) as success_rate " +
              "FROM Transaction t " +
              "JOIN Card c ON t.card_id = c.card_id " +
              "GROUP BY card_type";
      rs = stmt.executeQuery(query);
      while (rs.next()) {
        successDataset.addValue(
                rs.getDouble("success_rate"),
                "Success Rate",
                rs.getString("card_type")
        );
      }
      JFreeChart successChart = ChartFactory.createBarChart(
              "Success Rate by Card Type",
              "Card Type",
              "Success Rate (%)",
              successDataset,
              PlotOrientation.VERTICAL,
              false, true, false
      );
      this.successRateChart.setChart(successChart);

    } catch (SQLException e) {
      JOptionPane.showMessageDialog(this,
              "Error updating charts: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }
}