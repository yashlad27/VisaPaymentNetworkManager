package payment.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.math.BigDecimal;
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

import payment.database.DatabaseManager;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.CallableStatement;
import java.sql.Types;

import static payment.database.QueryManager.resultSetToList;

/**
 * Panel for analyzing card usage patterns and transaction details.
 * <p>
 * This panel provides comprehensive visualizations and analytics for card usage patterns
 * across the Visa payment network. It offers interactive charts, detailed data tables,
 * and summary statistics to help users understand card distribution, transaction volumes,
 * and success rates across different card types.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Card type distribution analysis with pie charts</li>
 *   <li>Transaction volume trends with time-series charts</li>
 *   <li>Success rate analysis across different card types</li>
 *   <li>Detailed transaction records for each card type</li>
 *   <li>Summary statistics for card usage and performance</li>
 *   <li>Automatic data refresh to ensure current information</li>
 * </ul>
 * </p>
 * <p>
 * The panel uses JFreeChart for visualization and implements robust type conversion
 * for database values to ensure correct rendering of numerical data.
 * </p>
 */
public class CardUsagePanel extends JPanel {
    /**
     * Database manager instance for database connectivity
     */
    private final DatabaseManager dbManager;

    /**
     * Database connection for executing SQL queries
     */
    private final Connection connection;

    // UI Components
    /**
     * Table displaying card distribution data
     */
    private JTable cardDistributionTable;

    /**
     * Table displaying card transaction details
     */
    private JTable cardTransactionTable;

    /**
     * Label showing total number of cards
     */
    private JLabel totalCardsLabel;

    /**
     * Label showing number of active cards
     */
    private JLabel activeCardsLabel;

    /**
     * Label showing total number of transactions
     */
    private JLabel totalTransactionsLabel;

    /**
     * Label showing total transaction amount
     */
    private JLabel totalAmountLabel;

    /**
     * Label showing average transaction amount
     */
    private JLabel avgTransactionLabel;

    /**
     * Label showing overall transaction success rate
     */
    private JLabel successRateLabel;

    // Table Models
    /**
     * Model for card distribution table
     */
    private DefaultTableModel cardDistributionModel;

    /**
     * Model for card transaction table
     */
    private DefaultTableModel cardTransactionModel;

    // Charts
    /**
     * Chart panel for card type distribution pie chart
     */
    private ChartPanel cardTypeChart;

    /**
     * Chart panel for transaction volume trends
     */
    private ChartPanel transactionVolumeChart;

    /**
     * Chart panel for success rate comparison
     */
    private ChartPanel successRateChart;

    // Formatters
    /**
     * Formatter for currency values
     */
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    // Auto-refresh timer
    /**
     * Timer for automatic data refresh
     */
    private Timer refreshTimer;

    /**
     * Interval for automatic data refresh (in milliseconds)
     */
    private final int REFRESH_INTERVAL = 60000; // 1 minute

