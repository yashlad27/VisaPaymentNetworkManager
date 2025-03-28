package main.java.payment.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import main.java.payment.database.DatabaseManager;

/**
 * Panel for performing CRUD (Create, Read, Update, Delete) operations on the database.
 */
public class CRUDPanel extends JPanel {
  private final DatabaseManager dbManager;
  private final Connection connection;

  // UI Components
  private JComboBox<String> tableComboBox;
  private JPanel formPanel;
  private JPanel buttonPanel;
  private JTable dataTable;
  private DefaultTableModel tableModel;
  private JButton createButton;
  private JButton updateButton;
  private JButton deleteButton;
  private JButton refreshButton;
  private JLabel statusLabel;

  // Current table context
  private String currentTable;
  private List<String> columnNames;
  private Map<String, JComponent> formFields;
  private Map<String, String> columnTypes;
  private String primaryKeyColumn;

  // Date format for displaying and parsing dates
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * Constructor for the CRUD panel.
   *
   * @param dbManager The database manager
   */
  public CRUDPanel(DatabaseManager dbManager) {
    this.dbManager = dbManager;
    this.connection = dbManager.getConnection();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    // Initialize collections
    columnNames = new ArrayList<>();
    formFields = new HashMap<>();
    columnTypes = new HashMap<>();

    // Create UI Components
    initComponents();

    // Load tables
    populateTableComboBox();
  }

  /**
   * Initialize UI components.
   */
  private void initComponents() {
    // Create header panel
    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);

    // Create main panel with form and data
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setResizeWeight(0.3); // 30% to form, 70% to data

    // Create form panel (left side)
    JPanel leftPanel = new JPanel(new BorderLayout());
    formPanel = new JPanel();
    formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
    leftPanel.add(new JScrollPane(formPanel), BorderLayout.CENTER);

    // Create button panel
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

    // Create data panel (right side)
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

    // Add refresh button
    JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    refreshButton = new JButton("Refresh Data");
    refreshButton.addActionListener(e -> loadTableData());
    refreshPanel.add(refreshButton);
    rightPanel.add(refreshPanel, BorderLayout.NORTH);

    // Add panels to split pane
    splitPane.setLeftComponent(leftPanel);
    splitPane.setRightComponent(rightPanel);

    // Add split pane to main panel
    add(splitPane, BorderLayout.CENTER);

