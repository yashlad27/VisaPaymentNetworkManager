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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import payment.database.DatabaseManager;

/**
 * Panel for analyzing peak sales times and transaction patterns.
 * <p>
 * This panel provides comprehensive visualizations and analytics for identifying
 * peak transaction periods throughout the day and week. It offers interactive
 * charts, heatmaps, and data tables that help users identify patterns in transaction
 * volumes and values.
 * </p>
 * <p>
 * Features include:
 * <ul>
 *   <li>Hourly transaction analysis with pie and bar charts</li>
 *   <li>Day-of-week transaction patterns</li>
 *   <li>Time-based heatmap visualization</li>
 *   <li>Detailed data tables with metrics</li>
 *   <li>Filtering by transaction count or value</li>
 *   <li>Multiple timeframe options (Today, Last 7 Days, Last 30 Days, All Time)</li>
 *   <li>Automatic data refresh</li>
 * </ul>
 * </p>
 * 
 */
public class PeakSalesPanel extends JPanel {
    /** Database manager instance for database connectivity */
    private final DatabaseManager dbManager;
    
    /** Database connection for executing SQL queries */
    private final Connection connection;

    // UI Components
    /** Dropdown for selecting view mode (count or value) */
    private JComboBox<String> viewByComboBox;
    
    /** Dropdown for selecting timeframe for analysis */
    private JComboBox<String> timeframeComboBox;
    
    /** Table for displaying hourly transaction data */
    private JTable hourlyVolumeTable;
    
    /** Model for hourly transaction table */
    private DefaultTableModel hourlyVolumeModel;
    
    /** Table for displaying weekday transaction data */
    private JTable weekdayVolumeTable;
    
    /** Model for weekday transaction table */
    private DefaultTableModel weekdayVolumeModel;
    
    /** Panel for displaying time-based heatmap */
    private TimeHeatmapPanel heatmapPanel;
    
    // Chart Components
    /** Chart panel for hourly transaction pie chart */
    private ChartPanel hourlyPieChart;
    
    /** Chart panel for hourly transaction bar chart */
    private ChartPanel hourlyBarChart;
    
    /** Chart panel for weekday transaction pie chart */
    private ChartPanel weekdayPieChart;
    
    /** Chart panel for weekday transaction bar chart */
    private ChartPanel weekdayBarChart;

    // Formatters
    /** Formatter for currency values */
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    // Auto-refresh timer
    /** Timer for automatic data refresh */
    private Timer refreshTimer;
    
    /** Interval for automatic data refresh (in milliseconds) */
    private final int REFRESH_INTERVAL = 60000; // 1 minute

