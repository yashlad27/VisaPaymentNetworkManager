package payment.gui;

import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import payment.database.DatabaseManager;

/**
 * Panel for displaying hourly transaction volume in a line chart.
 */
public class HourlyVolumePanel extends JPanel {
    private final Connection connection;
    private boolean viewByCount;

    private int[] transactionCounts;
    private double[] transactionAmounts;
    private int maxCount;
    private double maxAmount;

    private JPanel chartPanel;
    private JLabel titleLabel;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    /**
     * Constructor for the hourly volume panel.
     *
     * @param dbManager   The database manager
     * @param viewByCount Whether to view by count (true) or amount (false)
     */
    public HourlyVolumePanel(DatabaseManager dbManager, boolean viewByCount) {
        this.connection = dbManager.getConnection();
        this.viewByCount = viewByCount;

        transactionCounts = new int[24];
        transactionAmounts = new double[24];

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        initComponents();
        refreshData();
    }

    /**
     * Initialize UI components.
     */
    private void initComponents() {
        titleLabel = new JLabel("Hourly Transaction Volume", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(titleLabel, BorderLayout.NORTH);

        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawLineChart(g);
            }
        };
        chartPanel.setBackground(Color.WHITE);
        add(chartPanel, BorderLayout.CENTER);
    }

    /**
     * Refresh data from the database.
     */
    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    loadHourlyVolumeData();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            HourlyVolumePanel.this,
                            "Error loading hourly volume data: " + ex.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                return null;
            }

            @Override
            protected void done() {
                repaint();
            }
        };
        worker.execute();
    }

    /**
     * Load hourly volume data from the database.
     *
     * @throws SQLException If a database error occurs
     */
    private void loadHourlyVolumeData() throws SQLException {
        // Reset data
        maxCount = 0;
        maxAmount = 0;

        for (int i = 0; i < 24; i++) {
            transactionCounts[i] = 0;
            transactionAmounts[i] = 0;
        }

        // Query database for hourly transaction volume
        String query = "SELECT " +
                "    HOUR(timestamp) as hour, " +
                "    COUNT(*) as count, " +
                "    SUM(amount) as total_amount " +
                "FROM Transaction " +
                "GROUP BY HOUR(timestamp) " +
                "ORDER BY hour";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int hour = rs.getInt("hour");
                int count = rs.getInt("count");
                double amount = rs.getDouble("total_amount");

                if (hour >= 0 && hour < 24) {
                    transactionCounts[hour] = count;
                    transactionAmounts[hour] = amount;

                    if (count > maxCount) {
                        maxCount = count;
                    }
                    if (amount > maxAmount) {
                        maxAmount = amount;
                    }
                }
            }
        }
    }

    /**
     * Draw the line chart for hourly transaction volume.
     *
     * @param g The graphics context
     */
    private void drawLineChart(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int chartX = 50;
        int chartY = 30;
        int chartWidth = width - chartX - 20;
        int chartHeight = height - chartY - 50;

        g2d.setColor(Color.BLACK);
        g2d.drawLine(chartX, chartY, chartX, chartY + chartHeight);
        g2d.drawLine(chartX, chartY + chartHeight, chartX + chartWidth, chartY + chartHeight);

        for (int i = 0; i < 24; i += 2) {
            int x = chartX + (i * chartWidth / 23);
            g2d.drawLine(x, chartY + chartHeight, x, chartY + chartHeight + 5);

            String hourLabel = formatHour(i);
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(hourLabel);

            g2d.drawString(hourLabel, x - labelWidth / 2, chartY + chartHeight + 20);
        }

        int yDivisions = 5;
        for (int i = 0; i <= yDivisions; i++) {
            int y = chartY + chartHeight - (i * chartHeight / yDivisions);
            g2d.drawLine(chartX - 5, y, chartX, y);

            double value = viewByCount ?
                    maxCount * i / yDivisions :
                    maxAmount * i / yDivisions;

            String label = viewByCount ?
                    String.valueOf((int) value) :
                    currencyFormat.format(value);

            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);

            g2d.drawString(label, chartX - labelWidth - 10, y + fm.getAscent() / 2);
        }

        int[] xPoints = new int[24];
        int[] yPoints = new int[24];

        for (int i = 0; i < 24; i++) {
            xPoints[i] = chartX + (i * chartWidth / 23);

            double value = viewByCount ? transactionCounts[i] : transactionAmounts[i];
            double maxValue = viewByCount ? maxCount : maxAmount;

            double ratio = maxValue > 0 ? value / maxValue : 0;
            yPoints[i] = chartY + chartHeight - (int) (ratio * chartHeight);
        }

        int[] areaXPoints = new int[26];
        int[] areaYPoints = new int[26];

        System.arraycopy(xPoints, 0, areaXPoints, 0, 24);
        System.arraycopy(yPoints, 0, areaYPoints, 0, 24);

        areaXPoints[24] = chartX + chartWidth;
        areaYPoints[24] = chartY + chartHeight;
        areaXPoints[25] = chartX;
        areaYPoints[25] = chartY + chartHeight;

        g2d.setColor(new Color(0, 120, 215, 64));
        g2d.fillPolygon(areaXPoints, areaYPoints, 26);

        g2d.setColor(new Color(0, 102, 204));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawPolyline(xPoints, yPoints, 24);

        g2d.setColor(new Color(30, 144, 255));
        for (int i = 0; i < 24; i++) {
            double value = viewByCount ? transactionCounts[i] : transactionAmounts[i];
            if (value > 0) {
                g2d.fillOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.fillOval(xPoints[i] - 2, yPoints[i] - 2, 4, 4);
                g2d.setColor(new Color(30, 144, 255));
            }
        }
    }

    /**
     * Format an hour for display on the x-axis.
     *
     * @param hour The hour (0-23)
     * @return The formatted hour string
     */
    private String formatHour(int hour) {
        if (hour == 0) {
            return "12am";
        } else if (hour < 12) {
            return hour + "am";
        } else if (hour == 12) {
            return "12pm";
        } else {
            return (hour - 12) + "pm";
        }
    }
}