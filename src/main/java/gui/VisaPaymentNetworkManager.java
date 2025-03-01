package gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import database.DatabaseManager;

public class VisaPaymentNetworkManager extends JFrame {
  // database connection details
  private static final String DB_URL = "jdbc:mysql://localhost:3306/visa_payment_network";
  private static final String DB_USER = "root";
  private static final String DB_PASSWORD = "test123"; // Set your database password here

  // database manager
  private DatabaseManager dbManager;

  // UI Components
  private JLabel statusLabel;
  private JButton connectButton;
  private JTabbedPane tabbedPane;

  // Panel references
  private QueryPanel queryPanel;
  private CRUDPanel crudPanel;
  private JoinsPanel joinsPanel;
  private ViewsPanel viewsPanel;
//  private AggregatesPanel aggregatesPanel;

  public VisaPaymentNetworkManager() {
    // Set up the JFrame
    setTitle("Visa Payment Network Manager");
    setSize(1200, 800);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    // Get the database manager instance
    dbManager = DatabaseManager.getInstance();

    // Initialize UI components
    initializeUI();

    // Add window closing event to disconnect from database
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (dbManager != null) {
          dbManager.disconnect();
        }
      }
    });

    // Display the frame
    setVisible(true);
  }

  private void initializeUI() {
    // Create the main panel with border layout
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create database panels
    queryPanel = new QueryPanel();
    crudPanel = new CRUDPanel();
    joinsPanel = new JoinsPanel();
    viewsPanel = new ViewsPanel();
    //aggregatesPanel = new AggregatesPanel();

    // Create tabbed pane
    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Custom Query", queryPanel);
    tabbedPane.addTab("CRUD Operations", crudPanel);
    tabbedPane.addTab("Joins", joinsPanel);
    tabbedPane.addTab("Views", viewsPanel);
    //tabbedPane.addTab("Aggregates", aggregatesPanel);

    // Add the tabbed pane to the main panel
    mainPanel.add(tabbedPane, BorderLayout.CENTER);

    // Create status panel
    JPanel statusPanel = new JPanel(new BorderLayout());
    statusLabel = new JLabel("Not connected to database");
    connectButton = new JButton("Connect to Database");
    connectButton.addActionListener(e -> connectToDatabase());

    statusPanel.add(statusLabel, BorderLayout.WEST);
    statusPanel.add(connectButton, BorderLayout.EAST);
    mainPanel.add(statusPanel, BorderLayout.SOUTH);

    // Set the main panel as the content pane
    setContentPane(mainPanel);
  }

  private void connectToDatabase() {
    boolean connected = dbManager.connect(DB_URL, DB_USER, DB_PASSWORD);

    if (connected) {
      statusLabel.setText("Connected to database");
      connectButton.setEnabled(false);

      // Notify all panels that connection is established
      queryPanel.onDatabaseConnected();
      crudPanel.onDatabaseConnected();
      joinsPanel.onDatabaseConnected();
      viewsPanel.onDatabaseConnected();
      //aggregatesPanel.onDatabaseConnected();

      JOptionPane.showMessageDialog(this, "Successfully connected to the database",
              "Connection Status", JOptionPane.INFORMATION_MESSAGE);
    }
  }

  public static void main(String[] args) {
    try {
      // Set the look and feel to the system look and feel
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Start the application on the Event Dispatch Thread
    SwingUtilities.invokeLater(() -> new VisaPaymentNetworkManager());
  }
}