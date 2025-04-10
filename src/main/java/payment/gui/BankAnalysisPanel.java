package payment.gui;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * Panel for analyzing bank performance and transaction data in the payment network.
 * <p>
 * This panel provides comprehensive analytics and visualizations for bank performance
 * across the Visa payment network. It offers detailed metrics on both issuing and
 * acquiring banks, transaction history, success rates, and financial performance.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Bank performance metrics with total transaction volume and value</li>
 *   <li>Success rate analysis and trends over time</li>
 *   <li>Transaction history details with filtering by bank</li>
 *   <li>Financial metrics including average transaction amount</li>
 *   <li>Merchant relationship analysis</li>
 *   <li>Automatic data refresh to ensure current information</li>
 * </ul>
 * </p>
 * <p>
 * The panel uses a split-pane layout with bank selection and details at the top,
 * and transaction history at the bottom. It implements robust type conversion
 * for database values to ensure correct display of numerical data.
 * </p>
 */
public class BankAnalysisPanel extends JPanel {
    /**
     * Database manager instance for database connectivity
     */
    private final DatabaseManager dbManager;

    /**
     * Database connection for executing SQL queries
     */
    private final Connection connection;

    /**
     * Currently selected bank ID for detailed analysis
     */
    private int selectedBankId = -1;

    // UI Components
    /**
     * Table displaying bank performance data
     */
    private JTable bankTable;

    /**
     * Table displaying transaction history for selected bank
     */
    private JTable transactionTable;

    /**
     * Label showing the selected bank's name
     */
    private JLabel bankNameLabel;

    /**
     * Label showing the selected bank's code
     */
    private JLabel bankCodeLabel;

    /**
     * Label showing total transactions for the selected bank
     */
    private JLabel totalTransactionsLabel;

    /**
     * Label showing total transaction amount for the selected bank
     */
    private JLabel totalAmountLabel;

    /**
     * Label showing average transaction amount for the selected bank
     */
    private JLabel avgAmountLabel;

    /**
     * Label showing transaction success rate for the selected bank
     */
    private JLabel successRateLabel;

    /**
     * Label showing count of unique merchants for the selected bank
     */
    private JLabel uniqueMerchantsLabel;

    // Table Models
    /**
     * Model for issuing bank table
     */
    private DefaultTableModel issuingBankModel;

    /**
     * Model for acquiring bank table
     */
    private DefaultTableModel acquiringBankModel;

    /**
     * Model for bank performance metrics
     */
    private DefaultTableModel bankPerformanceModel;