    /**
     * Constructor for the peak sales analysis panel.
     * <p>
     * Initializes the panel with database connection, UI components, and initial data load.
     * Sets up auto-refresh timer to keep data current.
     * </p>
     *
     * @param dbManager The database manager instance for database connectivity
     * @throws IllegalArgumentException if dbManager is null
     */
    public PeakSalesPanel(DatabaseManager dbManager) {
        if (dbManager == null) {
            throw new IllegalArgumentException("Database manager cannot be null");
        }
        
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
     * Initialize UI components for the panel.
     * <p>
     * Creates and lays out all UI components including:
     * <ul>
     *   <li>Header panel with controls</li>
     *   <li>Hourly volume panel with charts and table</li>
     *   <li>Weekday analysis panel with charts and table</li>
     *   <li>Time heatmap panel</li>
     * </ul>
     * </p>
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
     * <p>
     * This panel includes:
     * <ul>
     *   <li>Title label</li>
     *   <li>View by dropdown (count or value)</li>
     *   <li>Timeframe dropdown</li>
     *   <li>Refresh button</li>
     * </ul>
     * </p>
     *
     * @return The fully configured header panel
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
     * Create the hourly volume panel with charts and table.
     * <p>
     * This panel includes:
     * <ul>
     *   <li>Pie chart showing distribution by hour</li>
     *   <li>Bar chart showing volumes by hour</li>
     *   <li>Detailed data table with metrics</li>
     * </ul>
     * </p>
     *
     * @return The fully configured hourly volume panel
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

        // Create a panel for the table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(new JScrollPane(hourlyVolumeTable), BorderLayout.CENTER);
        
        // Create a panel for the charts
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // Create pie chart
        hourlyPieChart = new ChartPanel(null);
        hourlyPieChart.setPreferredSize(new Dimension(300, 250));
        
        // Create bar chart
        hourlyBarChart = new ChartPanel(null);
        hourlyBarChart.setPreferredSize(new Dimension(300, 250));
        
        chartsPanel.add(hourlyPieChart);
        chartsPanel.add(hourlyBarChart);
        
        // Create split pane to hold both table and charts
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(chartsPanel);
        splitPane.setBottomComponent(tablePanel);
        splitPane.setResizeWeight(0.6); // 60% charts, 40% table
        
        // Add to panel
        panel.add(splitPane, BorderLayout.CENTER);

        // Add description label
        JLabel descLabel = new JLabel("Distribution of transactions by hour of day");
        descLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        descLabel.setForeground(Color.DARK_GRAY);
        panel.add(descLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create the weekday volume panel with charts and table.
     * <p>
     * This panel includes:
     * <ul>
     *   <li>Pie chart showing distribution by day of week</li>
     *   <li>Bar chart showing volumes by day of week</li>
     *   <li>Detailed data table with metrics</li>
     * </ul>
     * </p>
     *
     * @return The fully configured weekday volume panel
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
        
        // Create a panel for the table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(new JScrollPane(weekdayVolumeTable), BorderLayout.CENTER);
        
        // Create a panel for the charts
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // Create pie chart
        weekdayPieChart = new ChartPanel(null);
        weekdayPieChart.setPreferredSize(new Dimension(300, 250));
        
        // Create bar chart
        weekdayBarChart = new ChartPanel(null);
        weekdayBarChart.setPreferredSize(new Dimension(300, 250));
        
        chartsPanel.add(weekdayPieChart);
        chartsPanel.add(weekdayBarChart);
        
        // Create split pane to hold both table and charts
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(chartsPanel);
        splitPane.setBottomComponent(tablePanel);
        splitPane.setResizeWeight(0.6); // 60% charts, 40% table

        // Add description label
        JLabel descLabel = new JLabel("Distribution of transactions by day of week");
        descLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        panel.add(descLabel, BorderLayout.NORTH);

        // Add split pane to panel
        panel.add(splitPane, BorderLayout.CENTER);

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
     * Handle refresh button click action.
     * <p>
     * Triggered when the user clicks the refresh button, this method
     * initiates a manual data refresh.
     * </p>
     *
     * @param e The action event from the button click
     */
    private void refreshButtonClicked(ActionEvent e) {
        refreshData();
    }

    /**
     * Refresh all data from the database.
     * <p>
     * Uses SwingWorker to perform database operations in a background thread
     * to prevent UI freezing. Updates all charts, tables, and the heatmap
     * with fresh data based on the current filter settings.
     * </p>
     */
    private void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    updateHourlyVolume();
                    updateWeekdayVolume();
                    updateHourlyCharts();
                    updateWeekdayCharts();

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
     * Update hourly volume table with data from the database.
     * <p>
     * Retrieves hourly transaction data based on current view mode and timeframe,
     * and updates the hourly volume table with the results.
     * </p>
     *
     * @throws SQLException If a database error occurs during query execution
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
     * Update weekday volume table with data from the database.
     * <p>
     * Retrieves transaction data grouped by day of week based on current view mode
     * and timeframe, and updates the weekday volume table with the results.
     * </p>
     *
     * @throws SQLException If a database error occurs during query execution
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
     * Update hourly charts with data from the database.
     * <p>
     * Creates pie and bar charts showing transaction distribution by hour based
     * on the current view mode (count or value) and timeframe. Updates the chart
     * panels with the newly created charts.
     * </p>
     * 
     * @throws SQLException If a database error occurs during query execution
     */
    private void updateHourlyCharts() throws SQLException {
        boolean viewByCount = viewByComboBox.getSelectedIndex() == 0;
        String timeframe = getTimeframeWhereClause();
        
        String query = "SELECT " +
                "    HOUR(timestamp) as hour_of_day, " +
                "    COUNT(*) as transaction_count, " +
                "    SUM(amount) as total_amount " +
                "FROM Transaction " +
                timeframe +
                "GROUP BY HOUR(timestamp) " +
                "ORDER BY hour_of_day";
        
        // Create datasets for pie and bar charts
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                int hour = rs.getInt("hour_of_day");
                int count = rs.getInt("transaction_count");
                double amount = rs.getDouble("total_amount");
                
                // Format hour label
                String hourLabel;
                if (hour == 0) {
                    hourLabel = "12 AM";
                } else if (hour < 12) {
                    hourLabel = hour + " AM";
                } else if (hour == 12) {
                    hourLabel = "12 PM";
                } else {
                    hourLabel = (hour - 12) + " PM";
                }
                
                // Add data to datasets based on view mode
                if (viewByCount) {
                    pieDataset.setValue(hourLabel, count);
                    barDataset.addValue(count, "Transactions", hourLabel);
                } else {
                    pieDataset.setValue(hourLabel, amount);
                    barDataset.addValue(amount, "Amount ($)", hourLabel);
                }
            }
        }
        
        // Create charts
        final JFreeChart pieChart = ChartFactory.createPieChart(
                viewByCount ? "Transactions by Hour" : "Transaction Value by Hour",
                pieDataset,
                true, // include legend
                true, // tooltips
                false // URLs
        );
        
        // Customize pie chart
        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setLabelGenerator(null); // Remove labels from pie slices
        
        final JFreeChart barChart = ChartFactory.createBarChart(
                viewByCount ? "Transactions by Hour" : "Transaction Value by Hour",
                "Hour",
                viewByCount ? "Number of Transactions" : "Transaction Value ($)",
                barDataset,
                PlotOrientation.VERTICAL,
                true, // include legend
                true, // tooltips
                false // URLs
        );
        
        // Customize bar chart
        CategoryPlot barPlot = barChart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) barPlot.getRenderer();
        renderer.setSeriesPaint(0, new Color(41, 128, 185)); // Blue bars
        
