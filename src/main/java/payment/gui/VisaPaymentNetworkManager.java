package payment.gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;

import javax.swing.*;

import payment.database.DatabaseManager;

/**
 * Main application class for the Visa Payment Network Manager.
 * This application provides a dashboard for analyzing payment data.
 */
public class VisaPaymentNetworkManager extends JFrame {
  private static final String APP_TITLE = "Visa Payment Network Dashboard";
  private static final int DEFAULT_WIDTH = 1200;
  private static final int DEFAULT_HEIGHT = 800;

  private final DatabaseManager dbManager;

  /**
   * Constructor for the Visa Payment Network Manager.
   */
  public VisaPaymentNetworkManager() {
    // Setup database connection
    dbManager = DatabaseManager.getInstance();
    Connection conn = dbManager.connect();

    if (conn == null) {
      JOptionPane.showMessageDialog(this,
              "Unable to connect to database. Application will exit.",
              "Database Connection Error",
              JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

    // Setup the UI
    initUI();

    // Add a window listener to disconnect from the database on close
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
   */
  private void initUI() {
    setTitle(APP_TITLE);
    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    // Create tabbed pane for different views
    JTabbedPane tabbedPane = new JTabbedPane();

    // Create and add dashboard panels
    tabbedPane.addTab("Dashboard Overview", new DashboardPanel(dbManager));
    tabbedPane.addTab("Card Usage", new CardUsagePanel(dbManager));
    tabbedPane.addTab("Bank Analysis", new BankAnalysisPanel(dbManager));
    tabbedPane.addTab("Peak Sales", new PeakSalesPanel(dbManager));
    tabbedPane.addTab("Custom Query", new QueryPanel(dbManager));
    tabbedPane.addTab("CRUD Operations", new CRUDPanel(dbManager));

    // Add to content pane
    getContentPane().add(tabbedPane, BorderLayout.CENTER);

    // Add a status bar at the bottom
    JPanel statusBar = createStatusBar();
    getContentPane().add(statusBar, BorderLayout.SOUTH);
  }

  /**
   * Create a status bar with database connection info.
   *
   * @return The status bar panel
   */
  private JPanel createStatusBar() {
    JPanel statusBar = new JPanel(new BorderLayout());
    statusBar.setBorder(BorderFactory.createEtchedBorder());

    JLabel statusLabel = new JLabel("  Connected to database: visa_final_spring");

    JLabel timeLabel = new JLabel();
    // Update the time every second
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
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    // Set look and feel to system default
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Start application on Swing EDT
    SwingUtilities.invokeLater(() -> {
      VisaPaymentNetworkManager app = new VisaPaymentNetworkManager();
      app.setVisible(true);
    });
  }
}