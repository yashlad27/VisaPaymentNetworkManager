package payment.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import payment.database.DatabaseManager;

/**
 * Panel for analyzing peak sales periods in the payment network.
 * <p>
 * This panel provides analysis of transaction patterns during peak sales periods,
 * including time-based analysis, merchant performance, and transaction volume trends.
 * </p>
 */
public class PeakSalesPanel extends AbstractAnalysisPanel {
    private JTable timeDistributionTable;
    private JTable merchantPerformanceTable;
    private DefaultTableModel timeDistributionModel;
    private DefaultTableModel merchantPerformanceModel;
    private JLabel totalTransactionsLabel;
    private JLabel peakHourLabel;
    private JLabel peakDayLabel;
    private JLabel averageAmountLabel;

    public PeakSalesPanel(DatabaseManager dbManager) {
        super(dbManager);
        initTableModels();
        initComponents();
        refreshData();
        setupRefreshTimer();
    }

    @Override
    public void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel summaryPanel = createSummaryPanel();
        JPanel analysisPanel = createAnalysisPanel();
        
        mainPanel.add(summaryPanel, BorderLayout.NORTH);
        mainPanel.add(analysisPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public void initTableModels() {
        String[] timeColumns = {"Hour", "Day", "Transactions", "Total Amount", "Average Amount"};
        timeDistributionModel = new DefaultTableModel(timeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        timeDistributionTable = new JTable(timeDistributionModel);

        String[] merchantColumns = {"Merchant", "Transactions", "Total Amount", "Success Rate"};
        merchantPerformanceModel = new DefaultTableModel(merchantColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        merchantPerformanceTable = new JTable(merchantPerformanceModel);
    }

    @Override
    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    loadTimeDistributionData();
                    loadMerchantPerformanceData();
                    updateSummaryLabels();
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(
                            PeakSalesPanel.this,
                            "Error refreshing data: " + e.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    );
                }
                return null;
            }
        };
        worker.execute();
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        totalTransactionsLabel = new JLabel("Total Transactions: ");
        peakHourLabel = new JLabel("Peak Hour: ");
        peakDayLabel = new JLabel("Peak Day: ");
        averageAmountLabel = new JLabel("Average Amount: ");

        panel.add(totalTransactionsLabel);
        panel.add(peakHourLabel);
        panel.add(peakDayLabel);
        panel.add(averageAmountLabel);

        return panel;
    }

    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane timeScroll = new JScrollPane(timeDistributionTable);
        JScrollPane merchantScroll = new JScrollPane(merchantPerformanceTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(timeScroll);
        splitPane.setBottomComponent(merchantScroll);
        splitPane.setResizeWeight(0.5);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadTimeDistributionData() throws SQLException {
        timeDistributionModel.setRowCount(0);
        String query = "SELECT " +
            "HOUR(timestamp) as hour, " +
            "DAYNAME(timestamp) as day, " +
            "COUNT(*) as transactions, " +
            "SUM(amount) as total_amount, " +
            "AVG(amount) as avg_amount " +
            "FROM Transaction " +
            "GROUP BY HOUR(timestamp), DAYNAME(timestamp) " +
            "ORDER BY transactions DESC";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            Object totalAmountObj = rs.getObject("total_amount");
            Object avgAmountObj = rs.getObject("avg_amount");
            
            double totalAmount = 0.0;
            double avgAmount = 0.0;
            
            if (totalAmountObj instanceof BigDecimal) {
                totalAmount = ((BigDecimal) totalAmountObj).doubleValue();
            }
            if (avgAmountObj instanceof BigDecimal) {
                avgAmount = ((BigDecimal) avgAmountObj).doubleValue();
            }

            timeDistributionModel.addRow(new Object[]{
                rs.getInt("hour"),
                rs.getString("day"),
                rs.getInt("transactions"),
                currencyFormat.format(totalAmount),
                currencyFormat.format(avgAmount)
            });
        }
    }

    private void loadMerchantPerformanceData() throws SQLException {
        merchantPerformanceModel.setRowCount(0);
        String query = "SELECT " +
            "m.merchant_name, " +
            "COUNT(t.transaction_id) as transactions, " +
            "SUM(t.amount) as total_amount, " +
            "ROUND((COUNT(CASE WHEN t.status = 'SUCCESS' THEN 1 END) / COUNT(t.transaction_id)) * 100, 2) as success_rate " +
            "FROM Transaction t " +
            "JOIN Merchant m ON t.merchant_id = m.merchant_id " +
            "GROUP BY m.merchant_id, m.merchant_name " +
            "ORDER BY transactions DESC";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            Object totalAmountObj = rs.getObject("total_amount");
            double totalAmount = 0.0;
            if (totalAmountObj instanceof BigDecimal) {
                totalAmount = ((BigDecimal) totalAmountObj).doubleValue();
            }

            merchantPerformanceModel.addRow(new Object[]{
                rs.getString("merchant_name"),
                rs.getInt("transactions"),
                currencyFormat.format(totalAmount),
                String.format("%.2f%%", rs.getDouble("success_rate"))
            });
        }
    }

    private void updateSummaryLabels() throws SQLException {
        String query = "SELECT " +
            "COUNT(*) as total_transactions, " +
            "HOUR(timestamp) as peak_hour, " +
            "DAYNAME(timestamp) as peak_day, " +
            "AVG(amount) as avg_amount " +
            "FROM Transaction " +
            "GROUP BY HOUR(timestamp), DAYNAME(timestamp) " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT 1";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        if (rs.next()) {
            totalTransactionsLabel.setText("Total Transactions: " + rs.getInt("total_transactions"));
            peakHourLabel.setText("Peak Hour: " + rs.getInt("peak_hour"));
            peakDayLabel.setText("Peak Day: " + rs.getString("peak_day"));
            
            Object avgAmountObj = rs.getObject("avg_amount");
            double avgAmount = 0.0;
            if (avgAmountObj instanceof BigDecimal) {
                avgAmount = ((BigDecimal) avgAmountObj).doubleValue();
            }
            averageAmountLabel.setText("Average Amount: " + currencyFormat.format(avgAmount));
        }
    }
}