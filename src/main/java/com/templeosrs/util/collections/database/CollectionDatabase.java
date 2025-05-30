package com.templeosrs.util.collections.database;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.templeosrs.util.collections.data.CollectionItem;
import com.templeosrs.util.collections.data.CollectionResponse;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.game.ItemVariationMapping;

import javax.inject.Singleton;
import java.io.File;
import java.sql.*;
import java.util.*;

@Slf4j
@Singleton
public class CollectionDatabase {
    private static final String DB_URL = "jdbc:h2:file:" + RuneLite.RUNELITE_DIR + "/templeosrs/runelite-collections;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";

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
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS collection_log(" +
                        "id IDENTITY PRIMARY KEY, " +
                        "category VARCHAR(255), " +
                        "item_id INT, " +
                        "item_count INT, " +
                        "item_name VARCHAR(255)," +
                        "collected_date TIMESTAMP" +
                        ")");

                addColumnIfNotExists(conn, "collection_log", "player_name", "VARCHAR(255)");
                addColumnIfNotExists(conn, "collection_log", "last_accessed", "TIMESTAMP");
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
        String sql = "SELECT 1 FROM collection_log WHERE player_name = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.warn("Error checking player data: {}", e.getMessage());
            return false;
        }
    }

    public static void insertItemsBatch(String playerName, String category, List<CollectionResponse.ItemEntry> items) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO collection_log (player_name, category, item_id, item_name, item_count, collected_date, last_accessed) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                for (CollectionResponse.ItemEntry item : items) {
                    ps.setString(1, playerName.toLowerCase());
                    ps.setString(2, category);
                    ps.setInt(3, item.id);
                    ps.setString(4, item.name);
                    ps.setInt(5, item.count);
                    ps.setTimestamp(6, Timestamp.valueOf(item.date));
                    ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            log.warn("Error inserting items batch: {}", e.getMessage());
        }
    }

    public static Multiset<Integer> findUpdatedItems(String playerName, Multiset<Integer> collectionLogItems)
    {
        try (Connection conn = getConnection())
        {
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT item_count, item_id FROM collection_log WHERE player_name = ? AND item_id = ? OR item_id = ? LIMIT 1"))
            {
                final Multiset<Integer> foundItems = HashMultiset.create();

                for (Multiset.Entry<Integer> entry : collectionLogItems.entrySet())
                {
                    final int itemId = entry.getElement();
                    final int baseItemId = ItemVariationMapping.map(itemId);

                    if (foundItems.contains(baseItemId)) {
                        foundItems.add(itemId, collectionLogItems.count(itemId));

                        continue;
                    }

                    ps.setString(1, playerName.toLowerCase());
                    ps.setInt(2, itemId);
                    ps.setInt(3, baseItemId);

                    final ResultSet rs = ps.executeQuery();

                    if (rs.next())
                    {
                        final int rsItemCount = rs.getInt("item_count");
                        final int rsItemId = rs.getInt("item_id");

                        foundItems.add(rsItemId, rsItemCount);

                        if (rsItemId != itemId) {
                            foundItems.add(itemId, rsItemCount);
                        }
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
        String sql = "SELECT MAX(collected_date) FROM collection_log WHERE player_name = ?";
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
            stmt.executeUpdate("DELETE FROM collection_log");
        } catch (SQLException e) {
            log.warn("Error clearing all items: {}", e.getMessage());
        }
    }

    public static List<CollectionItem> getItemsByCategory(String playerName, String category) {
        List<CollectionItem> items = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT item_id, item_name, item_count, collected_date FROM collection_log WHERE category = ? AND player_name = ?")) {
            ps.setString(1, category);
            ps.setString(2, playerName.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("item_id");
                    String name = rs.getString("item_name");
                    int count = rs.getInt("item_count");
                    Timestamp date = rs.getTimestamp("collected_date");

                    items.add(new CollectionItem(category, itemId, name, count, date));
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
                             "FROM collection_log " +
                             "WHERE player_name != ? " +
                             "GROUP BY player_name " +
                             "ORDER BY oldest ASC"
             );
             PreparedStatement deleteStmt = conn.prepareStatement(
                     "DELETE FROM collection_log WHERE player_name = ?"
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