    /**
     * Model for transaction history table
     */
    private DefaultTableModel transactionModel;

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
     * Constructor for the bank analysis panel.
     * <p>
     * Initializes the panel with database connection, UI components,
     * table models, and sets up the initial data load and auto-refresh timer.
     * </p>
     *
     * @param dbManager The database manager instance for database connectivity
     * @throws IllegalArgumentException if dbManager is null
     */
    public BankAnalysisPanel(DatabaseManager dbManager) {
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
     * Creates and lays out all UI components including the bank details panel
     * and transaction history panel. Sets up the main panel with a border layout
     * to organize these elements.
     * </p>
     */
    private void initComponents() {
        // Create main panel with card layout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create bank details panel
        JPanel bankDetailsPanel = createBankDetailsPanel();

        // Create transaction history panel
        JPanel transactionPanel = createTransactionPanel();

        // Add panels to main panel
        mainPanel.add(bankDetailsPanel, BorderLayout.CENTER);

        // Add main panel to this panel
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Initialize table models for bank and transaction data.
     * <p>
     * Creates the table models with appropriate columns and sets up
     * non-editable cells for display-only data. These models are used
     * to populate the data tables in the UI.
     * </p>
     */
    private void initTableModels() {
        // Bank table model
        String[] bankColumns = {"Bank ID", "Bank Name", "Total Transactions", "Success Rate", "Total Amount"};
        issuingBankModel = new DefaultTableModel(bankColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bankTable = new JTable(issuingBankModel);

        // Hide the Bank ID column but keep it for selection purposes
        bankTable.getColumnModel().getColumn(0).setMinWidth(0);
        bankTable.getColumnModel().getColumn(0).setMaxWidth(0);
        bankTable.getColumnModel().getColumn(0).setPreferredWidth(0);

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

    /**
     * Create the bank details panel with summary information and bank selection.
     * <p>
     * This panel includes:
     * <ul>
     *   <li>Summary statistics about the selected bank</li>
     *   <li>A table for selecting and viewing bank performance data</li>
     *   <li>A transaction history panel for the selected bank</li>
     * </ul>
     * </p>
     *
     * @return The fully configured bank details panel
     */
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
                // Get the bank ID from the hidden first column
                selectedBankId = Integer.parseInt(bankTable.getValueAt(bankTable.getSelectedRow(), 0).toString());
                loadBankDetails();
                loadTransactionHistory();
            }
        });

        // Create transaction panel
        JPanel transactionPanel = createTransactionPanel();

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(tableScroll);
        splitPane.setBottomComponent(transactionPanel);
        splitPane.setResizeWeight(0.4); // 40% top, 60% bottom

        // Add components to panel
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Create the transaction history panel for displaying bank transactions.
     * <p>
     * This panel shows detailed transaction information for the selected bank,
     * including timestamp, merchant, card type, amount, status, authorization code,
     * and response code.
     * </p>
     *
     * @return The fully configured transaction history panel
     */
    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add transaction table
        JScrollPane tableScroll = new JScrollPane(transactionTable);
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
     * Reloads bank data and transaction history from the database based on
     * the current selection state. If a bank is selected, also refreshes
     * the bank details and transaction history for that bank.
     * </p>
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
            // First clear any existing data
            if (issuingBankModel != null) {
                issuingBankModel.setRowCount(0);
            }

            // Load acquiring bank data
            String query = "SELECT " +
                    "ab.acquiring_bank_id, " +
                    "ab.bank_name, " +
                    "COUNT(t.transaction_id) as total_transactions, " +
                    "CASE WHEN COUNT(t.transaction_id) > 0 THEN " +
                    "  ROUND((COUNT(CASE WHEN t.status = 'Approved' THEN 1 END) / COUNT(t.transaction_id)) * 100, 2) " +
                    "ELSE 0 END as success_rate, " +
                    "SUM(IFNULL(t.amount, 0)) as total_amount " +
                    "FROM AcquiringBank ab " +
                    "LEFT JOIN Transaction t ON ab.acquiring_bank_id = t.acquiring_bank_id " +
                    "WHERE ab.is_active = true " +
                    "GROUP BY ab.acquiring_bank_id " +
                    "ORDER BY total_transactions DESC";

            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                // Safely convert BigDecimal to double without direct casting
                Object successRateObj = rs.getObject("success_rate");
                double successRate = 0.0;
                if (successRateObj instanceof BigDecimal) {
                    successRate = ((BigDecimal) successRateObj).doubleValue();
                } else if (successRateObj instanceof Double) {
                    successRate = (Double) successRateObj;
                } else if (successRateObj != null) {
                    successRate = Double.parseDouble(successRateObj.toString());
                }

                Object totalAmountObj = rs.getObject("total_amount");
                double totalAmount = 0.0;
                if (totalAmountObj instanceof BigDecimal) {
                    totalAmount = ((BigDecimal) totalAmountObj).doubleValue();
                } else if (totalAmountObj instanceof Double) {
                    totalAmount = (Double) totalAmountObj;
                } else if (totalAmountObj != null) {
                    totalAmount = Double.parseDouble(totalAmountObj.toString());
                }

                issuingBankModel.addRow(new Object[]{
                        rs.getInt("acquiring_bank_id"),
                        rs.getString("bank_name"),
                        rs.getInt("total_transactions"),
                        String.format("%.2f%%", successRate),
                        currencyFormat.format(totalAmount)
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading bank performance data: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Load detailed information for the selected bank.
     * <p>
     * Retrieves detailed metrics and performance data for the currently
     * selected bank and updates the UI components with this information.
     * </p>
     */
    private void loadBankDetails() {
        try {
            String query = "SELECT ab.*, " +
                    "COUNT(t.transaction_id) as total_transactions, " +
                    "SUM(IFNULL(t.amount, 0)) as total_amount, " +
                    "AVG(IFNULL(t.amount, 0)) as avg_amount, " +
                    "COUNT(DISTINCT t.merchant_id) as unique_merchants, " +
                    "CASE WHEN COUNT(t.transaction_id) > 0 THEN " +
                    "  ROUND((COUNT(CASE WHEN t.status = 'Approved' THEN 1 END) / COUNT(t.transaction_id)) * 100, 2) " +
                    "ELSE 0 END as success_rate " +
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

                // Safely convert BigDecimal to double
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
                avgAmountLabel.setText("Average Amount: " + currencyFormat.format(avgAmount));
                successRateLabel.setText("Success Rate: " + String.format("%.2f%%", successRate));
                uniqueMerchantsLabel.setText("Unique Merchants: " + rs.getInt("unique_merchants"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading bank details: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Load transaction history for the selected bank.
     * <p>
     * Retrieves transaction records from the database for the currently
     * selected bank and populates the transaction table with this data.
     * </p>
     */
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
            transactionModel.setRowCount(0);

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

                transactionModel.addRow(new Object[]{
                        rs.getTimestamp("timestamp"),
                        rs.getString("merchant_name"),
                        rs.getString("card_type"),
                        currencyFormat.format(amount),
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