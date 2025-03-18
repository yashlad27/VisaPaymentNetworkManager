package gui;

import database.DatabaseManager;
import database.ViewOperations;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ViewsPanel extends JPanel {
  private DatabaseManager dbManager;
  private ViewOperations viewOperations;

  private JComboBox<String> viewsComboBox;
  private JButton executeViewButton;
  private JTable resultTable;
  private DefaultTableModel tableModel;
  private JTextArea viewDefinitionArea;
  private JTextArea createViewTextArea;
  private JButton createViewButton;
  private JButton dropViewButton;

  public ViewsPanel(DatabaseManager databaseManager) {
    this.dbManager = DatabaseManager.getInstance();
    this.viewOperations = new ViewOperations();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    initializeUI();
  }

  private void initializeUI() {
    // Create tabbed pane for different view operations
    JTabbedPane viewsTabPane = new JTabbedPane();

    // Create existing views panel
    JPanel existingViewsPanel = createExistingViewsPanel();
    viewsTabPane.addTab("Existing Views", existingViewsPanel);

    // Create view creation panel
    JPanel createViewPanel = createViewCreationPanel();
    viewsTabPane.addTab("Create View", createViewPanel);

    // Create table model and results table
    tableModel = new DefaultTableModel();
    resultTable = new JTable(tableModel);
    JScrollPane resultScrollPane = new JScrollPane(resultTable);

    // Add components to main panel
    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(viewsTabPane, BorderLayout.CENTER);

    // Add definition display area
    viewDefinitionArea = new JTextArea(5, 40);
    viewDefinitionArea.setEditable(false);
    viewDefinitionArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    JScrollPane definitionScrollPane = new JScrollPane(viewDefinitionArea);
    definitionScrollPane.setBorder(BorderFactory.createTitledBorder("View Definition"));

    topPanel.add(definitionScrollPane, BorderLayout.SOUTH);

    // Create split pane to show both controls and results
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, resultScrollPane);
    splitPane.setDividerLocation(300);

    add(splitPane, BorderLayout.CENTER);
  }

  private JPanel createExistingViewsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create dropdown for views
    viewsComboBox = new JComboBox<>();
    viewsComboBox.setEnabled(false);
    viewsComboBox.addActionListener(e -> showViewDefinition());

    // Create execute button
    executeViewButton = new JButton("Show View Data");
    executeViewButton.setEnabled(false);
    executeViewButton.addActionListener(e -> executeView());

    // Create drop view button
    dropViewButton = new JButton("Drop View");
    dropViewButton.setEnabled(false);
    dropViewButton.addActionListener(e -> dropView());

    // Create control panel
    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    controlPanel.add(new JLabel("Select View: "));
    controlPanel.add(viewsComboBox);
    controlPanel.add(executeViewButton);
    controlPanel.add(dropViewButton);

    // Add description area
    JTextArea descriptionArea = new JTextArea();
    descriptionArea.setEditable(false);
    descriptionArea.setRows(3);
    descriptionArea.setText("Views are saved queries that can be referenced like tables. " +
            "Select a view from the dropdown to see its definition and data.");
    descriptionArea.setLineWrap(true);
    descriptionArea.setWrapStyleWord(true);
    JScrollPane descScrollPane = new JScrollPane(descriptionArea);

    // Add components to panel
    panel.add(controlPanel, BorderLayout.NORTH);
    panel.add(descScrollPane, BorderLayout.CENTER);

    return panel;
  }

  private JPanel createViewCreationPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create text area for view creation SQL
    createViewTextArea = new JTextArea();
    createViewTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    createViewTextArea.setRows(8);

    // Example placeholder text
    createViewTextArea.setText("-- Example CREATE VIEW statement:\n" +
            "CREATE VIEW example_view AS\n" +
            "SELECT c.cardholder_id, c.first_name, c.last_name, COUNT(cd.card_id) AS card_count\n" +
            "FROM cardholders c\n" +
            "JOIN cards cd ON c.cardholder_id = cd.cardholder_id\n" +
            "GROUP BY c.cardholder_id, c.first_name, c.last_name");

    JScrollPane createViewScrollPane = new JScrollPane(createViewTextArea);

    // Create execute button
    createViewButton = new JButton("Create View");
    createViewButton.setEnabled(false);
    createViewButton.addActionListener(e -> createView());

    // Create button panel
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(createViewButton);

    // Add components to panel
    panel.add(createViewScrollPane, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    return panel;
  }

  private void showViewDefinition() {
    String selectedView = (String) viewsComboBox.getSelectedItem();
    if (selectedView != null && !selectedView.isEmpty()) {
      String definition = viewOperations.getViewDefinition(selectedView);
      viewDefinitionArea.setText(definition);
    }
  }

  private void executeView() {
    String selectedView = (String) viewsComboBox.getSelectedItem();
    if (selectedView != null && !selectedView.isEmpty()) {
      viewOperations.executeView(selectedView, tableModel);
    } else {
      JOptionPane.showMessageDialog(this, "Please select a view from the dropdown",
              "View Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void createView() {
    String createViewSQL = createViewTextArea.getText().trim();
    if (!createViewSQL.isEmpty()) {
      boolean success = viewOperations.createView(createViewSQL);
      if (success) {
        refreshViewsList();
        JOptionPane.showMessageDialog(this, "View created successfully",
                "Success", JOptionPane.INFORMATION_MESSAGE);
      }
    } else {
      JOptionPane.showMessageDialog(this, "Please enter a valid CREATE VIEW statement",
              "View Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void dropView() {
    String selectedView = (String) viewsComboBox.getSelectedItem();
    if (selectedView != null && !selectedView.isEmpty()) {
      // Confirm before dropping
      int result = JOptionPane.showConfirmDialog(this,
              "Are you sure you want to drop the view '" + selectedView + "'?",
              "Confirm Drop", JOptionPane.YES_NO_OPTION);

      if (result == JOptionPane.YES_OPTION) {
        boolean success = viewOperations.dropView(selectedView);
        if (success) {
          refreshViewsList();
          viewDefinitionArea.setText("");
          tableModel.setRowCount(0);
          tableModel.setColumnCount(0);
        }
      }
    } else {
      JOptionPane.showMessageDialog(this, "Please select a view to drop",
              "View Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void refreshViewsList() {
    viewsComboBox.removeAllItems();
    String[] viewNames = viewOperations.getViewList();
    for (String viewName : viewNames) {
      viewsComboBox.addItem(viewName);
    }
  }

  // Called when the database connection is established
  public void onDatabaseConnected() {
    // Enable buttons
    executeViewButton.setEnabled(true);
    dropViewButton.setEnabled(true);
    createViewButton.setEnabled(true);
    viewsComboBox.setEnabled(true);

    // Refresh views list
    refreshViewsList();
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