    /**
     * Constructor for the card usage panel.
     * <p>
     * Initializes the panel with database connection, UI components,
     * table models, and sets up the initial data load and auto-refresh timer.
     * </p>
     *
     * @param dbManager The database manager instance for database connectivity
     * @throws IllegalArgumentException if dbManager is null
     */
    public CardUsagePanel(DatabaseManager dbManager) {
        if (dbManager == null) {
            throw new IllegalArgumentException("Database manager cannot be null");
        }

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
     * <p>
     * Creates and lays out all UI components including the card usage panel
     * and transaction details panel. Sets up the main panel with a card layout
     * to switch between different views.
     * </p>
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

    /**
     * Initialize table models for card distribution and transaction data.
     * <p>
     * Creates the table models with appropriate columns and sets up
     * non-editable cells for display-only data. These models are used
     * to populate the data tables in the UI.
     * </p>
     */
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
     * Create the card usage panel with summary information and charts.
     * <p>
     * This panel includes:
     * <ul>
     *   <li>Summary statistics about card usage</li>
     *   <li>Charts showing card type distribution, transaction volume, and success rates</li>
     *   <li>A table with detailed card distribution data</li>
     * </ul>
     * </p>
     *
     * @return The fully configured card usage panel
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
     * Create the transaction details panel for displaying card transactions.
     * <p>
     * This panel shows detailed transaction information for the selected card type,
     * including timestamp, cardholder, amount, status, merchant, authorization code,
     * and response code.
     * </p>
     *
     * @return The fully configured transaction details panel
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
     * Set up the automatic data refresh timer.
     * <p>
     * Initializes a timer that refreshes data at regular intervals to ensure
     * that the displayed information remains current.
     * </p>
     */
    private void setupRefreshTimer() {
        refreshTimer = new Timer(REFRESH_INTERVAL, e -> refreshData());
        refreshTimer.start();
    }

    /**
     * Refresh all data from the database.
     * <p>
     * Uses SwingWorker to perform database operations in a background thread
     * to prevent UI freezing. Updates all charts, tables, and summary statistics
     * with fresh data from the database.
     * </p>
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
     * Load transaction data for a specific card type.
     * <p>
     * Retrieves transaction details from the database for the specified card type
     * and populates the transaction table with this data.
     * </p>
     *
     * @param cardType The type of card to load transactions for
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
                // Safely handle BigDecimal conversion for amount
                Object amountObj = rs.getObject("amount");
                double amount = 0.0;
                if (amountObj instanceof BigDecimal) {
                    amount = ((BigDecimal) amountObj).doubleValue();
                } else if (amountObj instanceof Double) {
                    amount = (Double) amountObj;
                } else if (amountObj != null) {
                    amount = Double.parseDouble(amountObj.toString());
                }

                cardTransactionModel.addRow(new Object[]{
                        rs.getTimestamp("timestamp"),
                        rs.getString("cardholder"),
                        currencyFormat.format(amount),
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
     * Load card usage data from the database.
     */
    private void loadCardUsageData() {
        try {
            String query = "SELECT " +
                    "c.card_type, " +
                    "COUNT(DISTINCT c.card_id) as total_cards, " +
                    "COUNT(DISTINCT CASE WHEN c.is_active = true THEN c.card_id END) as active_cards, " +
                    "COUNT(t.transaction_id) as total_transactions, " +
                    "SUM(IFNULL(t.amount, 0)) as total_amount, " +
                    "CASE WHEN COUNT(t.transaction_id) > 0 THEN " +
                    "  ROUND((COUNT(CASE WHEN t.status = 'Approved' THEN 1 END) / COUNT(t.transaction_id)) * 100, 2) " +
                    "ELSE 0 END as success_rate " +
                    "FROM Card c " +
                    "LEFT JOIN Transaction t ON c.card_id = t.card_id " +
                    "GROUP BY c.card_type";

            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            cardDistributionModel.setRowCount(0);
            while (rs.next()) {
                // Safely handle BigDecimal conversion
                Object totalAmountObj = rs.getObject("total_amount");
                double totalAmount = 0.0;
                if (totalAmountObj instanceof BigDecimal) {
                    totalAmount = ((BigDecimal) totalAmountObj).doubleValue();
                } else if (totalAmountObj instanceof Double) {
                    totalAmount = (Double) totalAmountObj;
                } else if (totalAmountObj != null) {
                    totalAmount = Double.parseDouble(totalAmountObj.toString());
                }

                Object successRateObj = rs.getObject("success_rate");
                double successRate = 0.0;
                if (successRateObj instanceof BigDecimal) {
                    successRate = ((BigDecimal) successRateObj).doubleValue();
                } else if (successRateObj instanceof Double) {
                    successRate = (Double) successRateObj;
                } else if (successRateObj != null) {
                    successRate = Double.parseDouble(successRateObj.toString());
                }

                cardDistributionModel.addRow(new Object[]{
                        rs.getString("card_type"),
                        rs.getInt("total_cards"),
                        rs.getInt("active_cards"),
                        rs.getInt("total_transactions"),
                        currencyFormat.format(totalAmount),
                        String.format("%.2f%%", successRate)
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
                    "SUM(IFNULL(t.amount, 0)) as total_amount, " +
                    "AVG(IFNULL(t.amount, 0)) as avg_amount, " +
                    "CASE WHEN COUNT(t.transaction_id) > 0 THEN " +
                    "  ROUND((COUNT(CASE WHEN t.status = 'Approved' THEN 1 END) / COUNT(t.transaction_id)) * 100, 2) " +
                    "ELSE 0 END as success_rate " +
                    "FROM Card c " +
                    "LEFT JOIN Transaction t ON c.card_id = t.card_id";

            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                totalCardsLabel.setText("Total Cards: " + rs.getInt("total_cards"));
                activeCardsLabel.setText("Active Cards: " + rs.getInt("active_cards"));
                totalTransactionsLabel.setText("Total Transactions: " + rs.getInt("total_transactions"));

                // Safely handle BigDecimal conversion
                Object totalAmountObj = rs.getObject("total_amount");
                double totalAmount = 0.0;
                if (totalAmountObj instanceof BigDecimal) {
                    totalAmount = ((BigDecimal) totalAmountObj).doubleValue();
                } else if (totalAmountObj instanceof Double) {
                    totalAmount = (Double) totalAmountObj;
                } else if (totalAmountObj != null) {
                    totalAmount = Double.parseDouble(totalAmountObj.toString());
                }

                Object avgAmountObj = rs.getObject("avg_amount");
                double avgAmount = 0.0;
                if (avgAmountObj instanceof BigDecimal) {
                    avgAmount = ((BigDecimal) avgAmountObj).doubleValue();
                } else if (avgAmountObj instanceof Double) {
                    avgAmount = (Double) avgAmountObj;
                } else if (avgAmountObj != null) {
                    avgAmount = Double.parseDouble(avgAmountObj.toString());
                }

                Object successRateObj = rs.getObject("success_rate");
                double successRate = 0.0;
                if (successRateObj instanceof BigDecimal) {
                    successRate = ((BigDecimal) successRateObj).doubleValue();
                } else if (successRateObj instanceof Double) {
                    successRate = (Double) successRateObj;
                } else if (successRateObj != null) {
                    successRate = Double.parseDouble(successRateObj.toString());
                }

                totalAmountLabel.setText("Total Amount: " + currencyFormat.format(totalAmount));
                avgTransactionLabel.setText("Average Transaction: " + currencyFormat.format(avgAmount));
                successRateLabel.setText("Success Rate: " + String.format("%.2f%%", successRate));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error updating summary labels: " + e.getMessage(),
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
                    "CASE WHEN COUNT(*) > 0 THEN " +
                    "  ROUND((COUNT(CASE WHEN status = 'Approved' THEN 1 END) / COUNT(*)) * 100, 2) " +
                    "ELSE 0 END as success_rate " +
                    "FROM Transaction t " +
                    "JOIN Card c ON t.card_id = c.card_id " +
                    "GROUP BY card_type";
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                // Safely handle BigDecimal conversion
                Object rateObj = rs.getObject("success_rate");
                double rate = 0.0;
                if (rateObj instanceof BigDecimal) {
                    rate = ((BigDecimal) rateObj).doubleValue();
                } else if (rateObj instanceof Double) {
                    rate = (Double) rateObj;
                } else if (rateObj != null) {
                    rate = Double.parseDouble(rateObj.toString());
                }

                successDataset.addValue(
                        rate,
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

    // Example: Using the GetCardTransactionHistory stored procedure
    public List<Map<String, Object>> getCardTransactionHistory(int cardId) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL GetCardTransactionHistory(?)}")) {

            stmt.setInt(1, cardId);
            ResultSet rs = stmt.executeQuery();

            results = resultSetToList(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    // Example: Using the ProcessTransaction stored procedure
    public Map<String, Object> processTransaction(BigDecimal amount, String currency,
                                                  int cardId, int merchantId, int bankId) {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL ProcessTransaction(?, ?, ?, ?, ?, ?, ?)}")) {

            stmt.setBigDecimal(1, amount);
            stmt.setString(2, currency);
            stmt.setInt(3, cardId);
            stmt.setInt(4, merchantId);
            stmt.setInt(5, bankId);
            stmt.registerOutParameter(6, Types.INTEGER);
            stmt.registerOutParameter(7, Types.VARCHAR);

            stmt.execute();

            result.put("transaction_id", stmt.getInt(6));
            result.put("status", stmt.getString(7));
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
        }
        return result;
    }
}