        // Update chart panels on EDT
        SwingUtilities.invokeLater(() -> {
            hourlyPieChart.setChart(pieChart);
            hourlyBarChart.setChart(barChart);
        });
    }
    
    /**
     * Update weekday charts with data from the database.
     * <p>
     * Creates pie and bar charts showing transaction distribution by day of week
     * based on the current view mode (count or value) and timeframe. Updates
     * the chart panels with the newly created charts.
     * </p>
     * 
     * @throws SQLException If a database error occurs during query execution
     */
    private void updateWeekdayCharts() throws SQLException {
        boolean viewByCount = viewByComboBox.getSelectedIndex() == 0;
        String timeframe = getTimeframeWhereClause();
        
        String query = "SELECT " +
                "    DAYNAME(timestamp) as day_of_week, " +
                "    COUNT(*) as transaction_count, " +
                "    SUM(amount) as total_amount " +
                "FROM Transaction " +
                timeframe +
                "GROUP BY DAYNAME(timestamp) " +
                "ORDER BY FIELD(day_of_week, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')";
        
        // Create datasets for pie and bar charts
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String day = rs.getString("day_of_week");
                int count = rs.getInt("transaction_count");
                double amount = rs.getDouble("total_amount");
                
                // Add data to datasets based on view mode
                if (viewByCount) {
                    pieDataset.setValue(day, count);
                    barDataset.addValue(count, "Transactions", day);
                } else {
                    pieDataset.setValue(day, amount);
                    barDataset.addValue(amount, "Amount ($)", day);
                }
            }
        }
        
        // Create charts
        final JFreeChart pieChart = ChartFactory.createPieChart(
                viewByCount ? "Transactions by Day" : "Transaction Value by Day",
                pieDataset,
                true, // include legend
                true, // tooltips
                false // URLs
        );
        
        // Customize pie chart
        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setLabelGenerator(null); // Remove labels from pie slices
        
        final JFreeChart barChart = ChartFactory.createBarChart(
                viewByCount ? "Transactions by Day" : "Transaction Value by Day",
                "Day of Week",
                viewByCount ? "Number of Transactions" : "Transaction Value ($)",
                barDataset,
                PlotOrientation.VERTICAL,
                true, // include legend
                true, // tooltips
                false // URLs
        );
        
        // Customize bar chart
        CategoryPlot barPlot = barChart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) barPlot.getRenderer();
        renderer.setSeriesPaint(0, new Color(46, 204, 113)); // Green bars
        
        // Update chart panels on EDT
        SwingUtilities.invokeLater(() -> {
            weekdayPieChart.setChart(pieChart);
            weekdayBarChart.setChart(barChart);
        });
    }

    /**
     * Get the WHERE clause for the selected timeframe.
     * <p>
     * Constructs an SQL WHERE clause based on the timeframe selected in the UI.
     * Supports Today, Last 7 Days, Last 30 Days, and All Time options.
     * </p>
     *
     * @return The SQL WHERE clause string for the selected timeframe
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