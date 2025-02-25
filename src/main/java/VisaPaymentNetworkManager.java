import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class VisaPaymentNetworkManager extends JFrame {
  // database connection details:
  private static final String DB_URL = "jdbc:mysql://localhost:3306/visa_payment_network";
  private static final String DB_USER = "root";
  private static final String DB_PASSWORD = "test123";

  // JDBC Objects
  private Connection connection;
  private Statement statement;

  private JLabel statusLabel;
  private JButton connectButton;

  public VisaPaymentNetworkManager() {
    setTitle("Visa Payment Network Manager");
    setSize(1000, 800);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    initializeUI();
    setVisible(true);
  }

  private void initializeUI() {
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    JTabbedPane tabbedPane = new JTabbedPane();

    JPanel queryPanel = new JPanel();
    JPanel crudPanel = new JPanel();
    JPanel joinsPanel = new JPanel();
    JPanel viewsPanel = new JPanel();
    JPanel aggregatesPanel = new JPanel();

    tabbedPane.addTab("Custom Query", queryPanel);
    tabbedPane.addTab("CRUD Operations", crudPanel);
    tabbedPane.addTab("Joins", joinsPanel);
    tabbedPane.addTab("Views", viewsPanel);
    tabbedPane.addTab("Aggregates", aggregatesPanel);

    mainPanel.add(tabbedPane, BorderLayout.CENTER);

    JPanel statusPanel = new JPanel(new BorderLayout());
    statusLabel = new JLabel("Not connected to database");
    connectButton = new JButton("Connect to Database");
    connectButton.addActionListener(e -> connectToDatabase());

    statusPanel.add(statusLabel, BorderLayout.WEST);
    statusPanel.add(connectButton, BorderLayout.EAST);
    mainPanel.add(statusPanel, BorderLayout.SOUTH);

    setContentPane(mainPanel);
  }

  private void connectToDatabase() {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");

      connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
      statement = connection.createStatement();

      statusLabel.setText("Connected to db");
      connectButton.setEnabled(false);

      JOptionPane.showMessageDialog(this, "Successfully connected to the db",
              "Connection Status", JOptionPane.INFORMATION_MESSAGE);

    } catch (ClassNotFoundException e) {
      statusLabel.setText("JDBC Driver not found");
      JOptionPane.showMessageDialog(this, "JDBC Drivernot found: " + e.getMessage(),
              "Connection Error", JOptionPane.ERROR_MESSAGE);
    } catch (SQLException e) {
      statusLabel.setText("failed to connect to db");
      JOptionPane.showMessageDialog(this, "JDBC Driver not found: "
              + e.getMessage());
    }
  }

  public static void main(String[] args) {
    // SwingUtilities for thread safety
    SwingUtilities.invokeLater(VisaPaymentNetworkManager::new);
  }
}
