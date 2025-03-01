package gui;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import database.CRUDOperations;
import database.DatabaseManager;

public class CRUDPanel extends JPanel {
  private DatabaseManager dbManager;
  private CRUDOperations crudOperations;

  private JComboBox<String> tableComboBox;
  private JTable dataTable;
  private DefaultTableModel tableModel;
  private JButton refreshButton;
  private JButton addButton;
  private JButton editButton;
  private JButton deleteButton;

  private JTabbedPane operationTabs;
  private JPanel viewPanel;
  private JPanel insertPanel;
  private JPanel updatePanel;
  private JPanel deletePanel;

  // Fields to store form components for insert/update operations
  private Map<String, JTextField> insertFields = new HashMap<>();
  private Map<String, JTextField> updateFields = new HashMap<>();
  private JComboBox<String> primaryKeySelector;

  public CRUDPanel() {
    this.dbManager = DatabaseManager.getInstance();
    this.crudOperations = new CRUDOperations();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    initializeUI();
  }

  private void initializeUI() {
    // Create top control panel
    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    controlPanel.add(new JLabel("Select Table: "));
    tableComboBox = new JComboBox<>();
    tableComboBox.setEnabled(false);
    tableComboBox.addActionListener(e -> loadTableData());
    controlPanel.add(tableComboBox);

    refreshButton = new JButton("Refresh");
    refreshButton.setEnabled(false);
    refreshButton.addActionListener(e -> loadTableData());
    controlPanel.add(refreshButton);

    // Create table model and data table
    tableModel = new DefaultTableModel();
    dataTable = new JTable(tableModel);
    JScrollPane tableScrollPane = new JScrollPane(dataTable);
    tableScrollPane.setBorder(new TitledBorder("Table Data"));

    // Create operation tabs
    operationTabs = new JTabbedPane();

    // View panel (just shows the data)
    viewPanel = new JPanel(new BorderLayout());
    viewPanel.add(tableScrollPane);
    operationTabs.addTab("View", viewPanel);

    // Insert panel
    insertPanel = new JPanel(new BorderLayout());
    JPanel insertFormPanel = new JPanel();
    insertFormPanel.setLayout(new BoxLayout(insertFormPanel, BoxLayout.Y_AXIS));
    JScrollPane insertScrollPane = new JScrollPane(insertFormPanel);
    addButton = new JButton("Add Record");
    addButton.setEnabled(false);
    addButton.addActionListener(e -> insertRecord());
    JPanel insertButtonPanel = new JPanel();
    insertButtonPanel.add(addButton);
    insertPanel.add(insertScrollPane, BorderLayout.CENTER);
    insertPanel.add(insertButtonPanel, BorderLayout.SOUTH);
    operationTabs.addTab("Insert", insertPanel);

    // Update panel
    updatePanel = new JPanel(new BorderLayout());
    JPanel updateFormPanel = new JPanel();
    updateFormPanel.setLayout(new BoxLayout(updateFormPanel, BoxLayout.Y_AXIS));
    JScrollPane updateScrollPane = new JScrollPane(updateFormPanel);
    JPanel updateSelectionPanel = new JPanel();
    updateSelectionPanel.add(new JLabel("Select Primary Key: "));
    primaryKeySelector = new JComboBox<>();
    primaryKeySelector.setEnabled(false);
    updateSelectionPanel.add(primaryKeySelector);
    JButton loadRecordButton = new JButton("Load Record");
    loadRecordButton.setEnabled(false);
    loadRecordButton.addActionListener(e -> loadRecordForUpdate());
    updateSelectionPanel.add(loadRecordButton);
    editButton = new JButton("Update Record");
    editButton.setEnabled(false);
    editButton.addActionListener(e -> updateRecord());
    JPanel updateButtonPanel = new JPanel();
    updateButtonPanel.add(editButton);
    updatePanel.add(updateSelectionPanel, BorderLayout.NORTH);
    updatePanel.add(updateScrollPane, BorderLayout.CENTER);
    updatePanel.add(updateButtonPanel, BorderLayout.SOUTH);
    operationTabs.addTab("Update", updatePanel);

    // Delete panel
    deletePanel = new JPanel(new BorderLayout());
    JPanel deleteControlPanel = new JPanel();
    deleteControlPanel.add(new JLabel("Select Record to Delete (Click in table)"));
    deleteButton = new JButton("Delete Selected Record");
    deleteButton.setEnabled(false);
    deleteButton.addActionListener(e -> deleteRecord());
    JPanel deleteButtonPanel = new JPanel();
    deleteButtonPanel.add(deleteButton);
    deletePanel.add(deleteControlPanel, BorderLayout.NORTH);
    deletePanel.add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);
    deletePanel.add(deleteButtonPanel, BorderLayout.SOUTH);
    operationTabs.addTab("Delete", deletePanel);

