package gui;

import database.CRUDOperations;
import database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CRUDPanel extends JPanel {
    private CRUDOperations crudOperations;
    private JTextField nameField, emailField, phoneField;
    private JTable table;
    private DefaultTableModel tableModel;

    public CRUDPanel(DatabaseManager databaseManager) {
        this.crudOperations = new CRUDOperations(databaseManager);
        setLayout(new BorderLayout());

        // Form for Adding Customers
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Customer"));

        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Email:"));
        emailField = new JTextField();
        formPanel.add(emailField);

        formPanel.add(new JLabel("Phone:"));
        phoneField = new JTextField();
        formPanel.add(phoneField);

        JButton addButton = new JButton("Add Customer");
        addButton.addActionListener(this::addCustomer);
        formPanel.add(addButton);

        add(formPanel, BorderLayout.NORTH);

        // Table for displaying customers
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Phone"}, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Load data initially
        loadCustomers();
    }

    private void addCustomer(ActionEvent e) {
        String name = nameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            crudOperations.addCustomer(name, email, phone);
            JOptionPane.showMessageDialog(this, "Customer added successfully!");
            loadCustomers(); // Refresh table data
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding customer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCustomers() {
        try {
            ResultSet rs = crudOperations.getCustomers();
            tableModel.setRowCount(0); // Clear previous data

            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getInt("cardholder_id"), rs.getString("first_name"),
                        rs.getString("email"), rs.getString("phone")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading customers: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}