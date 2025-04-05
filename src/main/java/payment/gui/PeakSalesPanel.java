package payment.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import payment.database.DatabaseManager;

/**
 * Panel for analyzing peak sales times and transaction patterns.
 */
public class PeakSalesPanel extends JPanel {
  private final DatabaseManager dbManager;
  private final Connection connection;

  // UI Components
  private JComboBox<String> viewByComboBox;
  private JComboBox<String> timeframeComboBox;
  private JTable hourlyVolumeTable;
  private DefaultTableModel hourlyVolumeModel;
  private JTable weekdayVolumeTable;
  private DefaultTableModel weekdayVolumeModel;
  private TimeHeatmapPanel heatmapPanel;

  // Formatters
  private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

  // Auto-refresh timer
  private Timer refreshTimer;
  private final int REFRESH_INTERVAL = 60000; // 1 minute

  /**
   * Constructor for the peak sales analysis panel.
   *
   * @param dbManager The database manager
   */
  public PeakSalesPanel(DatabaseManager dbManager) {
    this.dbManager = dbManager;
    this.connection = dbManager.getConnection();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create UI Components
    initComponents();

    // Load initial data
    refreshData();

    // Set up auto-refresh timer
    setupRefreshTimer();
  }

  /**
   * Initialize UI components.
   */
  private void initComponents() {
    // Create title and control panel at the top
    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);

    // Create main content panel with split pane
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPane.setResizeWeight(0.5); // Equal resize weight

    // Create top panel with hourly volume
    JPanel hourlyPanel = createHourlyVolumePanel();

    // Create bottom panel with tabbed pane for weekday and heatmap
    JTabbedPane bottomTabs = new JTabbedPane();
    JPanel weekdayPanel = createWeekdayVolumePanel();
    bottomTabs.addTab("Weekday Analysis", weekdayPanel);

    // Create time heatmap panel using our new component
    boolean viewByCount = viewByComboBox.getSelectedIndex() == 0;
    heatmapPanel = new TimeHeatmapPanel(dbManager, viewByCount);
    bottomTabs.addTab("Time Heatmap", heatmapPanel);

    // Add panels to split pane
    splitPane.setTopComponent(hourlyPanel);
    splitPane.setBottomComponent(bottomTabs);

