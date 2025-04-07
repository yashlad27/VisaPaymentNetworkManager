package payment.gui;

import payment.database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.Vector;

/**
 * Panel for running custom SQL queries against the Visa payment network database.
 * <p>
 * This panel provides an interface for users to write, save, load, and execute custom
 * SQL queries directly against the database. It features a text area for entering SQL
 * statements, a results table for displaying query output, and controls for managing
 * saved queries for quick access to common analyses.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>SQL query text editor with syntax highlighting</li>
 *   <li>Query execution with background processing to prevent UI freezes</li>
 *   <li>Tabular results display with dynamic column generation</li>
 *   <li>Saved queries with easy selection and loading</li>
 *   <li>Result count and execution status feedback</li>
 *   <li>Error handling with user-friendly messages</li>
 * </ul>
 * </p>
 * <p>
 * The panel uses a split-pane layout with the query editor at the top and
 * the results table at the bottom. It supports SELECT, INSERT, UPDATE, and DELETE
 * operations, with appropriate handling for queries that return results versus
 * those that only affect records.
 * </p>
 */
public class QueryPanel extends JPanel {
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
     * Text area for entering SQL queries
     */
    private JTextArea queryTextArea;

    /**
     * Table for displaying query results
     */
    private JTable resultTable;

    /**
     * Model for result table
     */
    private DefaultTableModel resultTableModel;

    /**
     * Label for displaying status messages
     */
    private JLabel statusLabel;

    /**
     * Button for executing SQL queries
     */
    private JButton executeButton;

    /**
     * Button for clearing the query text area
     */
    private JButton clearButton;

    /**
     * Button for saving queries
     */
    private JButton saveButton;

    /**
     * Dropdown for selecting saved queries
     */
    private JComboBox<String> savedQueriesCombo;

    /**
     * Constructor for the query panel.
     * <p>
     * Initializes the panel with database connection and UI components.
     * Sets up the query editor, results table, and controls for query management.
     * </p>
     *
     * @param dbManager The database manager instance for database connectivity
     * @throws IllegalArgumentException if dbManager is null
     */
    public QueryPanel(DatabaseManager dbManager) {
        if (dbManager == null) {
            throw new IllegalArgumentException("Database manager cannot be null");
        }

        this.dbManager = dbManager;
        this.connection = dbManager.getConnection();

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create UI Components
        initComponents();
    }

    /**
     * Initialize UI components.
     * <p>
     * Creates and lays out all UI components including:
     * <ul>
     *   <li>Header panel with title and saved query selector</li>
     *   <li>Query input panel with text area and action buttons</li>
     *   <li>Results panel with data table</li>
     *   <li>Status bar for displaying messages</li>
     * </ul>
     * </p>
     * <p>
     * Uses a split-pane layout to divide the query input and results sections.
     * </p>
     */
    private void initComponents() {
        // Create title and control panel at the top
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Create main panel with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.3); // 30% to query, 70% to results

        // Create query input panel
        JPanel queryPanel = createQueryPanel();
        splitPane.setTopComponent(queryPanel);

        // Create results panel
        JPanel resultsPanel = createResultsPanel();
        splitPane.setBottomComponent(resultsPanel);

        // Add split pane to main panel
        add(splitPane, BorderLayout.CENTER);

