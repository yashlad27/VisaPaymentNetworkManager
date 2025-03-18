package gui;

import database.DatabaseManager;
import database.AggregateOperations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AggregatePanel extends JPanel {
    private DatabaseManager dbManager;
    private AggregateOperations aggregateOps;

    private JTabbedPane aggregateTabs;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    // Custom Aggregate UI components
    private JComboBox<String> tableComboBox;
    private JComboBox<String> functionComboBox;
    private JComboBox<String> columnComboBox;
    private JTextField whereField;
    private JComboBox<String> groupByComboBox;
    private JButton executeAggregateButton;

    // Predefined Aggregate UI components
    private JComboBox<String> predefinedQueryComboBox;
    private JButton executePredefinedButton;

    public AggregatePanel(DatabaseManager databaseManager) {
        this.dbManager = DatabaseManager.getInstance();
        this.aggregateOps = new AggregateOperations();

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initializeUI();
    }

    private void initializeUI() {
        // Create tabbed pane for different aggregate options
        aggregateTabs = new JTabbedPane();

        // Create custom aggregate panel
        JPanel customAggregatePanel = createCustomAggregatePanel();
        aggregateTabs.addTab("Custom Aggregate", customAggregatePanel);

        // Create predefined aggregates panel
        JPanel predefinedAggregatesPanel = createPredefinedAggregatesPanel();
        aggregateTabs.addTab("Predefined Aggregates", predefinedAggregatesPanel);

        // Create table model and results table
        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        JScrollPane resultScrollPane = new JScrollPane(resultTable);

        // Add components to main panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, aggregateTabs, resultScrollPane);
        splitPane.setDividerLocation(200);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createCustomAggregatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create control panel with form layout
        JPanel controlPanel = new JPanel(new GridLayout(6, 2, 5, 5));

        // Table selection
        controlPanel.add(new JLabel("Select Table:"));
        tableComboBox = new JComboBox<>();
        tableComboBox.setEnabled(false);
        tableComboBox.addActionListener(e -> populateColumnComboBoxes());
        controlPanel.add(tableComboBox);

        // Function selection
        controlPanel.add(new JLabel("Aggregate Function:"));
        functionComboBox = new JComboBox<>(new String[]{
                "COUNT(*)", "SUM", "AVG", "MIN", "MAX", "COUNT"
        });
        functionComboBox.setEnabled(false);
        functionComboBox.addActionListener(e -> handleFunctionSelection());
        controlPanel.add(functionComboBox);

        // Column selection
        controlPanel.add(new JLabel("Column:"));
        columnComboBox = new JComboBox<>();
        columnComboBox.setEnabled(false);
        controlPanel.add(columnComboBox);

        // WHERE clause
        controlPanel.add(new JLabel("WHERE Clause (optional):"));
        whereField = new JTextField();
        whereField.setEnabled(false);
        controlPanel.add(whereField);

        // GROUP BY clause
        controlPanel.add(new JLabel("GROUP BY Column (optional):"));
        groupByComboBox = new JComboBox<>();
        groupByComboBox.setEnabled(false);
        groupByComboBox.addItem(""); // Empty option
        controlPanel.add(groupByComboBox);

        // Execute button
        controlPanel.add(new JLabel(""));
        executeAggregateButton = new JButton("Execute Aggregate");
        executeAggregateButton.setEnabled(false);
        executeAggregateButton.addActionListener(e -> executeCustomAggregate());
        controlPanel.add(executeAggregateButton);

        // Add description area
        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setRows(3);
        descriptionArea.setText("Use aggregate functions to analyze data. Select a table, function, and column, " +
                "then optionally add WHERE conditions and GROUP BY clauses to refine your results.");
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);

        // Add components to panel
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(descScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPredefinedAggregatesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Predefined query selection
        controlPanel.add(new JLabel("Select Predefined Query:"));
        predefinedQueryComboBox = new JComboBox<>();
        predefinedQueryComboBox.setEnabled(false);
        controlPanel.add(predefinedQueryComboBox);

        // Execute button
        executePredefinedButton = new JButton("Execute");
        executePredefinedButton.setEnabled(false);
        executePredefinedButton.addActionListener(e -> executePredefinedAggregate());
        controlPanel.add(executePredefinedButton);

        // Add description area
        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setRows(5);
        descriptionArea.setText("Predefined aggregates provide quick insights into your data. " +
                "These are common aggregate queries that analyze transaction patterns, " +
                "merchant activity, card usage, and more.");
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);

        // Add components to panel
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(descScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void populateColumnComboBoxes() {
        String selectedTable = (String) tableComboBox.getSelectedItem();

        if (selectedTable != null && !selectedTable.isEmpty()) {
            // Get column names for the selected table
            String[] columnNames = aggregateOps.getColumnNames(selectedTable);

            // Update column combo box
            columnComboBox.removeAllItems();
            for (String columnName : columnNames) {
                columnComboBox.addItem(columnName);
            }

            // Update group by combo box
            groupByComboBox.removeAllItems();
            groupByComboBox.addItem(""); // Empty option for no grouping
            for (String columnName : columnNames) {
                groupByComboBox.addItem(columnName);
            }

            // Enable form controls
            columnComboBox.setEnabled(true);
            groupByComboBox.setEnabled(true);
        }
    }

    private void handleFunctionSelection() {
        String selectedFunction = (String) functionComboBox.getSelectedItem();

        // Disable column selection for COUNT(*) since it doesn't need a column
        if (selectedFunction != null && selectedFunction.equals("COUNT(*)")) {
            columnComboBox.setEnabled(false);
        } else {
            columnComboBox.setEnabled(true);
        }
    }

    private void executeCustomAggregate() {
        String tableName = (String) tableComboBox.getSelectedItem();
        String function = (String) functionComboBox.getSelectedItem();
        String column = (String) columnComboBox.getSelectedItem();
        String whereClause = whereField.getText().trim();
        String groupBy = (String) groupByComboBox.getSelectedItem();

        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a table",
                    "Aggregate Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (function == null || function.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an aggregate function",
                    "Aggregate Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!function.equals("COUNT(*)") && (column == null || column.isEmpty())) {
            JOptionPane.showMessageDialog(this, "Please select a column for the aggregate function",
                    "Aggregate Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Execute the aggregate
        aggregateOps.executeAggregate(tableName, function, column, whereClause, groupBy, tableModel);
    }

    private void executePredefinedAggregate() {
        String queryName = (String) predefinedQueryComboBox.getSelectedItem();

        if (queryName == null || queryName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a predefined query",
                    "Aggregate Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Execute the predefined aggregate
        aggregateOps.executePredefinedAggregate(queryName, tableModel);
    }

    // Called when the database connection is established
    public void onDatabaseConnected() {
        // Populate table dropdown
        String[] tableNames = aggregateOps.getTableNames();
        tableComboBox.removeAllItems();
        for (String tableName : tableNames) {
            tableComboBox.addItem(tableName);
        }

        // Populate predefined queries dropdown
        String[] queryNames = aggregateOps.getPredefinedQueryNames();
        predefinedQueryComboBox.removeAllItems();
        for (String queryName : queryNames) {
            predefinedQueryComboBox.addItem(queryName);
        }

        // Enable controls
        tableComboBox.setEnabled(true);
        functionComboBox.setEnabled(true);
        whereField.setEnabled(true);
        executeAggregateButton.setEnabled(true);
        predefinedQueryComboBox.setEnabled(true);
        executePredefinedButton.setEnabled(true);

        // Handle initial function selection
        handleFunctionSelection();
    }

    // Get the result table for sharing with other panels
    public JTable getResultTable() {
        return resultTable;
    }

    // Get the table model for sharing with other panels
    public DefaultTableModel getTableModel() {
        return tableModel;
    }
}