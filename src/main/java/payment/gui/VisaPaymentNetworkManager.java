package payment.gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;

import javax.swing.*;

import payment.database.DatabaseManager;

/**
 * Main application class for the Visa Payment Network Manager.
 * <p>
 * This application provides a comprehensive dashboard for monitoring, analyzing, and
 * visualizing credit card payment transaction data within the Visa payment network.
 * It serves as the entry point for the application and creates the main application
 * window containing multiple specialized panels for different aspects of payment analysis.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Dashboard overview with summary statistics and transaction trends</li>
 *   <li>Card usage analysis with distribution charts and transaction details</li>
 *   <li>Bank performance analysis for issuing and acquiring banks</li>
 *   <li>Peak sales analysis with time-based charts and heatmaps</li>
 *   <li>Custom SQL query interface for advanced data exploration</li>
 *   <li>CRUD operations for database entity management</li>
 *   <li>Real-time status updates and connection monitoring</li>
 * </ul>
 * </p>
 * <p>
 * The application utilizes a tabbed interface to organize these features into
 * separate panels, each focusing on a specific aspect of payment data analysis.
 * </p>
 */
public class VisaPaymentNetworkManager extends JFrame {
    /**
     * Application title displayed in the window title bar
     */
    private static final String APP_TITLE = "Visa Payment Network Dashboard";

    /**
     * Default window width in pixels
     */
    private static final int DEFAULT_WIDTH = 1200;

    /**
     * Default window height in pixels
     */
    private static final int DEFAULT_HEIGHT = 800;

    /**
     * Database manager instance for database connectivity throughout the application
     */
    private final DatabaseManager dbManager;

    /**
     * Constructor for the Visa Payment Network Manager.
     * <p>
     * Initializes the application by establishing a database connection and setting up
     * the user interface. If the database connection fails, an error message is displayed
     * and the application exits.
     * </p>
     * <p>
     * Also sets up a window listener to properly disconnect from the database when
     * the application is closed, ensuring proper resource cleanup.
     * </p>
     */
    public VisaPaymentNetworkManager() {
        dbManager = DatabaseManager.getInstance();
        Connection conn = dbManager.connect();

        if (conn == null) {
            JOptionPane.showMessageDialog(this,
                    "Unable to connect to database. Application will exit.",
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        initUI();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dbManager.disconnect();
                System.exit(0);
            }
        });
    }

    /**
     * Initialize the user interface components.
     * <p>
     * Sets up the main application window with appropriate size and position,
     * creates the tabbed pane containing all analysis panels, and adds a status
     * bar at the bottom of the window.
     * </p>
     * <p>
     * Each tab represents a different aspect of payment data analysis:
     * <ul>
     *   <li>Dashboard Overview: Summary statistics and key metrics</li>
     *   <li>Card Usage: Card distribution analysis and transaction details</li>
     *   <li>Bank Analysis: Bank performance metrics and transaction history</li>
     *   <li>Peak Sales: Time-based analysis of transaction patterns</li>
     *   <li>Custom Query: Interface for executing custom SQL queries</li>
     *   <li>CRUD Operations: Interface for managing database records</li>
     * </ul>
     * </p>
     */
    private void initUI() {
        setTitle(APP_TITLE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Dashboard Overview", new DashboardPanel(dbManager));
        tabbedPane.addTab("Card Usage", new CardUsagePanel(dbManager));
        tabbedPane.addTab("Bank Analysis", new BankAnalysisPanel(dbManager));
        tabbedPane.addTab("Peak Sales", new PeakSalesPanel(dbManager));
        tabbedPane.addTab("Custom Query", new QueryPanel(dbManager));
        tabbedPane.addTab("CRUD Operations", new CRUDPanel(dbManager));

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        JPanel statusBar = createStatusBar();
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Create a status bar with database connection info and current time.
     * <p>
     * This method creates a panel at the bottom of the application window that
     * displays the current database connection status and the current time, which
     * is updated every second.
     * </p>
     *
     * @return The configured status bar panel
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        JLabel statusLabel = new JLabel("  Connected to database: visa_final_spring");

        JLabel timeLabel = new JLabel();
        Timer timer = new Timer(1000, e -> {
            timeLabel.setText(new java.util.Date().toString() + "  ");
        });
        timer.start();

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(timeLabel, BorderLayout.EAST);

        return statusBar;
    }

    /**
     * Main method to start the application.
     * <p>
     * This is the entry point for the application. It sets the look and feel to match
     * the system's native look and feel, then creates and displays the main application
     * window on the Event Dispatch Thread (EDT) to ensure thread safety for Swing components.
     * </p>
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            VisaPaymentNetworkManager app = new VisaPaymentNetworkManager();
            app.setVisible(true);
        });
    }
}