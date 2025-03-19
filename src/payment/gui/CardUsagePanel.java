package payment.gui;

import payment.database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Panel for analyzing card usage patterns.
 */
public class CardUsagePanel extends JPanel {
  private final DatabaseManager dbManager;
  private final Connection connection;

  // UI Components
  private JComboBox<String> viewByComboBox;
  private JTable cardDistributionTable;
  private DefaultTableModel cardDistributionModel;
  private JTable popularCardsTable;
  private DefaultTableModel popularCardsModel;

  // Formatters
  private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

  // Auto-refresh timer
  private Timer refreshTimer;
  private final int REFRESH_INTERVAL = 60000; // 1 minute

  /**
   * Constructor for the card usage panel.
   *
   * @param dbManager The database manager
   */
  public CardUsagePanel(DatabaseManager dbManager) {
    this.dbManager = dbManager;
    this.connection = dbManager.getConnection();

    setLayout(new BorderLayout());
    setBorder(new EmptyBorder(10, 10, 10, 10));

    // Create UI Components
    initComponents();

    // Load initial data
    refreshData();

    // Set up auto-refresh timer
    setupRefreshTimer();
  }

  /**
   * Initialize UI components.
   */
  private void initComponents() {
    // Create title and control panel at the top
    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);

    // Create main content panel with GridBagLayout for flexible layout
    JPanel contentPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    // Create card distribution panel
    JPanel distributionPanel = createCardDistributionPanel();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.weighty = 0.5;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(0, 0, 10, 0);
    contentPanel.add(distributionPanel, gbc);

    // Create popular cards panel
    JPanel popularCardsPanel = createPopularCardsPanel();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weighty = 0.5;
    gbc.insets = new Insets(0, 0, 0, 0);
    contentPanel.add(popularCardsPanel, gbc);