    // Create status bar
    statusLabel = new JLabel("Ready");
    statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
    ));
    add(statusLabel, BorderLayout.SOUTH);
  }

  /**
   * Create the header panel with title and table selector.
   *
   * @return The header panel
   */
  private JPanel createHeaderPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(0, 0, 10, 0));

    // Title label
    JLabel titleLabel = new JLabel("Database CRUD Operations");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
    panel.add(titleLabel, BorderLayout.WEST);

    // Controls panel
    JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    // Table combobox
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
   * Populate the table combo box with database tables.
   */
  private void populateTableComboBox() {
    try {
      DatabaseMetaData metaData = connection.getMetaData();
      ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

      while (tables.next()) {
        String tableName = tables.getString("TABLE_NAME");
        // Skip system tables
        if (!tableName.startsWith("sys_") && !tableName.startsWith("information_schema")) {
          tableComboBox.addItem(tableName);
        }
      }

      // Set the first table as selected if available
      if (tableComboBox.getItemCount() > 0) {
        currentTable = tableComboBox.getItemAt(0);
        loadTableStructure();
        loadTableData();
      }

    } catch (SQLException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(
              this,
              "Error loading tables: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE
      );
    }
  }

  /**
   * Load the structure of the selected table and create input fields.
   */
  private void loadTableStructure() {
    try {
      // Clear previous form fields
      formPanel.removeAll();
      formFields.clear();
      columnNames.clear();
      columnTypes.clear();
      primaryKeyColumn = null;

      // Add a titled border with the table name
      formPanel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(),
              currentTable + " Record",
              TitledBorder.LEFT,
              TitledBorder.TOP
      ));

      // Get table metadata
      DatabaseMetaData dbMetaData = connection.getMetaData();

      // Find primary key
      ResultSet primaryKeys = dbMetaData.getPrimaryKeys(null, null, currentTable);
      while (primaryKeys.next()) {
        primaryKeyColumn = primaryKeys.getString("COLUMN_NAME");
      }

      // Get column information
      ResultSet columns = dbMetaData.getColumns(null, null, currentTable, null);
      while (columns.next()) {
        String columnName = columns.getString("COLUMN_NAME");
        String columnType = columns.getString("TYPE_NAME");
        int nullable = columns.getInt("NULLABLE");

        columnNames.add(columnName);
        columnTypes.put(columnName, columnType);

        // Create label and input field
        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.setBorder(new EmptyBorder(2, 5, 2, 5));

        JLabel label = new JLabel(columnName + ":");
        label.setPreferredSize(new Dimension(150, 25));

        JComponent inputField;

        // Create appropriate input field based on column type
        if (columnName.equals(primaryKeyColumn) && columnType.contains("INT")) {
          // Primary key is usually auto-increment
          JTextField field = new JTextField();
          field.setEditable(false);
          field.setBackground(new Color(240, 240, 240));
          inputField = field;
        } else if (columnType.contains("CHAR") || columnType.contains("VARCHAR") ||
                columnType.contains("TEXT")) {
          // String type
          inputField = new JTextField();
        } else if (columnType.contains("INT") || columnType.contains("FLOAT") ||
                columnType.contains("DOUBLE") || columnType.contains("DECIMAL")) {
          // Numeric type
          inputField = new JTextField();
        } else if (columnType.contains("DATE")) {
          // Date type
          inputField = new JTextField();
          ((JTextField) inputField).setToolTipText("Format: YYYY-MM-DD");
        } else if (columnType.contains("BOOL") || columnType.contains("TINYINT(1)")) {
          // Boolean type
          inputField = new JCheckBox();
        } else {
          // Default to text field
          inputField = new JTextField();
        }

        // Set field properties
        inputField.setPreferredSize(new Dimension(200, 25));
        if (nullable == DatabaseMetaData.columnNoNulls) {
          label.setText(label.getText() + " *");
        }

        fieldPanel.add(label, BorderLayout.WEST);
        fieldPanel.add(inputField, BorderLayout.CENTER);

        formPanel.add(fieldPanel);
        formFields.put(columnName, inputField);
      }

      // Add some spacing at the bottom
      formPanel.add(Box.createVerticalGlue());

      // Update UI
      formPanel.revalidate();
      formPanel.repaint();

    } catch (SQLException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(
              this,
              "Error loading table structure: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE
      );
    }
  }

  /**
   * Load data from the selected table.
   */
  private void loadTableData() {
    try {
      String query = "SELECT * FROM " + currentTable;

      try (Statement stmt = connection.createStatement();
           ResultSet rs = stmt.executeQuery(query)) {

        // Get metadata
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Create column names array
        String[] columns = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
          columns[i] = metaData.getColumnLabel(i + 1);
        }

        // Create data model
        tableModel = new DefaultTableModel(columns, 0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return false; // Make all cells non-editable
          }
        };

        // Add rows to model
        while (rs.next()) {
          Object[] row = new Object[columnCount];
          for (int i = 0; i < columnCount; i++) {
            row[i] = rs.getObject(i + 1);
          }
          tableModel.addRow(row);
        }

        // Update table
        dataTable.setModel(tableModel);

        // Update status
        statusLabel.setText("Loaded " + tableModel.getRowCount() + " records from " + currentTable);
      }
    } catch (SQLException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(
              this,
              "Error loading table data: " + e.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE
      );
    }
  }

  /**
   * Populate form fields from the selected table row.
   */
  private void populateFormFromSelection() {
    int selectedRow = dataTable.getSelectedRow();
    if (selectedRow == -1) {
      return; // No row selected
    }

    for (int i = 0; i < columnNames.size(); i++) {
      String columnName = columnNames.get(i);
      JComponent field = formFields.get(columnName);
      Object value = dataTable.getValueAt(selectedRow, i);

      if (field instanceof JTextField) {
        ((JTextField) field).setText(value != null ? value.toString() : "");
      } else if (field instanceof JCheckBox) {
        ((JCheckBox) field).setSelected(value != null && (value.equals(1) || value.equals(true)));
      }
    }
  }

  /**
   * Clear all form fields.
   */
  private void clearFormFields() {
    for (JComponent field : formFields.values()) {
      if (field instanceof JTextField) {
        ((JTextField) field).setText("");
      } else if (field instanceof JCheckBox) {
        ((JCheckBox) field).setSelected(false);
      }
    }
  }

  /**
   * Create a new record from form data.
   *
   * @param e The action event
   */
  private void createRecord(ActionEvent e) {
    try {
      // Build insert query
      StringBuilder query = new StringBuilder("INSERT INTO " + currentTable + " (");
      StringBuilder values = new StringBuilder("VALUES (");
      List<Object> paramValues = new ArrayList<>();

      boolean first = true;

      for (String columnName : columnNames) {
        // Skip primary key if it's auto-increment
        if (columnName.equals(primaryKeyColumn) &&
                columnTypes.get(columnName).contains("INT") &&
                ((JTextField) formFields.get(columnName)).getText().isEmpty()) {
          continue;
        }

        JComponent field = formFields.get(columnName);
        String columnType = columnTypes.get(columnName);
        Object value = null;

        // Get value from field
        if (field instanceof JTextField) {
          String text = ((JTextField) field).getText().trim();
          if (!text.isEmpty()) {
            if (columnType.contains("INT")) {
              value = Integer.parseInt(text);
            } else if (columnType.contains("FLOAT") || columnType.contains("DOUBLE") ||
                    columnType.contains("DECIMAL")) {
              value = Double.parseDouble(text);
            } else if (columnType.contains("DATE")) {
              try {
                value = dateFormat.parse(text);
              } catch (ParseException ex) {
                throw new IllegalArgumentException("Invalid date format for " + columnName +
                        ". Use YYYY-MM-DD format.");
              }
            } else {
              value = text;
            }
          }
        } else if (field instanceof JCheckBox) {
          value = ((JCheckBox) field).isSelected() ? 1 : 0;
        }

        // If value is not null or the column allows NULL
        if (value != null || !field.getParent().getComponent(0).toString().contains("*")) {
          if (!first) {
            query.append(", ");
            values.append(", ");
          }

          query.append(columnName);
          values.append("?");
          paramValues.add(value);
          first = false;
        }
      }

      query.append(") ");
      values.append(")");
      query.append(values);

      // Execute insert
      PreparedStatement pstmt = connection.prepareStatement(query.toString());
      for (int i = 0; i < paramValues.size(); i++) {
        pstmt.setObject(i + 1, paramValues.get(i));
      }

      int rowsAffected = pstmt.executeUpdate();

      if (rowsAffected > 0) {
        statusLabel.setText("Record created successfully");
        clearFormFields();
        loadTableData(); // Refresh table
      } else {
        statusLabel.setText("Failed to create record");
      }

    } catch (SQLException ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(
              this,
              "Error creating record: " + ex.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE
      );
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(
              this,
              "Invalid number format: " + ex.getMessage(),
              "Input Error",
              JOptionPane.ERROR_MESSAGE
      );
    } catch (IllegalArgumentException ex) {
      JOptionPane.showMessageDialog(
              this,
              ex.getMessage(),
              "Input Error",
              JOptionPane.ERROR_MESSAGE
      );
    }
  }

  /**
   * Update the selected record with form data.
   *
   * @param e The action event
   */
  private void updateRecord(ActionEvent e) {
    int selectedRow = dataTable.getSelectedRow();
    if (selectedRow == -1) {
      JOptionPane.showMessageDialog(
              this,
              "Please select a record to update",
              "Selection Required",
              JOptionPane.INFORMATION_MESSAGE
      );
      return;
    }

    if (primaryKeyColumn == null) {
      JOptionPane.showMessageDialog(
              this,
              "Cannot update record: No primary key found for table",
              "Update Error",
              JOptionPane.ERROR_MESSAGE
      );
      return;
    }

    try {
      // Get primary key value
      int primaryKeyIndex = columnNames.indexOf(primaryKeyColumn);
      Object primaryKeyValue = dataTable.getValueAt(selectedRow, primaryKeyIndex);

      // Build update query
      StringBuilder query = new StringBuilder("UPDATE " + currentTable + " SET ");
      List<Object> paramValues = new ArrayList<>();

      boolean first = true;

      for (String columnName : columnNames) {
        // Skip primary key
        if (columnName.equals(primaryKeyColumn)) {
          continue;
        }

        JComponent field = formFields.get(columnName);
        String columnType = columnTypes.get(columnName);
        Object value = null;

        // Get value from field
        if (field instanceof JTextField) {
          String text = ((JTextField) field).getText().trim();
          if (!text.isEmpty()) {
            if (columnType.contains("INT")) {
              value = Integer.parseInt(text);
            } else if (columnType.contains("FLOAT") || columnType.contains("DOUBLE") ||
                    columnType.contains("DECIMAL")) {
              value = Double.parseDouble(text);
            } else if (columnType.contains("DATE")) {
              try {
                value = dateFormat.parse(text);
              } catch (ParseException ex) {
                throw new IllegalArgumentException("Invalid date format for " + columnName +
                        ". Use YYYY-MM-DD format.");
              }
            } else {
              value = text;
            }
          }
        } else if (field instanceof JCheckBox) {
          value = ((JCheckBox) field).isSelected() ? 1 : 0;
        }

        if (!first) {
          query.append(", ");
        }

        query.append(columnName).append(" = ?");
        paramValues.add(value);
        first = false;
      }

      query.append(" WHERE ").append(primaryKeyColumn).append(" = ?");
      paramValues.add(primaryKeyValue);

      // Execute update
      PreparedStatement pstmt = connection.prepareStatement(query.toString());
      for (int i = 0; i < paramValues.size(); i++) {
        pstmt.setObject(i + 1, paramValues.get(i));
      }

      int rowsAffected = pstmt.executeUpdate();

      if (rowsAffected > 0) {
        statusLabel.setText("Record updated successfully");
        loadTableData(); // Refresh table
      } else {
        statusLabel.setText("Failed to update record");
      }

    } catch (SQLException ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(
              this,
              "Error updating record: " + ex.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE
      );
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(
              this,
              "Invalid number format: " + ex.getMessage(),
              "Input Error",
              JOptionPane.ERROR_MESSAGE
      );
    } catch (IllegalArgumentException ex) {
      JOptionPane.showMessageDialog(
              this,
              ex.getMessage(),
              "Input Error",
              JOptionPane.ERROR_MESSAGE
      );
    }
  }

  /**
   * Delete the selected record.
   *
   * @param e The action event
   */
  private void deleteRecord(ActionEvent e) {
    int selectedRow = dataTable.getSelectedRow();
    if (selectedRow == -1) {
      JOptionPane.showMessageDialog(
              this,
              "Please select a record to delete",
              "Selection Required",
              JOptionPane.INFORMATION_MESSAGE
      );
      return;
    }

    if (primaryKeyColumn == null) {
      JOptionPane.showMessageDialog(
              this,
              "Cannot delete record: No primary key found for table",
              "Delete Error",
              JOptionPane.ERROR_MESSAGE
      );
      return;
    }

    // Confirm delete
    int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this record?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
    );

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    try {
      // Get primary key value
      int primaryKeyIndex = columnNames.indexOf(primaryKeyColumn);
      Object primaryKeyValue = dataTable.getValueAt(selectedRow, primaryKeyIndex);

      // Build delete query
      String query = "DELETE FROM " + currentTable + " WHERE " + primaryKeyColumn + " = ?";

      // Execute delete
      PreparedStatement pstmt = connection.prepareStatement(query);
      pstmt.setObject(1, primaryKeyValue);

      int rowsAffected = pstmt.executeUpdate();

      if (rowsAffected > 0) {
        statusLabel.setText("Record deleted successfully");
        clearFormFields();
        loadTableData(); // Refresh table
      } else {
        statusLabel.setText("Failed to delete record");
      }

    } catch (SQLException ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(
              this,
              "Error deleting record: " + ex.getMessage(),
              "Database Error",
              JOptionPane.ERROR_MESSAGE
      );
    }
  }
}