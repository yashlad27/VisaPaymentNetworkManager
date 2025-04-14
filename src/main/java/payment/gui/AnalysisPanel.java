package payment.gui;

import payment.database.DatabaseManager;

/**
 * Interface defining the contract for all analysis panels in the payment network.
 * <p>
 * This interface ensures that all analysis panels implement the required functionality
 * for displaying and analyzing payment network data. It defines the core methods that
 * must be implemented by any class that wants to function as an analysis panel.
 * </p>
 */
public interface AnalysisPanel {
    /**
     * Initialize the panel's components and layout.
     * This method should be called after construction to set up the UI.
     */
    void initComponents();

    /**
     * Initialize the panel's data models.
     * This method should set up any table models or data structures needed.
     */
    void initTableModels();

    /**
     * Refresh the panel's data from the database.
     * This method should update all displayed information with current data.
     */
    void refreshData();

    /**
     * Get the database manager instance used by this panel.
     *
     * @return The database manager instance
     */
    DatabaseManager getDatabaseManager();

    /**
     * Set up automatic data refresh for this panel.
     * This method should configure a timer to periodically refresh the data.
     */
    void setupRefreshTimer();

    /**
     * Clean up resources when the panel is no longer needed.
     * This method should stop any timers and release any resources.
     */
    void cleanup();
} 