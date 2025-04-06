package payment.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager class for handling predefined and custom SQL queries in the application.
 * <p>
 * This class provides centralized management of SQL queries used throughout the
 * application. It maintains a cache of commonly used queries, offers methods for
 * executing both predefined and custom queries, and provides utility methods for
 * processing query results.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Cached predefined queries for common analytics operations</li>
 *   <li>Methods for executing SQL queries and stored functions</li>
 *   <li>Utilities for converting ResultSet objects to more manageable data structures</li>
 *   <li>Support for parameterized queries to prevent SQL injection</li>
 * </ul>
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * // Execute a predefined query
 * Connection conn = dbManager.getConnection();
 * ResultSet rs = QueryManager.executePredefinedQuery(QueryManager.TOP_BANKS_QUERY, conn);
 *
 * // Convert ResultSet to a List of Maps for easier processing
 * List<Map<String, Object>> results = QueryManager.resultSetToList(rs);
 * </pre>
 * </p>
 */
public class QueryManager {
    /**
     * Path to file containing analytics queries
     */
    private static final String ANALYTICS_QUERIES_PATH = "src/res/db/queries/analytics_queries.sql";

    /**
     * Database manager instance for database connectivity
     */
    private static final DatabaseManager dbManager = new DatabaseManager();

    // Predefined query keys
    /**
     * Query key for retrieving top performing banks
     */
    public static final String TOP_BANKS_QUERY = "top_banks";

    /**
     * Query key for retrieving transaction volume by card type
     */
    public static final String CARD_TYPE_VOLUME_QUERY = "card_type_volume";

    /**
     * Query key for retrieving merchant performance metrics
     */
    public static final String MERCHANT_METRICS_QUERY = "merchant_metrics";

    /**
     * Cache storing predefined queries by their key
     */
    private static Map<String, String> queryCache = new HashMap<>();

    static {
        // Initialize query cache with correct table names and schema
        queryCache.put(TOP_BANKS_QUERY,
                "SELECT ab.bank_name, " +
                        "COUNT(t.transaction_id) as total_transactions, " +
                        "SUM(t.amount) as total_amount, " +
                        "(SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*)) * 100 as success_rate " +
                        "FROM AcquiringBank ab " +
                        "JOIN Transaction t ON ab.acquiring_bank_id = t.acquiring_bank_id " +
                        "WHERE t.timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                        "GROUP BY ab.bank_name " +
                        "ORDER BY total_amount DESC");

        queryCache.put(CARD_TYPE_VOLUME_QUERY,
                "SELECT c.card_type, " +
                        "COUNT(t.transaction_id) as transaction_count, " +
                        "SUM(t.amount) as total_amount, " +
                        "AVG(t.amount) as average_amount " +
                        "FROM Card c " +
                        "JOIN Transaction t ON c.card_id = t.card_id " +
                        "WHERE t.timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                        "GROUP BY c.card_type");

        queryCache.put(MERCHANT_METRICS_QUERY,
                "SELECT pm.merchant_name, pm.merchant_category, " +
                        "COUNT(t.transaction_id) as total_transactions, " +
                        "SUM(t.amount) as total_amount, " +
                        "(SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*)) * 100 as success_rate " +
                        "FROM PaymentMerchant pm " +
                        "JOIN Transaction t ON pm.merchant_id = t.merchant_id " +
                        "WHERE t.timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                        "GROUP BY pm.merchant_name, pm.merchant_category " +
                        "ORDER BY total_amount DESC");
    }

    /**
     * Execute a predefined query identified by its key.
     * <p>
     * This method retrieves a query from the cache using the provided key and
     * executes it against the database using the given connection. If the query
     * key is not found in the cache, a SQLException is thrown.
     * </p>
     *
     * @param queryKey The key identifying the predefined query to execute
     * @param conn     The database connection to use
     * @return The ResultSet containing the query results
     * @throws SQLException If the query key is not found or a database error occurs
     */
    public static ResultSet executePredefinedQuery(String queryKey, Connection conn) throws SQLException {
        String query = queryCache.get(queryKey);
        if (query == null) {
            throw new SQLException("Query not found: " + queryKey);
        }

        PreparedStatement stmt = conn.prepareStatement(query);
        return stmt.executeQuery();
    }

    /**
     * Execute a database analytics function with the provided parameters.
     * <p>
     * This method constructs a SQL query to call a stored function in the database
     * with the specified parameters. It properly handles parameter binding and
     * prepares the statement for execution.
     * </p>
     *
     * @param functionName The name of the database function to call
     * @param params       Variable number of parameters to pass to the function
     * @return The ResultSet containing the function result
     * @throws SQLException If a database error occurs during execution
     */
    public static ResultSet executeAnalyticsFunction(String functionName, Object... params) throws SQLException {
        Connection conn = dbManager.getConnection();
        try {
            String query = "SELECT " + functionName + "(";

            // Add parameters
            for (int i = 0; i < params.length; i++) {
                query += "?";
                if (i < params.length - 1) {
                    query += ", ";
                }
            }
            query += ") as result";

            PreparedStatement stmt = conn.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeQuery();
        } finally {
            // Note: Don't close the connection here as it might be reused
            // The connection should be closed by the calling code
        }
    }

    /**
     * Convert a ResultSet to a List of Maps for easier data processing.
     * <p>
     * This utility method transforms a JDBC ResultSet into a List of Map objects,
     * where each map represents a row from the result set with column names as keys
     * and column values as values. This makes ResultSet data easier to work with in
     * Java code.
     * </p>
     *
     * @param rs The ResultSet to convert
     * @return A List of Maps where each Map represents a row of data
     * @throws SQLException If a database error occurs during conversion
     */
    public static List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            result.add(row);
        }

        return result;
    }
} 