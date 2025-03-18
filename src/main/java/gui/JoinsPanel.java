package gui;

import database.DatabaseManager;
import database.JoinOperations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class JoinsPanel extends JPanel {
  private DatabaseManager dbManager;
  private JoinOperations joinOperations;

  private JComboBox<String> predefinedJoinsComboBox;
  private JButton executeJoinButton;
  private JTable resultTable;
  private DefaultTableModel tableModel;
  private JTextArea customJoinTextArea;
  private JButton executeCustomJoinButton;
  private JTabbedPane joinTabPane;

  public JoinsPanel(DatabaseManager databaseManager) {
    this.dbManager = DatabaseManager.getInstance();
    this.joinOperations = new JoinOperations();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    initializeUI();
  }

  private void initializeUI() {
    // Create tabbed pane for different join options
    joinTabPane = new JTabbedPane();

    // Create predefined joins panel
    JPanel predefinedJoinsPanel = createPredefinedJoinsPanel();
    joinTabPane.addTab("Predefined Joins", predefinedJoinsPanel);

    // Create custom join panel
    JPanel customJoinPanel = createCustomJoinPanel();
    joinTabPane.addTab("Custom Join", customJoinPanel);

    // Create table model and results table
    tableModel = new DefaultTableModel();
    resultTable = new JTable(tableModel);
    JScrollPane resultScrollPane = new JScrollPane(resultTable);

    // Add components to main panel
    add(joinTabPane, BorderLayout.NORTH);
    add(resultScrollPane, BorderLayout.CENTER);
  }

  private JPanel createPredefinedJoinsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create dropdown for predefined joins
    predefinedJoinsComboBox = new JComboBox<>();
    predefinedJoinsComboBox.setEnabled(false);

    // Create execute button
    executeJoinButton = new JButton("Execute Join");
    executeJoinButton.setEnabled(false);
    executeJoinButton.addActionListener(e -> executePredefinedJoin());

    // Create control panel
    JPanel controlPanel = new JPanel();
    controlPanel.add(new JLabel("Select Join: "));
    controlPanel.add(predefinedJoinsComboBox);
    controlPanel.add(executeJoinButton);

    // Add description area
    JTextArea descriptionArea = new JTextArea();
    descriptionArea.setEditable(false);
    descriptionArea.setRows(3);
    descriptionArea.setText("Predefined joins allow you to quickly retrieve related data across multiple tables.");
    descriptionArea.setLineWrap(true);
    descriptionArea.setWrapStyleWord(true);
    JScrollPane descScrollPane = new JScrollPane(descriptionArea);

    // Add components to panel
    panel.add(controlPanel, BorderLayout.NORTH);
    panel.add(descScrollPane, BorderLayout.CENTER);

    return panel;
  }

  private JPanel createCustomJoinPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create text area for custom join
    customJoinTextArea = new JTextArea();
    customJoinTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    customJoinTextArea.setRows(6);
    JScrollPane customJoinScrollPane = new JScrollPane(customJoinTextArea);

    // Example placeholder text
    customJoinTextArea.setText("-- Example JOIN query:\n" +
            "SELECT a.column1, b.column2\n" +
            "FROM table1 a\n" +
            "JOIN table2 b ON a.id = b.table1_id\n" +
            "WHERE a.condition = 'value'");

    // Create execute button
    executeCustomJoinButton = new JButton("Execute Custom Join");
    executeCustomJoinButton.setEnabled(false);
    executeCustomJoinButton.addActionListener(e -> executeCustomJoin());

    // Create button panel
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(executeCustomJoinButton);

    // Add components to panel
    panel.add(customJoinScrollPane, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    return panel;
  }

  private void executePredefinedJoin() {
    String selectedJoin = (String) predefinedJoinsComboBox.getSelectedItem();
    if (selectedJoin != null && !selectedJoin.isEmpty()) {
      joinOperations.executeJoin(selectedJoin, tableModel);
    } else {
      JOptionPane.showMessageDialog(this, "Please select a join from the dropdown",
              "Join Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void executeCustomJoin() {
    String customJoinQuery = customJoinTextArea.getText().trim();
    if (!customJoinQuery.isEmpty()) {
      joinOperations.executeCustomJoin(customJoinQuery, tableModel);
    } else {
      JOptionPane.showMessageDialog(this, "Please enter a valid JOIN query",
              "Join Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  // Called when the database connection is established
  public void onDatabaseConnected() {
    // Populate predefined joins dropdown
    String[] joinNames = joinOperations.getPredefinedJoinNames();
    predefinedJoinsComboBox.setModel(new DefaultComboBoxModel<>(joinNames));

    // Enable components
    predefinedJoinsComboBox.setEnabled(true);
    executeJoinButton.setEnabled(true);
    executeCustomJoinButton.setEnabled(true);
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