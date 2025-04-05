package payment.gui;

import payment.database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Panel for displaying transaction status breakdown in a graphical format.
 */
public class TransactionStatusPanel extends JPanel {
  private final Connection connection;

    // Status categories
    private static final String[] STATUS_TYPES = {"Approved", "Declined", "Pending"};

    // Colors for different statuses
    private static final Color[] STATUS_COLORS = {
            new Color(46, 139, 87),  // Approved - SeaGreen
            new Color(178, 34, 34),  // Declined - Firebrick
            new Color(255, 165, 0)   // Pending - Orange
    };

    // UI Components
    private JPanel barChartPanel;
    private JPanel legendPanel;

    // Data holders
    private int[] statusCounts;
    private double[] statusPercentages;
    private double[] statusAmounts;
    private int totalTransactions;
    private double totalAmount;

    // Formatters
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    /**
     * Constructor for the transaction status panel.
     *
     * @param dbManager The database manager
     */
    public TransactionStatusPanel(DatabaseManager dbManager) {
      this.connection = dbManager.getConnection();

        // Initialize data arrays
        statusCounts = new int[STATUS_TYPES.length];
        statusPercentages = new double[STATUS_TYPES.length];
        statusAmounts = new double[STATUS_TYPES.length];

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        // Initialize UI components
        initComponents();

        // Load initial data
        refreshData();
    }

    /**
     * Initialize UI components.
     */
    private void initComponents() {
        // Title label
        JLabel titleLabel = new JLabel("Transaction Status Breakdown", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(titleLabel, BorderLayout.NORTH);

        // Create chart panel
        barChartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBarChart(g);
            }
        };
        barChartPanel.setBackground(Color.WHITE);
        add(barChartPanel, BorderLayout.CENTER);

        // Create legend panel
        legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        legendPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        for (int i = 0; i < STATUS_TYPES.length; i++) {
            JPanel item = createLegendItem(STATUS_TYPES[i], STATUS_COLORS[i]);
            legendPanel.add(item);
        }

        add(legendPanel, BorderLayout.SOUTH);
    }

    /**
     * Create a legend item with color box and label.
     *
     * @param text The label text
     * @param color The color
     * @return The legend item panel
     */
    private JPanel createLegendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JPanel colorBox = new JPanel();
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(15, 15));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JLabel label = new JLabel(text);

        item.add(colorBox);
        item.add(label);

        return item;
    }

    /**
     * Refresh data from the database.
     */
    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    loadTransactionStatusData();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            TransactionStatusPanel.this,
                            "Error loading transaction status data: " + ex.getMessage(),
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
     * Load transaction status data from the database.
     *
     * @throws SQLException If a database error occurs
     */
    private void loadTransactionStatusData() throws SQLException {
        // Reset counters
        totalTransactions = 0;
        totalAmount = 0;

        for (int i = 0; i < STATUS_TYPES.length; i++) {
            statusCounts[i] = 0;
            statusAmounts[i] = 0;
        }

        // Query database for transaction status breakdown
        String query = "SELECT " +
                "    status, " +
                "    COUNT(*) as count, " +
                "    SUM(amount) as total_amount " +
                "FROM Transaction " +
                "GROUP BY status";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("count");
                double amount = rs.getDouble("total_amount");

                // Map status to index
                int index = getStatusIndex(status);
                if (index >= 0) {
                    statusCounts[index] = count;
                    statusAmounts[index] = amount;
                }

                totalTransactions += count;
                totalAmount += amount;
            }
        }

        // Calculate percentages
        for (int i = 0; i < STATUS_TYPES.length; i++) {
            statusPercentages[i] = totalTransactions > 0 ?
                    (double) statusCounts[i] / totalTransactions : 0;
        }
    }

    /**
     * Get the index for a status in the STATUS_TYPES array.
     *
     * @param status The status string
     * @return The index or -1 if not found
     */
    private int getStatusIndex(String status) {
        for (int i = 0; i < STATUS_TYPES.length; i++) {
            if (STATUS_TYPES[i].equalsIgnoreCase(status)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Draw the bar chart for transaction status breakdown.
     *
     * @param g The graphics context
     */
    private void drawBarChart(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int barWidth = width / (STATUS_TYPES.length * 2 + 1);
        int maxBarHeight = height - 60;  // Leave space for labels

        // Draw bars
        for (int i = 0; i < STATUS_TYPES.length; i++) {
            // Skip if no data
            if (statusCounts[i] == 0) continue;

            int x = (i * 2 + 1) * barWidth;
            int barHeight = (int) (statusPercentages[i] * maxBarHeight);
            int y = height - 40 - barHeight;

            // Draw bar
            g2d.setColor(STATUS_COLORS[i]);
            g2d.fillRect(x, y, barWidth, barHeight);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, barWidth, barHeight);

            // Draw count and percentage at top of bar
            String countText = String.valueOf(statusCounts[i]);
            String percentText = percentFormat.format(statusPercentages[i]);
            FontMetrics fm = g2d.getFontMetrics();

            int countWidth = fm.stringWidth(countText);
            int percentWidth = fm.stringWidth(percentText);

            // Draw count
            g2d.drawString(countText, x + (barWidth - countWidth) / 2, y - 20);

            // Draw percentage
            g2d.drawString(percentText, x + (barWidth - percentWidth) / 2, y - 5);

            // Draw amount at bottom of bar
            String amountText = currencyFormat.format(statusAmounts[i]);
            int amountWidth = fm.stringWidth(amountText);
            g2d.drawString(amountText, x + (barWidth - amountWidth) / 2, height - 5);
        }
    }
}