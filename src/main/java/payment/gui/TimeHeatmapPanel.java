package payment.gui;

import payment.database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel for displaying a time-based heatmap of transaction volume.
 */
public class TimeHeatmapPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final Connection connection;
    private boolean viewByCount;

    // UI Components
    private JPanel heatmapPanel;
    private JLabel legendLabel;

    // Color scale for heatmap
    private final Color MIN_COLOR = new Color(220, 237, 255); // Light blue
    private final Color MAX_COLOR = new Color(0, 53, 128);    // Dark blue

    /**
     * Constructor for the time heatmap panel.
     *
     * @param dbManager   The database manager
     * @param viewByCount Whether to view by count (true) or value (false)
     */
    public TimeHeatmapPanel(DatabaseManager dbManager, boolean viewByCount) {
        this.dbManager = dbManager;
        this.connection = dbManager.getConnection();
        this.viewByCount = viewByCount;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create UI Components
        initComponents();

        // Load initial data
        refreshData();
    }

    /**
     * Initialize UI components.
     */
    private void initComponents() {
        // Add description label
        JLabel descLabel = new JLabel("Heatmap showing transaction density by day of week and hour");
        descLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        add(descLabel, BorderLayout.NORTH);

        // Create heatmap panel
        heatmapPanel = new JPanel(new GridLayout(8, 25));
        add(new JScrollPane(heatmapPanel), BorderLayout.CENTER);

        // Create legend panel for color scale
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        legendPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        legendPanel.add(new JLabel("Low"));

        JPanel colorScale = new JPanel(new GridLayout(1, 10));
        for (int i = 0; i < 10; i++) {
            float ratio = i / 9.0f;
            Color color = getColor(ratio);
            JPanel colorBox = new JPanel();
            colorBox.setBackground(color);
            colorBox.setPreferredSize(new Dimension(20, 20));
            colorBox.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            colorScale.add(colorBox);
        }

        legendPanel.add(colorScale);
        legendPanel.add(new JLabel("High"));

        // Add transaction count/value label
        legendLabel = new JLabel(viewByCount ? "Transaction Count" : "Transaction Value ($)");
        legendPanel.add(Box.createHorizontalStrut(20));
        legendPanel.add(legendLabel);

        add(legendPanel, BorderLayout.SOUTH);
    }

    /**
     * Set whether to view by count or value.
     *
     * @param viewByCount True to view by count, false to view by value
     */
    public void setViewByCount(boolean viewByCount) {
        this.viewByCount = viewByCount;
        legendLabel.setText(viewByCount ? "Transaction Count" : "Transaction Value ($)");
        refreshData();
    }

    /**
     * Refresh heatmap data from the database.
     */
    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    updateHeatmap();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            TimeHeatmapPanel.this,
                            "Error refreshing heatmap data: " + ex.getMessage(),
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
     * Update the heatmap with data from the database.
     *
     * @throws SQLException If a database error occurs
     */
    private void updateHeatmap() throws SQLException {
        // Query for transaction data by day of week and hour
        String query = "SELECT " +
                "    DAYOFWEEK(timestamp) as day_num, " +
                "    HOUR(timestamp) as hour_num, " +
                "    COUNT(*) as transaction_count, " +
                "    SUM(amount) as total_amount " +
                "FROM Transaction " +
                "GROUP BY DAYOFWEEK(timestamp), HOUR(timestamp) " +
                "ORDER BY DAYOFWEEK(timestamp), HOUR(timestamp)";

        // Store results in a map for easy lookup: key is "day_hour", value is count or amount
        Map<String, Double> dataMap = new HashMap<>();
        double maxValue = 0;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int dayNum = rs.getInt("day_num");
                int hourNum = rs.getInt("hour_num");
                double value = viewByCount ?
                        rs.getInt("transaction_count") :
                        rs.getDouble("total_amount");

                String key = dayNum + "_" + hourNum;
                dataMap.put(key, value);

                if (value > maxValue) {
                    maxValue = value;
                }
            }
        }

        // Final data and maxValue for use in SwingUtilities.invokeLater
        final Map<String, Double> finalDataMap = dataMap;
        final double finalMaxValue = maxValue;

        // Update UI on EDT
        SwingUtilities.invokeLater(() -> {
            heatmapPanel.removeAll();

            // Add hour labels at the top
            heatmapPanel.add(new JLabel(""));  // Empty corner cell
            for (int h = 0; h < 24; h++) {
                JLabel hourLabel = new JLabel(getHourLabel(h), SwingConstants.CENTER);
                hourLabel.setFont(new Font(hourLabel.getFont().getName(), Font.PLAIN, 10));
                heatmapPanel.add(hourLabel);
            }

            // Add rows for each day of week
            String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (int d = 1; d <= 7; d++) {
                // Add day label
                JLabel dayLabel = new JLabel(days[d - 1], SwingConstants.RIGHT);
                dayLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
                heatmapPanel.add(dayLabel);

                // Add cells for each hour of the day
                for (int h = 0; h < 24; h++) {
                    String key = d + "_" + h;
                    double value = finalDataMap.getOrDefault(key, 0.0);

                    // Create cell panel with color based on value
                    JPanel cell = new JPanel(new BorderLayout());
                    cell.setBorder(BorderFactory.createLineBorder(Color.GRAY));

                    // Calculate color based on value relative to max
                    float ratio = finalMaxValue > 0 ? (float) (value / finalMaxValue) : 0;
                    cell.setBackground(getColor(ratio));

                    // Add value label if there's data
                    if (value > 0) {
                        JLabel valueLabel = new JLabel(formatValue(value), SwingConstants.CENTER);
                        valueLabel.setFont(new Font(valueLabel.getFont().getName(), Font.PLAIN, 9));
                        valueLabel.setForeground(ratio > 0.5 ? Color.WHITE : Color.BLACK);
                        cell.add(valueLabel, BorderLayout.CENTER);
                    }

                    heatmapPanel.add(cell);
                }
            }

            heatmapPanel.revalidate();
            heatmapPanel.repaint();
        });
    }

    /**
     * Format a value for display in the heatmap.
     *
     * @param value The value to format
     * @return The formatted value as a string
     */
    private String formatValue(double value) {
        if (viewByCount) {
            return String.format("%.0f", value);
        } else {
            if (value >= 1000) {
                return String.format("%.1fK", value / 1000);
            } else {
                return String.format("%.0f", value);
            }
        }
    }

    /**
     * Get a label for an hour of the day.
     *
     * @param hour The hour (0-23)
     * @return The formatted hour label
     */
    private String getHourLabel(int hour) {
        if (hour == 0) {
            return "12a";
        } else if (hour < 12) {
            return hour + "a";
        } else if (hour == 12) {
            return "12p";
        } else {
            return (hour - 12) + "p";
        }
    }

    /**
     * Get a color from the blue color scale based on a ratio (0.0 to 1.0).
     *
     * @param ratio The ratio from 0.0 (min) to 1.0 (max)
     * @return The color for that ratio
     */
    private Color getColor(float ratio) {
        int r = (int) (MIN_COLOR.getRed() + ratio * (MAX_COLOR.getRed() - MIN_COLOR.getRed()));
        int g = (int) (MIN_COLOR.getGreen() + ratio * (MAX_COLOR.getGreen() - MIN_COLOR.getGreen()));
        int b = (int) (MIN_COLOR.getBlue() + ratio * (MAX_COLOR.getBlue() - MIN_COLOR.getBlue()));

        return new Color(r, g, b);
    }
}