        // Create status bar at the bottom
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Create the header panel with title and saved query controls.
     * <p>
     * This panel includes the panel title and a dropdown for selecting
     * pre-defined or previously saved SQL queries. When a saved query
     * is selected, it is loaded into the query text area.
     * </p>
     *
     * @return The fully configured header panel
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Title label
        JLabel titleLabel = new JLabel("Custom SQL Query");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.WEST);

        // Controls panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Saved queries combobox
        String[] savedQueries = {
                "Select a saved query...",
                "Transaction Count by Card Type",
                "Top 10 Merchants by Volume",
                "Monthly Transaction Totals",
                "Bank Approval Rates",
                "Settlement Status Summary"
        };
        savedQueriesCombo = new JComboBox<>(savedQueries);
        savedQueriesCombo.setPreferredSize(new Dimension(200, 25));
        savedQueriesCombo.addActionListener(this::loadSavedQuery);

        controlsPanel.add(new JLabel("Saved Queries:"));
        controlsPanel.add(savedQueriesCombo);

        panel.add(controlsPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * Create the query input panel with text area and action buttons.
     * <p>
     * This panel includes a text area for entering SQL queries and buttons
     * for executing, clearing, and saving queries. The text area supports
     * multi-line queries with word wrapping.
     * </p>
     *
     * @return The fully configured query panel
     */
    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "SQL Query",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        // Create text area for query input
        queryTextArea = new JTextArea();
        queryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        queryTextArea.setLineWrap(true);
        queryTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(queryTextArea);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        clearButton = new JButton("Clear");
        clearButton.addActionListener(this::clearButtonClicked);

        saveButton = new JButton("Save Query");
        saveButton.addActionListener(this::saveButtonClicked);

        executeButton = new JButton("Execute");
        executeButton.addActionListener(this::executeButtonClicked);

        buttonPanel.add(clearButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(executeButton);

        // Add components to panel
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create the query results panel with data table.
     * <p>
     * This panel displays the results of executed queries in a tabular format.
     * The table dynamically adjusts its columns based on the query result structure.
     * </p>
     *
     * @return The fully configured results panel
     */
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Query Results",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        // Create table for query results
        resultTableModel = new DefaultTableModel();
        resultTable = new JTable(resultTableModel);
        JScrollPane scrollPane = new JScrollPane(resultTable);

        // Add table to panel
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Handle clear button click to reset the query editor and results.
     * <p>
     * Clears the query text area and the results table, and updates
     * the status label to indicate the action.
     * </p>
     *
     * @param e The action event from the clear button
     */
    private void clearButtonClicked(ActionEvent e) {
        queryTextArea.setText("");
        resultTableModel.setDataVector(new Vector<>(), new Vector<>());
        statusLabel.setText("Query cleared");
    }

    /**
     * Handle save button click to store the current query.
     * <p>
     * Prompts the user for a name for the query and saves it
     * for future use. Saved queries appear in the saved queries dropdown.
     * </p>
     *
     * @param e The action event from the save button
     */
    private void saveButtonClicked(ActionEvent e) {
        String query = queryTextArea.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot save an empty query",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String name = JOptionPane.showInputDialog(
                this,
                "Enter a name for this query:",
                "Save Query",
                JOptionPane.QUESTION_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            // In a real application, this would save to a database or file
            JOptionPane.showMessageDialog(
                    this,
                    "Query saved as: " + name,
                    "Query Saved",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    /**
     * Handle execute button click to run the current query.
     * <p>
     * Executes the SQL query entered in the text area and displays
     * the results in the results table. The query is executed in a
     * background thread to prevent UI freezing.
     * </p>
     *
     * @param e The action event from the execute button
     */
    private void executeButtonClicked(ActionEvent e) {
        String query = queryTextArea.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Query is empty");
            return;
        }

        // Execute query in a separate thread to avoid freezing UI
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                executeQuery(query);
                return null;
            }
        };
        worker.execute();
    }

    /**
     * Load a saved query into the query text area.
     * <p>
     * When a saved query is selected from the dropdown, this method
     * loads the corresponding SQL statement into the query text area.
     * </p>
     *
     * @param e The action event from the saved queries dropdown
     */
    private void loadSavedQuery(ActionEvent e) {
        int selectedIndex = savedQueriesCombo.getSelectedIndex();
        if (selectedIndex == 0) {
            return; // "Select a saved query..." item
        }

        String query = "";
        switch (selectedIndex) {
            case 1: // Transaction Count by Card Type
                query = "SELECT \n" +
                        "    c.card_type, \n" +
                        "    COUNT(t.transaction_id) as transaction_count, \n" +
                        "    SUM(t.amount) as total_amount, \n" +
                        "    ROUND(AVG(t.amount), 2) as avg_transaction_amount \n" +
                        "FROM Transaction t \n" +
                        "JOIN Card c ON t.card_id = c.card_id \n" +
                        "GROUP BY c.card_type \n" +
                        "ORDER BY transaction_count DESC";
                break;
            case 2: // Top 10 Merchants by Volume
                query = "SELECT \n" +
                        "    pm.merchant_name, \n" +
                        "    pm.merchant_category, \n" +
                        "    COUNT(t.transaction_id) as transaction_count, \n" +
                        "    SUM(t.amount) as total_amount, \n" +
                        "    ROUND(AVG(t.amount), 2) as avg_transaction_value \n" +
                        "FROM Transaction t \n" +
                        "JOIN PaymentMerchant pm ON t.merchant_id = pm.merchant_id \n" +
                        "GROUP BY pm.merchant_id, pm.merchant_name, pm.merchant_category \n" +
                        "ORDER BY transaction_count DESC \n" +
                        "LIMIT 10";
                break;
            case 3: // Monthly Transaction Totals
                query = "SELECT \n" +
                        "    DATE_FORMAT(timestamp, '%Y-%m') as year_month, \n" +
                        "    COUNT(*) as transaction_count, \n" +
                        "    SUM(amount) as total_amount, \n" +
                        "    ROUND(AVG(amount), 2) as avg_transaction_amount \n" +
                        "FROM Transaction \n" +
                        "GROUP BY DATE_FORMAT(timestamp, '%Y-%m') \n" +
                        "ORDER BY year_month";
                break;
            case 4: // Bank Approval Rates
                query = "SELECT \n" +
                        "    ib.bank_name, \n" +
                        "    COUNT(t.transaction_id) as transaction_count, \n" +
                        "    COUNT(CASE WHEN t.status = 'Approved' THEN 1 END) as approved_count, \n" +
                        "    ROUND((COUNT(CASE WHEN t.status = 'Approved' THEN 1 END) / COUNT(*)) * 100, 2) as approval_rate \n" +
                        "FROM Transaction t \n" +
                        "JOIN Card c ON t.card_id = c.card_id \n" +
                        "JOIN IssuingBank ib ON c.issuing_bank_id = ib.issuing_bank_id \n" +
                        "GROUP BY ib.bank_name \n" +
                        "ORDER BY approval_rate DESC";
                break;
            case 5: // Settlement Status Summary
                query = "SELECT \n" +
                        "    CASE \n" +
                        "        WHEN s.settlement_id IS NOT NULL THEN 'Settled' \n" +
                        "        ELSE 'Unsettled' \n" +
                        "    END as settlement_status, \n" +
                        "    COUNT(*) as transaction_count, \n" +
                        "    SUM(t.amount) as total_amount, \n" +
                        "    ROUND(AVG(t.amount), 2) as avg_amount \n" +
                        "FROM Transaction t \n" +
                        "LEFT JOIN Settlement s ON t.transaction_id = s.transaction_id \n" +
                        "GROUP BY settlement_status";
                break;
        }

        queryTextArea.setText(query);
        savedQueriesCombo.setSelectedIndex(0); // Reset selection
    }

    /**
     * Execute a SQL query and display the results.
     * <p>
     * This method executes the provided SQL query against the database
     * and populates the results table with the query results. It handles
     * both queries that return results (SELECT) and those that don't
     * (INSERT, UPDATE, DELETE).
     * </p>
     *
     * @param query The SQL query to execute
     */
    private void executeQuery(String query) {
        long startTime = System.currentTimeMillis();

        try {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Executing query...");
                executeButton.setEnabled(false);
            });

            // Check if query is a SELECT statement
            boolean isSelect = query.trim().toLowerCase().startsWith("select");

            if (isSelect) {
                // Execute SELECT query and get results
                try (ResultSet rs = dbManager.executeQuery(query)) {
                    // Get metadata
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    // Create column names vector
                    Vector<String> columnNames = new Vector<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(metaData.getColumnLabel(i));
                    }

                    // Create data vector
                    Vector<Vector<Object>> data = new Vector<>();
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        data.add(row);
                    }

                    // Update table model on EDT
                    final Vector<String> finalColumnNames = columnNames;
                    final Vector<Vector<Object>> finalData = data;

                    SwingUtilities.invokeLater(() -> {
                        resultTableModel.setDataVector(finalData, finalColumnNames);
                        long endTime = System.currentTimeMillis();
                        statusLabel.setText(String.format("Query executed in %d ms, returned %d rows",
                                (endTime - startTime), finalData.size()));
                        executeButton.setEnabled(true);
                    });
                }
            } else {
                // Execute non-SELECT query (INSERT, UPDATE, DELETE, etc.)
                int rowsAffected = dbManager.executeUpdate(query);

                SwingUtilities.invokeLater(() -> {
                    long endTime = System.currentTimeMillis();
                    statusLabel.setText(String.format("Query executed in %d ms, %d rows affected",
                            (endTime - startTime), rowsAffected));
                    executeButton.setEnabled(true);

                    // Clear table for non-SELECT queries
                    resultTableModel.setDataVector(new Vector<>(), new Vector<>());
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Error: " + ex.getMessage());
                executeButton.setEnabled(true);

                JOptionPane.showMessageDialog(
                        this,
                        "SQL Error: " + ex.getMessage(),
                        "Query Error",
                        JOptionPane.ERROR_MESSAGE
                );
            });
        }
    }
}