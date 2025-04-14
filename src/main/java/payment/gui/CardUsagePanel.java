package payment.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartPanel;

import payment.database.DatabaseManager;
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
public class CardUsagePanel extends AbstractAnalysisPanel {
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
        super(dbManager);
        if (dbManager == null) {
            throw new IllegalArgumentException("Database manager cannot be null");
        }

        this.dbManager = dbManager;
        this.connection = dbManager.getConnection();

        initTableModels();
        initComponents();
        refreshData();
        setupRefreshTimer();
    }

    @Override
    public void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel summaryPanel = createSummaryPanel();
        JPanel analysisPanel = createAnalysisPanel();
        
        mainPanel.add(summaryPanel, BorderLayout.NORTH);
        mainPanel.add(analysisPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public void initTableModels() {
        String[] distributionColumns = {"Card Type", "Count", "Active Cards", "Transactions", "Success Rate"};
        cardDistributionModel = new DefaultTableModel(distributionColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cardDistributionTable = new JTable(cardDistributionModel);

        String[] transactionColumns = {"Timestamp", "Card Type", "Amount", "Status", "Merchant"};
        cardTransactionModel = new DefaultTableModel(transactionColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cardTransactionTable = new JTable(cardTransactionModel);
    }

    @Override
    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    loadCardDistributionData();
                    loadCardTransactions();
                    updateSummaryLabels();
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

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        totalCardsLabel = new JLabel("Total Cards: ");
        activeCardsLabel = new JLabel("Active Cards: ");
        totalTransactionsLabel = new JLabel("Total Transactions: ");
        successRateLabel = new JLabel("Success Rate: ");

        panel.add(totalCardsLabel);
        panel.add(activeCardsLabel);
        panel.add(totalTransactionsLabel);
        panel.add(successRateLabel);

        return panel;
    }

    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane distributionScroll = new JScrollPane(cardDistributionTable);
        JScrollPane transactionsScroll = new JScrollPane(cardTransactionTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(distributionScroll);
        splitPane.setBottomComponent(transactionsScroll);
        splitPane.setResizeWeight(0.4);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadCardDistributionData() throws SQLException {
        cardDistributionModel.setRowCount(0);
        String query = "SELECT " +
            "c.card_type, " +
            "COUNT(*) as total_cards, " +
            "SUM(CASE WHEN c.is_active = 1 THEN 1 ELSE 0 END) as active_cards, " +
            "COUNT(t.transaction_id) as transactions, " +
            "ROUND((COUNT(CASE WHEN t.status = 'SUCCESS' THEN 1 END) / COUNT(t.transaction_id)) * 100, 2) as success_rate " +
            "FROM Card c " +
            "LEFT JOIN Transaction t ON c.card_id = t.card_id " +
            "GROUP BY c.card_type " +
            "ORDER BY transactions DESC";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            cardDistributionModel.addRow(new Object[]{
                rs.getString("card_type"),
                rs.getInt("total_cards"),
                rs.getInt("active_cards"),
                rs.getInt("transactions"),
                String.format("%.2f%%", rs.getDouble("success_rate"))
            });
        }
    }

    private void loadCardTransactions() throws SQLException {
        cardTransactionModel.setRowCount(0);
        String query = "SELECT " +
            "t.timestamp, " +
            "c.card_type, " +
            "t.amount, " +
            "t.status, " +
            "m.merchant_name " +
            "FROM Transaction t " +
            "JOIN Card c ON t.card_id = c.card_id " +
            "JOIN Merchant m ON t.merchant_id = m.merchant_id " +
            "ORDER BY t.timestamp DESC " +
            "LIMIT 100";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            Object amountObj = rs.getObject("amount");
            double amount = 0.0;
            if (amountObj instanceof BigDecimal) {
                amount = ((BigDecimal) amountObj).doubleValue();
            }

            cardTransactionModel.addRow(new Object[]{
                rs.getTimestamp("timestamp"),
                rs.getString("card_type"),
                currencyFormat.format(amount),
                rs.getString("status"),
                rs.getString("merchant_name")
            });
        }
    }

    private void updateSummaryLabels() throws SQLException {
        String query = "SELECT " +
            "COUNT(*) as total_cards, " +
            "SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) as active_cards, " +
            "COUNT(t.transaction_id) as total_transactions, " +
            "ROUND((COUNT(CASE WHEN t.status = 'SUCCESS' THEN 1 END) / COUNT(t.transaction_id)) * 100, 2) as success_rate " +
            "FROM Card c " +
            "LEFT JOIN Transaction t ON c.card_id = t.card_id";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        if (rs.next()) {
            totalCardsLabel.setText("Total Cards: " + rs.getInt("total_cards"));
            activeCardsLabel.setText("Active Cards: " + rs.getInt("active_cards"));
            totalTransactionsLabel.setText("Total Transactions: " + rs.getInt("total_transactions"));
            successRateLabel.setText("Success Rate: " + String.format("%.2f%%", rs.getDouble("success_rate")));
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