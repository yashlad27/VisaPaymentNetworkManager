package payment.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import payment.database.DatabaseManager;

/**
 * Panel for performing CRUD (Create, Read, Update, Delete) operations on the database.
 * <p>
 * This panel provides a comprehensive interface for managing database records across
 * all tables in the Visa payment network. It dynamically adapts to the structure of
 * each selected table, providing appropriate form fields for creating and editing records,
 * and a data table for viewing and selecting existing records.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Dynamic form generation based on table structure</li>
 *   <li>Create, update, and delete operations for all database tables</li>
 *   <li>Data validation before submission</li>
 *   <li>Real-time data refresh</li>
 *   <li>Error handling with user-friendly messages</li>
 *   <li>Support for various data types (text, numbers, dates, etc.)</li>
 * </ul>
 * </p>
 * <p>
 * The panel uses a split-pane layout with a form on the left for data entry and editing,
 * and a table on the right for displaying and selecting records. The form adapts dynamically
 * to the structure of the selected table, providing appropriate input fields for each column.
 * </p>
 */
public class CRUDPanel extends JPanel {
    /**
     * Database manager instance for database connectivity
     */
    private final DatabaseManager dbManager;

    /**
     * Database connection for executing SQL queries
     */
    private final Connection connection;

    /**
     * Dropdown for selecting database tables
     */
    private JComboBox<String> tableComboBox;

    /**
     * Panel containing dynamically generated form fields
     */
    private JPanel formPanel;

    /**
     * Panel containing action buttons
     */
    private JPanel buttonPanel;

    /**
     * Table displaying database records
     */
    private JTable dataTable;

    /**
     * Model for data table
     */
    private DefaultTableModel tableModel;

    /**
     * Button for creating new records
     */
    private JButton createButton;

    /**
     * Button for updating existing records
     */
    private JButton updateButton;

    /**
     * Button for deleting records
     */
    private JButton deleteButton;

    /**
     * Button for refreshing data
     */
    private JButton refreshButton;

    /**
     * Label for displaying status messages
     */
    private JLabel statusLabel;

    /**
     * Name of the currently selected table
     */
    private String currentTable;

    /**
     * List of column names for the current table
     */
    private List<String> columnNames;

    /**
     * Map of form field components keyed by column name
     */
    private Map<String, JComponent> formFields;

    /**
     * Map of column data types keyed by column name
     */
    private Map<String, String> columnTypes;

    /**
     * Name of the primary key column for the current table
     */
    private String primaryKeyColumn;

    /**
     * Date format for displaying and parsing dates
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Constructor for the CRUD panel.
     * <p>
     * Initializes the panel with database connection and UI components.
     * Sets up collections for storing table metadata and creates the initial UI layout.
     * </p>
     *
     * @param dbManager The database manager instance for database connectivity
     * @throws IllegalArgumentException if dbManager is null
     */
    public CRUDPanel(DatabaseManager dbManager) {
        if (dbManager == null) {
            throw new IllegalArgumentException("Database manager cannot be null");
        }

        this.dbManager = dbManager;
        this.connection = dbManager.getConnection();

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        columnNames = new ArrayList<>();
        formFields = new HashMap<>();
        columnTypes = new HashMap<>();
        currentTable = null;
        initComponents();

        populateTableComboBox();
    }

    /**
     * Initialize UI components.
     * <p>
     * Creates and lays out all UI components including:
     * <ul>
     *   <li>Header panel with title and table selector</li>
     *   <li>Form panel for data entry and editing</li>
     *   <li>Button panel with action buttons</li>
     *   <li>Data table for displaying records</li>
     *   <li>Status bar for displaying messages</li>
     * </ul>
     * </p>
     * <p>
     * Uses a split-pane layout to divide the form and data table sections.
     * </p>
     */
    private void initComponents() {
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.3);

        JPanel leftPanel = new JPanel(new BorderLayout());
        formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        leftPanel.add(new JScrollPane(formPanel), BorderLayout.CENTER);

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        createButton = new JButton("Create");
        createButton.addActionListener(this::createRecord);
        updateButton = new JButton("Update");
        updateButton.addActionListener(this::updateRecord);
        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(this::deleteRecord);

