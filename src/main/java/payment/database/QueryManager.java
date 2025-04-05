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

public class QueryManager {
  private static final String ANALYTICS_QUERIES_PATH = "src/res/db/queries/analytics_queries.sql";
  private static final DatabaseManager dbManager = new DatabaseManager();

  // Predefined query keys
  public static final String TOP_BANKS_QUERY = "top_banks";
  public static final String CARD_TYPE_VOLUME_QUERY = "card_type_volume";
  public static final String MERCHANT_METRICS_QUERY = "merchant_metrics";

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

  public static ResultSet executePredefinedQuery(String queryKey, Connection conn) throws SQLException {
    String query = queryCache.get(queryKey);
    if (query == null) {
      throw new SQLException("Query not found: " + queryKey);
    }

    PreparedStatement stmt = conn.prepareStatement(query);
    return stmt.executeQuery();
  }

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