    // Add all components to main panel
    add(controlPanel, BorderLayout.NORTH);
    add(operationTabs, BorderLayout.CENTER);
  }

  private void loadTableData() {
    String selectedTable = (String) tableComboBox.getSelectedItem();
    if (selectedTable != null && !selectedTable.isEmpty()) {
      crudOperations.loadTableData(selectedTable, tableModel);

      // Update primary key selector for update tab
      updatePrimaryKeySelector(selectedTable);

      // Update insert and update forms
      updateInsertForm(selectedTable);
      updateUpdateForm(selectedTable);

      // Enable buttons
      addButton.setEnabled(true);
      primaryKeySelector.setEnabled(true);
      deleteButton.setEnabled(true);
    }
  }

  private void updatePrimaryKeySelector(String tableName) {
    primaryKeySelector.removeAllItems();

    try {
      // Load primary key column
      String primaryKeyColumn = crudOperations.getPrimaryKeyColumn(tableName);

      // Load primary key values from the table
      for (int i = 0; i < tableModel.getRowCount(); i++) {
        int pkColumnIndex = -1;

        // Find the index of primary key column
        for (int j = 0; j < tableModel.getColumnCount(); j++) {
          if (tableModel.getColumnName(j).equals(primaryKeyColumn)) {
            pkColumnIndex = j;
            break;
          }
        }

        if (pkColumnIndex != -1) {
          primaryKeySelector.addItem(tableModel.getValueAt(i, pkColumnIndex).toString());
        }
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Error loading primary keys: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void updateInsertForm(String tableName) {
    // Get the panel inside the scroll pane
    JPanel formPanel = (JPanel) ((JScrollPane) insertPanel.getComponent(0)).getViewport().getView();
    formPanel.removeAll();
    insertFields.clear();

    Vector<String> columnNames = crudOperations.getColumnNames(tableName, true);

    for (String columnName : columnNames) {
      JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      fieldPanel.add(new JLabel(columnName + ":"));
      JTextField textField = new JTextField(20);
      fieldPanel.add(textField);
      formPanel.add(fieldPanel);

      insertFields.put(columnName, textField);
    }

    formPanel.revalidate();
    formPanel.repaint();
  }

  private void updateUpdateForm(String tableName) {
    // Get the panel inside the scroll pane
    JPanel formPanel = (JPanel) ((JScrollPane) updatePanel.getComponent(1)).getViewport().getView();
    formPanel.removeAll();
    updateFields.clear();

    Vector<String> columnNames = crudOperations.getColumnNames(tableName, false);

    for (String columnName : columnNames) {
      JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      fieldPanel.add(new JLabel(columnName + ":"));
      JTextField textField = new JTextField(20);
      fieldPanel.add(textField);
      formPanel.add(fieldPanel);

      updateFields.put(columnName, textField);
    }

    formPanel.revalidate();
    formPanel.repaint();
  }

  private void loadRecordForUpdate() {
    String selectedTable = (String) tableComboBox.getSelectedItem();
    String primaryKeyValue = (String) primaryKeySelector.getSelectedItem();
    String primaryKeyColumn = crudOperations.getPrimaryKeyColumn(selectedTable);

    if (selectedTable == null || primaryKeyValue == null || primaryKeyColumn == null) {
      JOptionPane.showMessageDialog(this, "Please select a valid table and primary key",
              "Update Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      // Execute a query to get the selected record
      String query = "SELECT * FROM " + selectedTable + " WHERE " + primaryKeyColumn + " = '" + primaryKeyValue + "'";
      java.sql.ResultSet resultSet = dbManager.executeQuery(query);

      if (resultSet.next()) {
        // Populate the update form fields
        for (String columnName : updateFields.keySet()) {
          JTextField field = updateFields.get(columnName);
          field.setText(resultSet.getString(columnName));
        }

        // Enable the update button
        editButton.setEnabled(true);
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Error loading record: " + e.getMessage(),
              "Database Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void insertRecord() {
    String selectedTable = (String) tableComboBox.getSelectedItem();

    if (selectedTable == null || selectedTable.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Please select a table first",
              "Insert Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Collect field values
    Vector<String> columnNames = new Vector<>();
    Vector<String> values = new Vector<>();

    for (String columnName : insertFields.keySet()) {
      String value = insertFields.get(columnName).getText().trim();
      if (!value.isEmpty()) {
        columnNames.add(columnName);
        values.add(value);
      }
    }

    if (columnNames.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Please fill at least one field",
              "Insert Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Insert the record
    crudOperations.insertRecord(selectedTable, columnNames, values);

    // Refresh the table data
    loadTableData();

    // Clear form fields
    for (JTextField field : insertFields.values()) {
      field.setText("");
    }
  }

  private void updateRecord() {
    String selectedTable = (String) tableComboBox.getSelectedItem();
    String primaryKeyValue = (String) primaryKeySelector.getSelectedItem();
    String primaryKeyColumn = crudOperations.getPrimaryKeyColumn(selectedTable);

    if (selectedTable == null || primaryKeyValue == null || primaryKeyColumn == null) {
      JOptionPane.showMessageDialog(this, "Please select a valid table and primary key",
              "Update Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Collect field values
    Vector<String> columnNames = new Vector<>();
    Vector<String> values = new Vector<>();

    for (String columnName : updateFields.keySet()) {
      // Skip the primary key column for the SET clause
      if (!columnName.equals(primaryKeyColumn)) {
        String value = updateFields.get(columnName).getText().trim();
        columnNames.add(columnName);
        values.add(value);
      }
    }

    if (columnNames.isEmpty()) {
      JOptionPane.showMessageDialog(this, "No fields to update",
              "Update Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Update the record
    crudOperations.updateRecord(selectedTable, primaryKeyColumn, primaryKeyValue, columnNames, values);

    // Refresh the table data
    loadTableData();
  }

  private void deleteRecord() {
    String selectedTable = (String) tableComboBox.getSelectedItem();

    if (selectedTable == null || selectedTable.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Please select a table first",
              "Delete Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    int selectedRow = dataTable.getSelectedRow();
    if (selectedRow == -1) {
      JOptionPane.showMessageDialog(this, "Please select a row to delete",
              "Delete Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Get primary key column and value
    String primaryKeyColumn = crudOperations.getPrimaryKeyColumn(selectedTable);
    int primaryKeyIndex = -1;

    for (int i = 0; i < tableModel.getColumnCount(); i++) {
      if (tableModel.getColumnName(i).equals(primaryKeyColumn)) {
        primaryKeyIndex = i;
        break;
      }
    }

    if (primaryKeyIndex == -1) {
      JOptionPane.showMessageDialog(this, "Could not find primary key column",
              "Delete Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    String primaryKeyValue = tableModel.getValueAt(selectedRow, primaryKeyIndex).toString();

    // Confirm deletion
    int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete the record with " + primaryKeyColumn + " = " + primaryKeyValue + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
      // Delete the record
      crudOperations.deleteRecord(selectedTable, primaryKeyColumn, primaryKeyValue);

      // Refresh the table data
      loadTableData();
    }
  }

  // Called when the database connection is established
  public void onDatabaseConnected() {
    // Populate table dropdown
    String[] tableNames = crudOperations.getTableNames();
    tableComboBox.setModel(new DefaultComboBoxModel<>(tableNames));

    // Enable controls
    tableComboBox.setEnabled(true);
    refreshButton.setEnabled(true);

    // Load initial data if there are tables
    if (tableNames.length > 0) {
      loadTableData();
    }
  }
}