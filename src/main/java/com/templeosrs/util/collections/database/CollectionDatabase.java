package com.templeosrs.util.collections.database;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.inject.Singleton;
import java.io.File;
import java.sql.*;
import java.util.*;

@Slf4j
@Singleton
public class CollectionDatabase {
    private static final String DB_URL = "jdbc:h2:file:" + RuneLite.RUNELITE_DIR + "/templeosrs/runelite-collections;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";

    private static final String PLAYER_COLLECTION_LOG_TABLE = "player_collection_log";
    private static final String API_CACHE_TABLE_NAME = "api_cache";

    static {
        File pluginDir = new File(RuneLite.RUNELITE_DIR, "templeosrs");
        if (!pluginDir.exists()) {
            if (!pluginDir.mkdirs()) {
                log.warn("⚠️ Failed to create plugin directory at {}", pluginDir.getAbsolutePath());
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void init() {
        try {
            // 🚨 Required for Plugin Hub: explicitly load the H2 JDBC driver
            Class.forName("org.h2.Driver");

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                final String createPlayerCollectionLogTableSql = String.format(
                        "CREATE TABLE IF NOT EXISTS %s(" +
                                "id IDENTITY PRIMARY KEY, " +
                                "item_id INT, " +
                                "item_name VARCHAR(255), " +
                                "item_count INT, " +
                                "player_name VARCHAR(255)" +
                        ")",
                        PLAYER_COLLECTION_LOG_TABLE
                );

                final String createApiCacheTableSql = String.format(
                        "CREATE TABLE IF NOT EXISTS %s(" +
                                "id IDENTITY PRIMARY KEY, " +
                                "item_id INT, " +
                                "item_name VARCHAR(255), " +
                                "item_count INT, " +
                                "player_name VARCHAR(255), " +
                                "collected_date TIMESTAMP, " +
                                "last_accessed TIMESTAMP" +
                        ")",
                        API_CACHE_TABLE_NAME
                );

                stmt.executeUpdate(createPlayerCollectionLogTableSql);
                stmt.executeUpdate(createApiCacheTableSql);
            }
        } catch (ClassNotFoundException e) {
            log.warn("H2 Driver class not found: {}", e.getMessage());
        } catch (SQLException e) {
            log.warn("Database initialization failed: {}", e.getMessage());
        }
    }

    private static void addColumnIfNotExists(Connection conn, String table, String column, String type) throws SQLException {
        String checkQuery = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
            ps.setString(1, table.toUpperCase());
            ps.setString(2, column.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                    }
                }
            }
        }
    }

    public static boolean hasPlayerData(String playerName) {
        String sql = String.format("SELECT 1 FROM %s WHERE player_name = ? LIMIT 1", API_CACHE_TABLE_NAME);

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.warn("Error checking player data: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Saves the current player's collection log to the Player Collection Log table.
     * Note: this differs from the API cache data as it is designed to replicate the in-game log
     * i.e. it only saves item IDs and their counts.
     * It is primarily used to compute a diff of items to enable the auto-sync functionality.
     *
     * @param playerName The player name associated with the data.
     * @param items The item set to persist to the database.
     */
    // TODO: Delete items that are no longer in the collection log (in cases of rollbacks)
    public static void upsertPlayerCollectionLogItems(String playerName, Set<ObtainedCollectionItem> items)
    {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
            String.format("MERGE INTO %s USING DUAL ", PLAYER_COLLECTION_LOG_TABLE) +
                "ON item_id = ? AND player_name = ? " +
                "WHEN MATCHED THEN UPDATE SET item_count = ? " +
                "WHEN NOT MATCHED THEN INSERT (player_name, item_id, item_count, item_name) VALUES (?, ?, ?, ?)"
            ))
            {
                for (ObtainedCollectionItem item : items) {
                    final int itemId = item.getId();
                    final int itemCount = item.getCount();
                    final String lowerPlayerName = playerName.toLowerCase();

                    ps.setInt(1, itemId);
                    ps.setInt(5, itemId);

                    ps.setInt(3, itemCount);
                    ps.setInt(6, itemCount);

                    ps.setString(2, lowerPlayerName);
                    ps.setString(4, lowerPlayerName);

                    ps.setString(7, item.getName());

                    ps.addBatch();
                }

                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            log.warn("Error inserting items from game data: {}", e.getMessage());
        }
    }

    /**
     * Saves the API response data to the API cache table
     * @param playerName The player name associated with the response
     * @param items The items to persist to the database
     */
    public static void insertItemsBatch(String playerName, Set<ObtainedCollectionItem> items) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                String.format(
                    "INSERT INTO %s (player_name, item_id, item_count, item_name, collected_date, last_accessed) VALUES (?, ?, ?, ?, ?, ?)",
                    API_CACHE_TABLE_NAME
                )
            ))
            {
                for (ObtainedCollectionItem item : items) {
                    ps.setString(1, playerName.toLowerCase());
                    ps.setInt(2, item.getId());
                    ps.setInt(3, item.getCount());
                    ps.setString(4, item.getName());
                    ps.setTimestamp(5, item.getDate());
                    ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
                    ps.addBatch();
                }

                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            log.warn("Error inserting items to the API cache: {}", e.getMessage());
        }
    }

    public static Multiset<Integer> getCollectionLogDiff(String playerName, Multiset<Integer> collectionLogItems)
    {
        try (Connection conn = getConnection())
        {
            try (PreparedStatement ps = conn.prepareStatement(
                String.format(
                    "SELECT item_count, item_id FROM %s WHERE player_name = ? AND item_id = ? LIMIT 1",
                    PLAYER_COLLECTION_LOG_TABLE
                )
            ))
            {
                final Multiset<Integer> foundItems = HashMultiset.create();

                for (Multiset.Entry<Integer> entry : collectionLogItems.entrySet())
                {
                    final int itemId = entry.getElement();

                    ps.setString(1, playerName.toLowerCase());
                    ps.setInt(2, itemId);

                    final ResultSet rs = ps.executeQuery();

                    if (rs.next())
                    {
                        final int rsItemCount = rs.getInt("item_count");
                        final int rsItemId = rs.getInt("item_id");

                        foundItems.add(rsItemId, rsItemCount);
                    }
                }

                return Multisets.difference(collectionLogItems, foundItems);
            }
        } catch (SQLException e) {
            log.warn("Error comparing collection log: {}", e.getMessage());

            return null;
        }
    }

    public static Timestamp getLatestTimestamp(String playerName) {
        String sql = String.format("SELECT MAX(collected_date) FROM %s WHERE player_name = ?", API_CACHE_TABLE_NAME);
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp(1);
            }
        } catch (SQLException e) {
            log.warn("Error fetching latest timestamp: {}", e.getMessage());
        }

        return null;
    }

    public static void clearAll() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate(String.format("DELETE FROM %s", API_CACHE_TABLE_NAME));
        } catch (SQLException e) {
            log.warn("Error clearing all items: {}", e.getMessage());
        }
    }

    public static List<ObtainedCollectionItem> getItemsByCategory(String playerName, int categoryId) {
        List<ObtainedCollectionItem> items = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 String.format(
                     "SELECT item_id, item_name, item_count, collected_date FROM %s WHERE category = ? AND player_name = ?",
                     API_CACHE_TABLE_NAME
                 )
        ))
        {
            ps.setInt(1, categoryId);
            ps.setString(2, playerName.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("item_id");
                    String itemName = rs.getString("item_name");
                    int count = rs.getInt("item_count");
                    Timestamp date = rs.getTimestamp("collected_date");

                    items.add(new ObtainedCollectionItem(itemId, itemName, count, date.toString()));
                }
            }
        } catch (SQLException e) {
            log.warn("Error fetching items by category: {}", e.getMessage());
        }

        return items;
    }

    public static void pruneOldPlayers(String yourUsername, int maxPlayers) {
        try (Connection conn = getConnection();
             PreparedStatement ps1 = conn.prepareStatement(
             "SELECT player_name, MIN(last_accessed) as oldest " +
                 String.format("FROM %s ", API_CACHE_TABLE_NAME) +
                 "WHERE player_name != ? " +
                 "GROUP BY player_name " +
                 "ORDER BY oldest ASC"
             );
             PreparedStatement deleteStmt = conn.prepareStatement(
                 String.format("DELETE FROM %s WHERE player_name = ?", API_CACHE_TABLE_NAME)
             )
        ) {
            ps1.setString(1, yourUsername.toLowerCase());
            ResultSet rs = ps1.executeQuery();

            int count = 0;
            List<String> playersToRemove = new ArrayList<>();

            while (rs.next()) {
                count++;
                if (count > maxPlayers) {
                    playersToRemove.add(rs.getString("player_name"));
                }
            }

            for (String name : playersToRemove) {
                log.debug("\uD83E\uDDF9 Pruning cached player: {}", name);
                deleteStmt.setString(1, name);
                deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {
            log.warn("Error pruning old players: {}", e.getMessage());
        }
    }
}
