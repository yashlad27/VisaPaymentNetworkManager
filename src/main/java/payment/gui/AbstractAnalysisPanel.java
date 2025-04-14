package payment.gui;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import payment.database.DatabaseManager;

/**
 * Abstract base class for analysis panels in the payment network.
 * Provides common functionality and structure for all analysis panels.
 * <p>
 * This abstract class serves as a foundation for all analysis panels in the
 * Visa payment network application. It implements the AnalysisPanel interface
 * and provides common functionality that can be shared across all concrete
 * implementations.
 * </p>
 */
public abstract class AbstractAnalysisPanel extends JPanel implements AnalysisPanel {
    protected final DatabaseManager dbManager;
    protected final Connection connection;
    protected final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    protected Timer refreshTimer;
    protected final int REFRESH_INTERVAL = 60000; // 1 minute

    protected AbstractAnalysisPanel(DatabaseManager dbManager) {
        super();
        if (dbManager == null) {
            throw new IllegalArgumentException("Database manager cannot be null");
        }
        this.dbManager = dbManager;
        this.connection = dbManager.getConnection();
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    @Override
    public void setupRefreshTimer() {
        refreshTimer = new Timer(REFRESH_INTERVAL, e -> refreshData());
        refreshTimer.start();
    }

    @Override
    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            // Log the error but don't throw it
            e.printStackTrace();
        }
    }

    @Override
    public abstract void initComponents();

    @Override
    public abstract void initTableModels();

    @Override
    public abstract void refreshData();
} 