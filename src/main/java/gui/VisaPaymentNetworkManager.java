package gui;

import database.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;

public class VisaPaymentNetworkManager extends JFrame {
    private JTabbedPane tabbedPane;
    private DatabaseManager databaseManager;
    private Connection connection;

    // Panels for different business functions
    private TransactionPanel transactionPanel;
    private CardManagementPanel cardManagementPanel;
    private MerchantPanel merchantPanel;
    private ReportingPanel reportingPanel;
    private SettlementPanel settlementPanel;

    public VisaPaymentNetworkManager() {
        setTitle("Visa Payment Network Manager");
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize database connection
        databaseManager = DatabaseManager.getInstance();

        // Add window closing event to ensure database disconnection
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (databaseManager != null) {
                    databaseManager.disconnect();
                }
                super.windowClosing(e);
            }
        });

        // Initialize UI
        initializeUI();

        // Connect to database on startup
        connectToDatabase();
    }

    private void initializeUI() {
        // Create tabbed pane for different business functions
        tabbedPane = new JTabbedPane();

        // Create panels for different business functions
        transactionPanel = new TransactionPanel(databaseManager);
        cardManagementPanel = new CardManagementPanel(databaseManager);
        merchantPanel = new MerchantPanel(databaseManager);
        reportingPanel = new ReportingPanel(databaseManager);
        settlementPanel = new SettlementPanel(databaseManager);

        // Add tabs
        tabbedPane.addTab("Transactions", new ImageIcon(), transactionPanel,
                "Process and manage transactions");
        tabbedPane.addTab("Card Management", new ImageIcon(), cardManagementPanel,
                "Manage cards and cardholders");
        tabbedPane.addTab("Merchants", new ImageIcon(), merchantPanel,
                "Manage merchant accounts");
        tabbedPane.addTab("Settlements", new ImageIcon(), settlementPanel,
                "Process settlements");
        tabbedPane.addTab("Reports", new ImageIcon(), reportingPanel,
                "View reports and analytics");

        // Create status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        JLabel statusLabel = new JLabel("Ready");
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // Add components to frame
        setLayout(new BorderLayout());
        add(createToolbar(), BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton connectButton = new JButton("Connect");
        connectButton.setToolTipText("Connect to database");
        connectButton.addActionListener(e -> connectToDatabase());

        JButton helpButton = new JButton("Help");
        helpButton.setToolTipText("View help");
        helpButton.addActionListener(e -> showHelp());

        toolbar.add(connectButton);
        toolbar.addSeparator();
        toolbar.add(helpButton);

        return toolbar;
    }

    private void connectToDatabase() {
        try {
            connection = databaseManager.connect();
            if (connection != null) {
                JOptionPane.showMessageDialog(this,
                        "Connected to Database Successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                // Notify all panels of database connection
                transactionPanel.onDatabaseConnected();
                cardManagementPanel.onDatabaseConnected();
                merchantPanel.onDatabaseConnected();
                reportingPanel.onDatabaseConnected();
                settlementPanel.onDatabaseConnected();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Database Connection Failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(this,
                "Visa Payment Network Manager\n\n" +
                        "This application allows you to manage payment transactions, " +
                        "cards, merchants, and settlements within the Visa network.\n\n" +
                        "For assistance, please contact support at support@example.com.",
                "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            VisaPaymentNetworkManager manager = new VisaPaymentNetworkManager();
            manager.setVisible(true);
        });
    }
}