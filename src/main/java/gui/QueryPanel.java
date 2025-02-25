package gui;

import database.DatabaseManager;
import database.QueryExecutor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryPanel extends JPanel {
  private DatabaseManager dbManager;
  private QueryExecutor queryExecutor;

  private JTextArea queryTextArea;
  private JTable resultTable;
  private DefaultTableModel tableModel;
  private JButton executeButton;
  private JButton clearButton;

  public QueryPanel() {
    this.dbManager = DatabaseManager.getInstance();
    this.queryExecutor = new QueryExecutor();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    initializeUI();
  }

  private void initializeUI() {
    // Query input area
    queryTextArea = new JTextArea();
    queryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    JScrollPane queryScrollPane = new JScrollPane(queryTextArea);
    queryScrollPane.setPreferredSize(new Dimension(400, 150));

    // Button panel
    JPanel buttonPanel = new JPanel();
    executeButton = new JButton("Execute Query");
    clearButton = new JButton("Clear");

    executeButton.addActionListener(e -> executeQuery());
    clearButton.addActionListener(e -> queryTextArea.setText(""));

    // Initially disable the execute button until connected
    executeButton.setEnabled(false);
    clearButton.setEnabled(false);

    buttonPanel.add(executeButton);
    buttonPanel.add(clearButton);

    // Query panel north section
    JPanel queryNorthPanel = new JPanel(new BorderLayout());
    queryNorthPanel.add(queryScrollPane, BorderLayout.CENTER);
    queryNorthPanel.add(buttonPanel, BorderLayout.SOUTH);

    // Results table
    tableModel = new DefaultTableModel();
    resultTable = new JTable(tableModel);
    JScrollPane resultScrollPane = new JScrollPane(resultTable);

    // Add components to main panel
    add(queryNorthPanel, BorderLayout.NORTH);
    add(resultScrollPane, BorderLayout.CENTER);
  }

  private void executeQuery() {
    String query = queryTextArea.getText().trim();
    queryExecutor.executeQuery(query, tableModel);
  }

  // Enable buttons when database connection is established
  public void onDatabaseConnected() {
    executeButton.setEnabled(true);
    clearButton.setEnabled(true);
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