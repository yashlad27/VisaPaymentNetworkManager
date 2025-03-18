package gui;

import database.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;

public class VisaPaymentNetworkManager extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private DatabaseManager databaseManager;
    private Connection connection;

    public VisaPaymentNetworkManager() {
        setTitle("Visa Payment Network Manager");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize DatabaseManager
        databaseManager = new DatabaseManager();

        // Ensure the database connection is established before creating panels
        if (databaseManager.connect() == null) {
            JOptionPane.showMessageDialog(this, "Database connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        // Create Panels
        CRUDPanel crudPanel = new CRUDPanel(databaseManager);
        JoinsPanel joinsPanel = new JoinsPanel(databaseManager);
        ViewsPanel viewsPanel = new ViewsPanel(databaseManager);
        AggregatePanel aggregatePanel = new AggregatePanel(databaseManager);

        // Add panels to the main layout
        mainPanel.add(crudPanel, "CRUD");
        mainPanel.add(joinsPanel, "Joins");
        mainPanel.add(viewsPanel, "Views");
        mainPanel.add(aggregatePanel, "Aggregates");

        // Navigation Menu
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new FlowLayout());

        JButton crudButton = new JButton("CRUD Operations");
        JButton joinsButton = new JButton("Joins");
        JButton viewsButton = new JButton("Views");
        JButton aggregatesButton = new JButton("Aggregates");
        JButton connectButton = new JButton("Connect to Database");

        menuPanel.add(crudButton);
        menuPanel.add(joinsButton);
        menuPanel.add(viewsButton);
        menuPanel.add(aggregatesButton);
        menuPanel.add(connectButton);

        // Button Actions
        crudButton.addActionListener(e -> cardLayout.show(mainPanel, "CRUD"));
        joinsButton.addActionListener(e -> cardLayout.show(mainPanel, "Joins"));
        viewsButton.addActionListener(e -> cardLayout.show(mainPanel, "Views"));
        aggregatesButton.addActionListener(e -> cardLayout.show(mainPanel, "Aggregates"));
        connectButton.addActionListener(e -> connectToDatabase());

        // Layout Setup
        setLayout(new BorderLayout());
        add(menuPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void connectToDatabase() {
        try {
            connection = databaseManager.connect();
            JOptionPane.showMessageDialog(this, "Connected to Database Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database Connection Failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VisaPaymentNetworkManager manager = new VisaPaymentNetworkManager();
            manager.setVisible(true);
        });
    }
}