    // Add split pane to main panel with scroll capability
    add(new JScrollPane(splitPane), BorderLayout.CENTER);
  }

  /**
   * Create the header panel with title and controls.
   *
   * @return The header panel
   */
  private JPanel createHeaderPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(0, 0, 10, 0));

    // Title label
    JLabel titleLabel = new JLabel("Peak Sales Analysis");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
    panel.add(titleLabel, BorderLayout.WEST);

    // Controls panel
    JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    // View by combobox (count or value)
    String[] viewOptions = {"Transaction Count", "Transaction Value"};
    viewByComboBox = new JComboBox<>(viewOptions);
    viewByComboBox.setPreferredSize(new Dimension(150, 25));
    viewByComboBox.addActionListener(e -> {
      boolean viewByCount = viewByComboBox.getSelectedIndex() == 0;
      heatmapPanel.setViewByCount(viewByCount);
      refreshData();
    });

    // Timeframe combobox
    String[] timeframeOptions = {"Today", "Last 7 Days", "Last 30 Days", "All Time"};
    timeframeComboBox = new JComboBox<>(timeframeOptions);
    timeframeComboBox.setPreferredSize(new Dimension(120, 25));
    timeframeComboBox.addActionListener(e -> refreshData());

    // Refresh button
    JButton refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(this::refreshButtonClicked);

    controlsPanel.add(new JLabel("View by:"));
    controlsPanel.add(viewByComboBox);
    controlsPanel.add(new JLabel("Timeframe:"));
    controlsPanel.add(timeframeComboBox);
    controlsPanel.add(refreshButton);

    panel.add(controlsPanel, BorderLayout.EAST);

    return panel;
  }

  /**
   * Create the hourly volume panel.
   *
   * @return The hourly volume panel
   */
  private JPanel createHourlyVolumePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Hourly Transaction Volume",
            TitledBorder.LEFT,
            TitledBorder.TOP
    ));

    // Create table model and table
    String[] columns = {"Hour", "Transaction Count", "Total Value", "Average Value", "% of Daily Total"};
    hourlyVolumeModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Make all cells non-editable
      }
    };
    hourlyVolumeTable = new JTable(hourlyVolumeModel);
    hourlyVolumeTable.getTableHeader().setReorderingAllowed(false);

    // Custom renderer for hour column to show in 12-hour format
    hourlyVolumeTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (value instanceof Integer) {
          int hour = (Integer) value;
          String displayHour;
          if (hour == 0) {
            displayHour = "12 AM";
          } else if (hour < 12) {
            displayHour = hour + " AM";
          } else if (hour == 12) {
            displayHour = "12 PM";
          } else {
            displayHour = (hour - 12) + " PM";
          }
          setText(displayHour);
        }

        return c;
      }
    });

    // Add table to panel with scroll pane
    panel.add(new JScrollPane(hourlyVolumeTable), BorderLayout.CENTER);

    // Add description label
    JLabel descLabel = new JLabel("Distribution of transactions by hour of day");
    descLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
    descLabel.setForeground(Color.DARK_GRAY);
    panel.add(descLabel, BorderLayout.SOUTH);

    return panel;
  }

  /**
   * Create the weekday volume panel.
   *
   * @return The weekday volume panel
   */
  private JPanel createWeekdayVolumePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create table model and table
    String[] columns = {"Day of Week", "Transaction Count", "Total Value", "Average Value", "% of Weekly Total"};
    weekdayVolumeModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Make all cells non-editable
      }
    };
    weekdayVolumeTable = new JTable(weekdayVolumeModel);
    weekdayVolumeTable.getTableHeader().setReorderingAllowed(false);

    // Add description label
    JLabel descLabel = new JLabel("Distribution of transactions by day of week");
    descLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
    panel.add(descLabel, BorderLayout.NORTH);

    // Add table to panel with scroll pane
    panel.add(new JScrollPane(weekdayVolumeTable), BorderLayout.CENTER);

    return panel;
  }

  /**
   * Set up the refresh timer.
   */
  private void setupRefreshTimer() {
    refreshTimer = new Timer(REFRESH_INTERVAL, e -> refreshData());
    refreshTimer.start();
  }

  /**
   * Handle refresh button click.
   *
   * @param e The action event
   */
  private void refreshButtonClicked(ActionEvent e) {
    refreshData();
  }

  /**
   * Refresh all data from the database.
   */
  private void refreshData() {
    SwingWorker<Void, Void> worker = new SwingWorker<>() {
      @Override
      protected Void doInBackground() {
        try {
          updateHourlyVolume();
          updateWeekdayVolume();

          // Refresh the heatmap panel
          heatmapPanel.refreshData();

        } catch (SQLException ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(
                  PeakSalesPanel.this,
                  "Error refreshing data: " + ex.getMessage(),
                  "Database Error",
                  JOptionPane.ERROR_MESSAGE
          );
        }
        return null;
      }
    };
    worker.execute();
  }

  /**
   * Update hourly volume table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateHourlyVolume() throws SQLException {
    boolean viewByCount = viewByComboBox.getSelectedIndex() == 0;
    String timeframe = getTimeframeWhereClause();

    String query = "SELECT " +
            "    HOUR(timestamp) as hour_of_day, " +
            "    COUNT(*) as transaction_count, " +
            "    SUM(amount) as total_amount, " +
            "    ROUND(AVG(amount), 2) as avg_amount, " +
            "    ROUND((COUNT(*) / (SELECT COUNT(*) FROM Transaction" + timeframe + ")) * 100, 2) as percent_by_count, " +
            "    ROUND((SUM(amount) / (SELECT SUM(amount) FROM Transaction" + timeframe + ")) * 100, 2) as percent_by_value " +
            "FROM Transaction " +
            timeframe +
            "GROUP BY HOUR(timestamp) " +
            "ORDER BY hour_of_day";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      // Clear existing data
      SwingUtilities.invokeLater(() -> hourlyVolumeModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        final int hour = rs.getInt("hour_of_day");
        final int count = rs.getInt("transaction_count");
        final double amount = rs.getDouble("total_amount");
        final double avgAmount = rs.getDouble("avg_amount");
        final double percent = viewByCount ?
                rs.getDouble("percent_by_count") :
                rs.getDouble("percent_by_value");

        SwingUtilities.invokeLater(() ->
                hourlyVolumeModel.addRow(new Object[]{
                        hour,
                        String.format("%,d", count),
                        currencyFormat.format(amount),
                        currencyFormat.format(avgAmount),
                        String.format("%.2f%%", percent)
                })
        );
      }
    }
  }

  /**
   * Update weekday volume table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateWeekdayVolume() throws SQLException {
    boolean viewByCount = viewByComboBox.getSelectedIndex() == 0;
    String timeframe = getTimeframeWhereClause();

    String query = "SELECT " +
            "    DAYNAME(timestamp) as day_of_week, " +
            "    COUNT(*) as transaction_count, " +
            "    SUM(amount) as total_amount, " +
            "    ROUND(AVG(amount), 2) as avg_amount, " +
            "    ROUND((COUNT(*) / (SELECT COUNT(*) FROM Transaction" + timeframe + ")) * 100, 2) as percent_by_count, " +
            "    ROUND((SUM(amount) / (SELECT SUM(amount) FROM Transaction" + timeframe + ")) * 100, 2) as percent_by_value " +
            "FROM Transaction " +
            timeframe +
            "GROUP BY DAYNAME(timestamp) " +
            "ORDER BY FIELD(day_of_week, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      // Clear existing data
      SwingUtilities.invokeLater(() -> weekdayVolumeModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        final String day = rs.getString("day_of_week");
        final int count = rs.getInt("transaction_count");
        final double amount = rs.getDouble("total_amount");
        final double avgAmount = rs.getDouble("avg_amount");
        final double percent = viewByCount ?
                rs.getDouble("percent_by_count") :
                rs.getDouble("percent_by_value");

        SwingUtilities.invokeLater(() ->
                weekdayVolumeModel.addRow(new Object[]{
                        day,
                        String.format("%,d", count),
                        currencyFormat.format(amount),
                        currencyFormat.format(avgAmount),
                        String.format("%.2f%%", percent)
                })
        );
      }
    }
  }

  /**
   * Get the WHERE clause for the selected timeframe.
   *
   * @return The WHERE clause for the timeframe
   */
  private String getTimeframeWhereClause() {
    String clause;
    int selectedIndex = timeframeComboBox.getSelectedIndex();

    switch (selectedIndex) {
      case 0: // Today
        clause = " WHERE DATE(timestamp) = CURDATE() ";
        break;
      case 1: // Last 7 Days
        clause = " WHERE timestamp >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) ";
        break;
      case 2: // Last 30 Days
        clause = " WHERE timestamp >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) ";
        break;
      default: // All Time
        clause = " ";
        break;
    }

    return clause;
  }
}