package com.templeosrs.util.collections.services;

import com.templeosrs.util.collections.CollectionLogRequestManager;
import com.templeosrs.util.collections.data.CollectionResponse;
import com.templeosrs.util.collections.database.CollectionDatabase;
import com.templeosrs.util.collections.parser.CollectionParser;
import com.templeosrs.util.collections.utils.PlayerNameUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class CollectionLogService {
    @Inject
    private CollectionLogRequestManager collectionLogRequestManager;

    @Inject
    private ScheduledExecutorService scheduledExecutorService;

    @Inject
    private Client client;

    @Inject
    private CollectionParser collectionParser;

    /**
     * Compares the timestamp of the latest collection log to the saved data.
     * @param username The username to check
     * @return True if the saved data is the latest available.
     */
    public boolean isDataFresh(String username)
    {
        String lastChanged = collectionLogRequestManager.getLastChangedTimestamp(username);
        Timestamp dbTimestamp = CollectionDatabase.getLatestTimestamp(username);
        Timestamp apiTimestamp = lastChanged != null ? Timestamp.valueOf(lastChanged) : null;

        log.debug("🕒 [Compare] {} | DB: {} | API: {}", username, dbTimestamp, apiTimestamp);

        return dbTimestamp != null && !dbTimestamp.before(apiTimestamp);
    }

    /**
     * Synchronises the player's cached collection log.
     */
    public void syncCollectionLog() {
        scheduledExecutorService.execute(() -> {
            log.debug("🔄 Starting syncCollectionLog()...");

            CollectionDatabase.clearAll();

            if (client.getLocalPlayer() == null) {
                log.warn("⚠️ Local player is null — not logged in yet.");
                return;
            }

            String username = Objects.requireNonNull(client.getLocalPlayer().getName()).toLowerCase();

            log.debug("👤 Detected username: {}", username);

            String json = collectionLogRequestManager.getPlayerCollectionLog(username);

            log.debug("📥 Fetched JSON: {} characters", json != null ? json.length() : 0);

            if (json == null || json.isEmpty()) {
                log.error("❌ Empty or null response from Temple API");
                return;
            }

            log.debug("🧩 Parsing and storing JSON...");

            final String normalizedPlayerName = PlayerNameUtils.normalizePlayerName(username);
            final Map<String, List<CollectionResponse.ItemEntry>> items = collectionParser.parse(
                    normalizedPlayerName,
                    json
            );

            collectionParser.store(normalizedPlayerName, items);

            log.debug("✅ Parsing complete.");
        });
    }
}