    // Add content panel to main panel with scroll capability
    add(new JScrollPane(contentPanel), BorderLayout.CENTER);
  }

  /**
   * Create the header panel with title and controls.
   *
   * @return The header panel
   */
  private JPanel createHeaderPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EmptyBorder(0, 0, 10, 0));

    // Title label
    JLabel titleLabel = new JLabel("Card Usage Analysis");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
    panel.add(titleLabel, BorderLayout.WEST);

    // Controls panel
    JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    // View by combobox for different metrics
    String[] viewOptions = {"Count", "Value"};
    viewByComboBox = new JComboBox<>(viewOptions);
    viewByComboBox.setPreferredSize(new Dimension(120, 25));
    viewByComboBox.addActionListener(e -> refreshData());

    // Date range combobox
    String[] dateRanges = {"All Time", "Last 7 Days", "Last 30 Days", "Custom..."};
    JComboBox<String> dateRangeCombo = new JComboBox<>(dateRanges);
    dateRangeCombo.setPreferredSize(new Dimension(120, 25));

    // Refresh button
    JButton refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(this::refreshButtonClicked);

    controlsPanel.add(new JLabel("View by:"));
    controlsPanel.add(viewByComboBox);
    controlsPanel.add(new JLabel("Date Range:"));
    controlsPanel.add(dateRangeCombo);
    controlsPanel.add(refreshButton);

    panel.add(controlsPanel, BorderLayout.EAST);

    return panel;
  }

  /**
   * Create the card distribution panel.
   *
   * @return The card distribution panel
   */
  private JPanel createCardDistributionPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Card Type Distribution",
            TitledBorder.LEFT,
            TitledBorder.TOP
    ));

    // Create table model and table
    String[] columns = {"Card Type", "Transaction Count", "Total Value", "Percentage"};
    cardDistributionModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Make all cells non-editable
      }
    };
    cardDistributionTable = new JTable(cardDistributionModel);
    cardDistributionTable.getTableHeader().setReorderingAllowed(false);

    // Add table to panel with scroll pane
    panel.add(new JScrollPane(cardDistributionTable), BorderLayout.CENTER);

    // Description label at the bottom
    JLabel descLabel = new JLabel("Distribution of transactions by card type");
    descLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
    descLabel.setForeground(Color.DARK_GRAY);
    panel.add(descLabel, BorderLayout.SOUTH);

    return panel;
  }

  /**
   * Create the popular cards panel.
   *
   * @return The popular cards panel
   */
  private JPanel createPopularCardsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Popular Cards",
            TitledBorder.LEFT,
            TitledBorder.TOP
    ));

    // Create table model and table
    String[] columns = {"Card ID (Masked)", "Card Type", "Issuing Bank", "Transaction Count", "Total Value"};
    popularCardsModel = new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false; // Make all cells non-editable
      }
    };
    popularCardsTable = new JTable(popularCardsModel);
    popularCardsTable.getTableHeader().setReorderingAllowed(false);

    // Add table to panel with scroll pane
    panel.add(new JScrollPane(popularCardsTable), BorderLayout.CENTER);

    // Description label at the bottom
    JLabel descLabel = new JLabel("Top 10 cards by transaction volume");
    descLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
    descLabel.setForeground(Color.DARK_GRAY);
    panel.add(descLabel, BorderLayout.SOUTH);

    return panel;
  }

  /**
   * Set up the refresh timer.
   */
  private void setupRefreshTimer() {
    refreshTimer = new Timer(REFRESH_INTERVAL, e -> refreshData());
    refreshTimer.start();
  }

  /**
   * Handle refresh button click.
   *
   * @param e The action event
   */
  private void refreshButtonClicked(ActionEvent e) {
    refreshData();
  }

  /**
   * Refresh all data from the database.
   */
  private void refreshData() {
    SwingWorker<Void, Void> worker = new SwingWorker<>() {
      @Override
      protected Void doInBackground() {
        try {
          updateCardDistribution();
          updatePopularCards();
        } catch (SQLException ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(
                  CardUsagePanel.this,
                  "Error refreshing data: " + ex.getMessage(),
                  "Database Error",
                  JOptionPane.ERROR_MESSAGE
          );
        }
        return null;
      }
    };
    worker.execute();
  }

  /**
   * Update card distribution table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updateCardDistribution() throws SQLException {
    boolean viewByCount = viewByComboBox.getSelectedIndex() == 0;

    String query = "SELECT " +
            "    c.card_type, " +
            "    COUNT(t.transaction_id) as transaction_count, " +
            "    SUM(t.amount) as total_amount, " +
            "    ROUND((COUNT(t.transaction_id) / (SELECT COUNT(*) FROM Transaction)) * 100, 2) as percent_by_count, " +
            "    ROUND((SUM(t.amount) / (SELECT SUM(amount) FROM Transaction)) * 100, 2) as percent_by_value " +
            "FROM Transaction t " +
            "JOIN Card c ON t.card_id = c.card_id " +
            "GROUP BY c.card_type " +
            "ORDER BY " + (viewByCount ? "transaction_count" : "total_amount") + " DESC";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      // Clear existing data
      SwingUtilities.invokeLater(() -> cardDistributionModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        final String cardType = rs.getString("card_type");
        final int count = rs.getInt("transaction_count");
        final double amount = rs.getDouble("total_amount");
        final double percent = viewByCount ?
                rs.getDouble("percent_by_count") :
                rs.getDouble("percent_by_value");

        SwingUtilities.invokeLater(() ->
                cardDistributionModel.addRow(new Object[] {
                        cardType,
                        String.format("%,d", count),
                        currencyFormat.format(amount),
                        String.format("%.2f%%", percent)
                })
        );
      }
    }
  }

  /**
   * Update popular cards table.
   *
   * @throws SQLException If a database error occurs
   */
  private void updatePopularCards() throws SQLException {
    boolean viewByCount = viewByComboBox.getSelectedIndex() == 0;

    String query = "SELECT " +
            "    SUBSTRING(c.card_num_hash, 1, 8) as masked_card_id, " +
            "    c.card_type, " +
            "    ib.bank_name as issuing_bank, " +
            "    COUNT(t.transaction_id) as transaction_count, " +
            "    SUM(t.amount) as total_value " +
            "FROM Transaction t " +
            "JOIN Card c ON t.card_id = c.card_id " +
            "JOIN IssuingBank ib ON c.issuing_bank_id = ib.issuing_bank_id " +
            "GROUP BY c.card_id, c.card_type, ib.bank_name, masked_card_id " +
            "ORDER BY " + (viewByCount ? "transaction_count" : "total_value") + " DESC " +
            "LIMIT 10";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      // Clear existing data
      SwingUtilities.invokeLater(() -> popularCardsModel.setRowCount(0));

      // Add new data
      while (rs.next()) {
        final String maskedId = rs.getString("masked_card_id") + "***";
        final String cardType = rs.getString("card_type");
        final String bank = rs.getString("issuing_bank");
        final int count = rs.getInt("transaction_count");
        final double value = rs.getDouble("total_value");

        SwingUtilities.invokeLater(() ->
                popularCardsModel.addRow(new Object[] {
                        maskedId,
                        cardType,
                        bank,
                        String.format("%,d", count),
                        currencyFormat.format(value)
                })
        );
      }
    }
  }
}