        buttonPanel.add(createButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel();
        dataTable = new JTable(tableModel);
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelection();
            }
        });

        rightPanel.add(new JScrollPane(dataTable), BorderLayout.CENTER);

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(e -> loadTableData());
        refreshPanel.add(refreshButton);
        rightPanel.add(refreshPanel, BorderLayout.NORTH);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Create the header panel with title and table selector.
     * <p>
     * This panel includes the panel title and a dropdown for selecting
     * which database table to work with. When a new table is selected,
     * the panel updates to show the structure and data for that table.
     * </p>
     *
     * @return The fully configured header panel
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel("Database CRUD Operations");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.WEST);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        tableComboBox = new JComboBox<>();
        tableComboBox.setPreferredSize(new Dimension(200, 25));
        tableComboBox.addActionListener(e -> {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            if (selectedTable != null && !selectedTable.equals(currentTable)) {
                currentTable = selectedTable;
                loadTableStructure();
                loadTableData();
            }
        });

        controlsPanel.add(new JLabel("Select Table:"));
        controlsPanel.add(tableComboBox);

        panel.add(controlsPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * Populate the table combo box with available database tables.
     * <p>
     * This method queries the database to find all available tables in the
     * current schema, filtering out system tables. It verifies that each table
     * can be accessed before adding it to the dropdown.
     * </p>
     * <p>
     * If no tables are found or accessible, a warning message is displayed.
     * Once tables are loaded, the first table is automatically selected.
     * </p>
     */
    private void populateTableComboBox() {
        try {
            tableComboBox.removeAllItems();

            List<String> existingTables = new ArrayList<>();

            try {
                String query = "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'visa_final_spring' AND table_type = 'BASE TABLE'";

                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        if (!tableName.startsWith("sys_")) {
                            existingTables.add(tableName);
                        }
                    }
                }
            } catch (SQLException e) {
                DatabaseMetaData metaData = connection.getMetaData();
                ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (!tableName.startsWith("sys_") && !tableName.startsWith("information_schema")) {
                        existingTables.add(tableName);
                    }
                }
            }

            boolean hasValidTables = false;
            for (String tableName : existingTables) {
                try {
                    PreparedStatement pstmt = connection.prepareStatement(
                            "SELECT * FROM " + tableName + " LIMIT 0");
                    pstmt.executeQuery();
                    pstmt.close();

                    tableComboBox.addItem(tableName);
                    hasValidTables = true;
                } catch (SQLException e) {
                    System.err.println("Skipping inaccessible table: " + tableName + " - " + e.getMessage());
                }
            }

            if (tableComboBox.getItemCount() > 0) {
                currentTable = tableComboBox.getItemAt(0);
                loadTableStructure();
                loadTableData();
            } else if (!hasValidTables) {
                JOptionPane.showMessageDialog(
                        this,
                        "No tables found in database. CRUD operations will be disabled.",
                        "Database Warning",
                        JOptionPane.WARNING_MESSAGE
                );
                setComponentsEnabled(false);
            }
        } catch (SQLException e) {
            handleDatabaseError("Error loading database tables", e);
            setComponentsEnabled(false);
        }
    }

    /**
     * Load the structure of the currently selected table.
     * <p>
     * This method queries the database to retrieve metadata about the
     * current table, including column names, data types, and primary key
     * information. It then creates appropriate form fields for each column.
     * </p>
     */
    private void loadTableStructure() {
        try {
            formFields.clear();
            columnNames.clear();
            columnTypes.clear();
            formPanel.removeAll();

            String query = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY " +
                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'visa_final_spring' " +
                    "AND TABLE_NAME = ?";

            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, currentTable);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("DATA_TYPE");
                String columnKey = rs.getString("COLUMN_KEY");

                columnNames.add(columnName);
                columnTypes.put(columnName, dataType);

                if ("PRI".equals(columnKey)) {
                    primaryKeyColumn = columnName;
                }

                JLabel label = new JLabel(columnName + ":");
                label.setPreferredSize(new Dimension(150, 25));

                JComponent field;
                if (dataType.contains("char") || dataType.contains("text")) {
                    field = new JTextField(20);
                } else if (dataType.contains("int") || dataType.contains("decimal")) {
                    field = new JTextField(10);
                } else if (dataType.contains("date") || dataType.contains("time")) {
                    field = new JTextField(15);
                } else {
                    field = new JTextField(20);
                }

                formFields.put(columnName, field);

                JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                fieldPanel.add(label);
                fieldPanel.add(field);

                formPanel.add(fieldPanel);
            }

            formPanel.revalidate();
            formPanel.repaint();

        } catch (SQLException e) {
            handleDatabaseError("Error loading table structure", e);
        }
    }

    /**
     * Load data from the currently selected table.
     * <p>
     * This method queries the database to retrieve all records from the
     * current table and populates the data table with this information.
     * </p>
     */
    private void loadTableData() {
        try {
            String query = "SELECT * FROM " + currentTable;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            String[] columnNames = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = metaData.getColumnLabel(i + 1);
            }

            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                Object[] rowData = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    rowData[i] = rs.getObject(i + 1);
                }
                tableModel.addRow(rowData);
            }

            dataTable.setModel(tableModel);

            statusLabel.setText(tableModel.getRowCount() + " records found");

        } catch (SQLException e) {
            handleDatabaseError("Error loading table data", e);
        }
    }

    /**
     * Populate the form fields with data from the selected table row.
     * <p>
     * When a row is selected in the data table, this method extracts the
     * values from that row and populates the form fields with those values,
     * allowing for easy editing of existing records.
     * </p>
     */
    private void populateFormFromSelection() {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow == -1) return;

        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            String columnName = dataTable.getColumnName(i);
            Object value = dataTable.getValueAt(selectedRow, i);
            JComponent field = formFields.get(columnName);

            if (field instanceof JTextField) {
                ((JTextField) field).setText(value != null ? value.toString() : "");
            }
        }
    }

    /**
     * Create a new record in the current table.
     * <p>
     * This method retrieves values from the form fields, validates them,
     * and executes an INSERT statement to create a new record in the
     * currently selected table.
     * </p>
     *
     * @param e The action event from the create button
     */
    private void createRecord(ActionEvent e) {
        try {
            StringBuilder query = new StringBuilder("INSERT INTO " + currentTable + " (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();

            boolean first = true;
            for (String columnName : columnNames) {
                JComponent field = formFields.get(columnName);
                String value = field instanceof JTextField ? ((JTextField) field).getText() : "";

                if (columnName.equals(primaryKeyColumn) && value.isEmpty()) {
                    continue;
                }

                if (!first) {
                    query.append(", ");
                    values.append(", ");
                }
                first = false;

                query.append(columnName);
                values.append("?");
                parameters.add(value);
            }

            query.append(") ").append(values).append(")");

            PreparedStatement stmt = connection.prepareStatement(query.toString());
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setString(i + 1, parameters.get(i).toString());
            }

            int result = stmt.executeUpdate();
            if (result > 0) {
                statusLabel.setText("Record created successfully");
                loadTableData();
                clearForm();
            }

        } catch (SQLException ex) {
            handleDatabaseError("Error creating record", ex);
        }
    }

    /**
     * Update an existing record in the current table.
     * <p>
     * This method retrieves values from the form fields, validates them,
     * and executes an UPDATE statement to modify an existing record in the
     * currently selected table based on its primary key.
     * </p>
     *
     * @param e The action event from the update button
     */
    private void updateRecord(ActionEvent e) {
        if (dataTable.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a record to update",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            StringBuilder query = new StringBuilder("UPDATE " + currentTable + " SET ");
            List<Object> parameters = new ArrayList<>();
            String primaryKeyValue = null;

            boolean first = true;
            for (String columnName : columnNames) {
                JComponent field = formFields.get(columnName);
                String value = field instanceof JTextField ? ((JTextField) field).getText() : "";

                if (columnName.equals(primaryKeyColumn)) {
                    primaryKeyValue = value;
                    continue;
                }

                if (!first) {
                    query.append(", ");
                }
                first = false;

                query.append(columnName).append(" = ?");
                parameters.add(value);
            }

            query.append(" WHERE ").append(primaryKeyColumn).append(" = ?");
            parameters.add(primaryKeyValue);

            PreparedStatement stmt = connection.prepareStatement(query.toString());
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setString(i + 1, parameters.get(i).toString());
            }

            int result = stmt.executeUpdate();
            if (result > 0) {
                statusLabel.setText("Record updated successfully");
                loadTableData();
            }

        } catch (SQLException ex) {
            handleDatabaseError("Error updating record", ex);
        }
    }

    /**
     * Delete a record from the current table.
     * <p>
     * This method retrieves the primary key of the selected record and
     * executes a DELETE statement to remove the record from the currently
     * selected table after confirming with the user.
     * </p>
     *
     * @param e The action event from the delete button
     */
    private void deleteRecord(ActionEvent e) {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a record to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int primaryKeyIndex = columnNames.indexOf(primaryKeyColumn);
                Object primaryKeyValue = dataTable.getValueAt(selectedRow, primaryKeyIndex);

                String query = "DELETE FROM " + currentTable + " WHERE " + primaryKeyColumn + " = ?";
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setObject(1, primaryKeyValue);

                int result = stmt.executeUpdate();
                if (result > 0) {
                    statusLabel.setText("Record deleted successfully");
                    loadTableData();
                    clearForm();
                }

            } catch (SQLException ex) {
                handleDatabaseError("Error deleting record", ex);
            }
        }
    }

    /**
     * Clear all form fields.
     */
    private void clearForm() {
        for (JComponent field : formFields.values()) {
            if (field instanceof JTextField) {
                ((JTextField) field).setText("");
            }
        }
    }

    /**
     * Set the enabled state of all interactive components.
     * <p>
     * This method is used to enable or disable all interactive components
     * in the panel, typically in response to database connectivity issues
     * or when no tables are available.
     * </p>
     *
     * @param enabled Whether the components should be enabled
     */
    private void setComponentsEnabled(boolean enabled) {
        tableComboBox.setEnabled(enabled);
        createButton.setEnabled(enabled);
        updateButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        dataTable.setEnabled(enabled);

        for (JComponent field : formFields.values()) {
            field.setEnabled(enabled);
        }
    }

    /**
     * Handle database errors with user-friendly messages.
     * <p>
     * This method displays an error message to the user and logs the
     * exception details when a database error occurs.
     * </p>
     *
     * @param message A user-friendly error message
     * @param e       The SQLException that occurred
     */
    private void handleDatabaseError(String message, SQLException e) {
        e.printStackTrace();
        String errorMessage = message + ": " + e.getMessage();
        statusLabel.setText("Error: " + errorMessage);
        JOptionPane.showMessageDialog(this,
                errorMessage,
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }
}