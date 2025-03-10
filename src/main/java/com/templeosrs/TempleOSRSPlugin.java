/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.templeosrs;

import com.google.gson.Gson; // Collection Log
import com.google.gson.JsonParseException; // Collection Log
import com.google.inject.Provides;
import com.templeosrs.ui.TempleOSRSPanel;
import com.templeosrs.ui.clans.TempleClans;
import com.templeosrs.ui.competitions.TempleCompetitions;
import com.templeosrs.ui.ranks.TempleRanks;
import com.templeosrs.util.TempleService;
import com.templeosrs.util.collections.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread; // Collection Log
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType; // Collection Log
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.xpupdater.XpUpdaterConfig;
import net.runelite.client.plugins.xpupdater.XpUpdaterPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import okhttp3.*;

// ------------------ Collection Log Related
import net.runelite.client.task.Schedule;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@PluginDependency(XpUpdaterPlugin.class)
@PluginDescriptor(name = "TempleOSRS", description = "A RuneLite plugin utilizing the TempleOSRS API.", tags = {"Temple", "ehp", "ehb"})
public class TempleOSRSPlugin extends Plugin {
    private static final String TEMPLE = "Temple";

    private static final int XP_THRESHOLD = 10000;

    private static NavigationButton navButton;

    public TempleRanks ranks;

    public TempleClans clans;

    public TempleCompetitions competitions;

    public TempleOSRSPanel panel;

    private long lastAccount;

    private boolean fetchXp;

    private long lastXp;

    // ------------------ Collection Log variables
    private static final int SECONDS_BETWEEN_UPLOADS = 30;
    private static final int SECONDS_BETWEEN_MANIFEST_CHECKS = 1200;
    private final int VARBITS_ARCHIVE_ID = 14;
    private final String CONFIG_GROUP = "TempleOSRS";
    private static final String PLUGIN_USER_AGENT = "TempleOSRS RuneLite Plugin Collection Log Sync - For any issues/abuse Contact 44mikael on Discord (https://www.templeosrs.com)";

    private static final String MANIFEST_URL = "https://templeosrs.com/collection-log/manifest.json";
    private static final String SUBMIT_URL = "https://templeosrs.com/api/collection-log/sync_collection.php";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private Map<Integer, VarbitComposition> varbitCompositions = new HashMap<>();

    private Manifest manifest;
    private Map<PlayerProfile, PlayerData> playerDataMap = new HashMap<>();
    private int cyclesSinceSuccessfulCall = 0;

    // Keeps track of what collection log slots the user has set.
    private static final BitSet clogItemsBitSet = new BitSet();
    private static Integer clogItemsCount = null;

    // Map item ids to bit index in the bitset
    private static final HashMap<Integer, Integer> collectionLogItemIdToBitsetIndex = new HashMap<>();
    private int tickCollectionLogScriptFired = -1;
    private final HashSet<Integer> collectionLogItemIdsFromCache = new HashSet<>();

    @Inject
    private SyncButtonManager syncButtonManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ScheduledExecutorService scheduledExecutorService;

    @Inject
    private Gson gson;

    // ------------------ Collection Log variables end

    @Inject
    private Client client;

    @Inject
    private Provider<MenuManager> menuManager;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private TempleOSRSConfig config;

    @Inject
    private XpUpdaterConfig xpUpdaterConfig;

    @Inject
    private XpUpdaterPlugin xpUpdaterPlugin;

    @Inject
    private TempleService service;

    @Override
    protected void startUp() {
        fetchXp = true;

        lastAccount = -1L;

        ranks = injector.getInstance(TempleRanks.class);

        clans = injector.getInstance(TempleClans.class);

        competitions = injector.getInstance(TempleCompetitions.class);

        panel = new TempleOSRSPanel(ranks, clans, competitions);

        navButton = NavigationButton.builder().tooltip("TempleOSRS").icon(ImageUtil.loadImageResource(TempleOSRSPlugin.class, "skills/skill_icon_ehp.png")).priority(5).panel(panel).build();

        clientToolbar.addNavigation(navButton);

        if (config.playerLookup() && client != null) {
            menuManager.get().addPlayerMenuItem(TEMPLE);
        }

        // ------------------ Collection Log Related
        clientThread.invoke(() -> {
            if (client.getIndexConfig() == null || client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal()) {
                return false;
            }
            collectionLogItemIdsFromCache.addAll(parseCacheForClog());
            populateCollectionLogItemIdToBitsetIndex();
            final int[] varbitIds = client.getIndexConfig().getFileIds(VARBITS_ARCHIVE_ID);
            for (int id : varbitIds) {
                varbitCompositions.put(id, client.getVarbit(id));
            }
            return true;
        });

        checkManifest();

        // Only display clog update button if enabled
        if (config.clogSyncButton()) {
            syncButtonManager.startUp();
        }
        // ------------------
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);

        if (client != null) {
            menuManager.get().removePlayerMenuItem(TEMPLE);
        }
        ranks.shutdown();

        clogItemsBitSet.clear();
        clogItemsCount = null;
        syncButtonManager.shutDown();
    }


    // -------------------- Collection Log Methods
    // ----------------
    // ------------

    /**
     * Finds the index this itemId is assigned to in the collections mapping.
     *
     * @param itemId: The itemId to look up
     * @return The index of the bit that represents the given itemId, if it is in the map. -1 otherwise.
     */
    private int lookupCollectionLogItemIndex(int itemId) {
        // The map has not loaded yet, or failed to load.
        if (collectionLogItemIdToBitsetIndex.isEmpty()) {
            return -1;
        }
        Integer result = collectionLogItemIdToBitsetIndex.get(itemId);
        if (result == null) {
//			log.debug("Item id {} not found in the mapping of items", itemId);
            return -1;
        }
        return result;
    }

    //CollectionLog Subscribe
    @Subscribe
    public void onScriptPreFired(ScriptPreFired preFired) {
        if (syncButtonManager.isSyncAllowed() && preFired.getScriptId() == 4100) {
            tickCollectionLogScriptFired = client.getTickCount();
            if (collectionLogItemIdToBitsetIndex.isEmpty()) {
                return;
            }
            clogItemsCount = collectionLogItemIdsFromCache.size();
            Object[] args = preFired.getScriptEvent().getArguments();
            int itemId = (int) args[1];
            int idx = lookupCollectionLogItemIndex(itemId);
            // We should never return -1 under normal circumstances
            if (idx != -1)
                clogItemsBitSet.set(idx);
        }
    }

    @Schedule(
            period = SECONDS_BETWEEN_UPLOADS,
            unit = ChronoUnit.SECONDS,
            asynchronous = true
    )
    public void queueSubmitTask() {
        scheduledExecutorService.execute(this::submitTask);
    }

    synchronized public void submitTask() {
        // If sync hasn't been toggled to be allowed
        if (!syncButtonManager.isSyncAllowed()) {
            return;
        }

        // TODO: do we want other GameStates?
        if (client.getGameState() != GameState.LOGGED_IN || varbitCompositions.isEmpty()) {
            return;
        }

        if (manifest == null || client.getLocalPlayer() == null) {
//			log.debug("Skipped due to bad manifest: {}", manifest);
            return;
        }

        String username = client.getLocalPlayer().getName();
        RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
        PlayerProfile profileKey = new PlayerProfile(username, profileType);

        PlayerData newPlayerData = getPlayerData();
        PlayerData oldPlayerData = playerDataMap.computeIfAbsent(profileKey, k -> new PlayerData());

        // Subtraction is done in place so newPlayerData becomes a map of only changed fields
        subtract(newPlayerData, oldPlayerData);
        if (newPlayerData.isEmpty()) {
            return;
        }

        // Do not send if slot data wasn't generated
        if (newPlayerData.collectionLogSlots.isEmpty()) {
            return;
        }


        submitPlayerData(profileKey, newPlayerData, oldPlayerData);
    }

    public void manifestTask() {
        if (client.getGameState() == GameState.LOGGED_IN) {
            checkManifest();
        }
    }

    private PlayerData getPlayerData() {
        PlayerData out = new PlayerData();
        for (int varbitId : manifest.varbits) {
            out.varb.put(varbitId, getVarbitValue(varbitId));
        }
        for (int varpId : manifest.varps) {
            out.varp.put(varpId, client.getVarpValue(varpId));
        }
        for (Skill s : Skill.values()) {
            out.level.put(s.getName(), client.getRealSkillLevel(s));
        }

        out.collectionLogSlots = Base64.getEncoder().encodeToString(clogItemsBitSet.toByteArray());
        out.collectionLogItemCount = clogItemsCount;
        return out;
    }

    private void subtract(PlayerData newPlayerData, PlayerData oldPlayerData) {
        oldPlayerData.varb.forEach(newPlayerData.varb::remove);
        oldPlayerData.varp.forEach(newPlayerData.varp::remove);
        oldPlayerData.level.forEach(newPlayerData.level::remove);
        if (newPlayerData.collectionLogSlots.equals(oldPlayerData.collectionLogSlots))
            newPlayerData.clearCollectionLog();
    }

    private void merge(PlayerData oldPlayerData, PlayerData delta) {
        oldPlayerData.varb.putAll(delta.varb);
        oldPlayerData.varp.putAll(delta.varp);
        oldPlayerData.level.putAll(delta.level);
        oldPlayerData.collectionLogSlots = delta.collectionLogSlots;
        oldPlayerData.collectionLogItemCount = delta.collectionLogItemCount;
    }

    private void submitPlayerData(PlayerProfile profileKey, PlayerData delta, PlayerData old) {
        // If cyclesSinceSuccessfulCall is not a perfect square, we should not try to submit.
        // This gives us quadratic backoff.
        cyclesSinceSuccessfulCall += 1;
        if (Math.pow((int) Math.sqrt(cyclesSinceSuccessfulCall), 2) != cyclesSinceSuccessfulCall) {
            return;
        }

        PlayerDataSubmission submission = new PlayerDataSubmission(
                profileKey.getUsername(),
                profileKey.getProfileType().name(),
                client.getAccountHash(),
                delta
        );

        Request request = new Request.Builder()
                .addHeader("User-Agent", PLUGIN_USER_AGENT)
                .url(SUBMIT_URL)
                .post(RequestBody.create(JSON, gson.toJson(submission)))
                .build();

        Call call = okHttpClient.newCall(request);
        call.timeout().timeout(3, TimeUnit.SECONDS);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//				log.debug("Failed to submit: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
//						log.debug("Failed to submit: {}", response.code());
                        return;
                    }
                    merge(old, delta);
                    cyclesSinceSuccessfulCall = 0;
                } finally {
                    response.close();
                }
            }
        });
    }

    private void checkManifest() {
        Request request = new Request.Builder()
                .addHeader("User-Agent", PLUGIN_USER_AGENT)
                .url(MANIFEST_URL)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//				log.debug("Failed to get manifest: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
//						log.debug("Failed to get manifest: {}", response.code());
                        return;
                    }
//					log.debug("Got manifest: {}", response.body());
                    InputStream in = response.body().byteStream();
                    manifest = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Manifest.class);
                    populateCollectionLogItemIdToBitsetIndex();
                } catch (JsonParseException e) {
//					log.debug("Failed to parse manifest: ", e);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void populateCollectionLogItemIdToBitsetIndex() {
        if (manifest == null) {
//			log.debug("Manifest is not present so the collection log bitset index will not be updated");
            return;
        }
        clientThread.invoke(() -> {
            // Add missing keys in order to the map. Order is extremely important here so
            // we get a stable map given the same cache data.
            List<Integer> itemIdsMissingFromManifest = collectionLogItemIdsFromCache
                    .stream()
                    .filter((t) -> !manifest.collections.contains(t))
                    .sorted()
                    .collect(Collectors.toList());

            int currentIndex = 0;
            collectionLogItemIdToBitsetIndex.clear();
            for (Integer itemId : manifest.collections)
                collectionLogItemIdToBitsetIndex.put(itemId, currentIndex++);
            for (Integer missingItemId : itemIdsMissingFromManifest) {
                collectionLogItemIdToBitsetIndex.put(missingItemId, currentIndex++);
            }
        });
    }

    /**
     * Parse the enums and structs in the cache to figure out which item ids
     * exist in the collection log. This can be diffed with the manifest to
     * determine the item ids that need to be appended to the end of the
     * bitset we send to the TempleOSRS server.
     */
    private HashSet<Integer> parseCacheForClog() {
        HashSet<Integer> itemIds = new HashSet<>();
        // 2102 - Struct that contains the highest level tabs in the collection log (Bosses, Raids, etc)
        // https://chisel.weirdgloop.org/structs/index.html?type=enums&id=2102
        int[] topLevelTabStructIds = client.getEnum(2102).getIntVals();
        for (int topLevelTabStructIndex : topLevelTabStructIds) {
            // The collection log top level tab structs contain a param that points to the enum
            // that contains the pointers to sub tabs.
            // ex: https://chisel.weirdgloop.org/structs/index.html?type=structs&id=471
            StructComposition topLevelTabStruct = client.getStructComposition(topLevelTabStructIndex);

            // Param 683 contains the pointer to the enum that contains the subtabs ids
            // ex: https://chisel.weirdgloop.org/structs/index.html?type=enums&id=2103
            int[] subtabStructIndices = client.getEnum(topLevelTabStruct.getIntValue(683)).getIntVals();
            for (int subtabStructIndex : subtabStructIndices) {

                // The subtab structs are for subtabs in the collection log (Commander Zilyana, Chambers of Xeric, etc.)
                // and contain a pointer to the enum that contains all the item ids for that tab.
                // ex subtab struct: https://chisel.weirdgloop.org/structs/index.html?type=structs&id=476
                // ex subtab enum: https://chisel.weirdgloop.org/structs/index.html?type=enums&id=2109
                StructComposition subtabStruct = client.getStructComposition(subtabStructIndex);
                int[] clogItems = client.getEnum(subtabStruct.getIntValue(690)).getIntVals();
                for (int clogItemId : clogItems) itemIds.add(clogItemId);
            }
        }

        // Some items with data saved on them have replacements to fix a duping issue (satchels, flamtaer bag)
        // Enum 3721 contains a mapping of the item ids to replace -> ids to replace them with
        EnumComposition replacements = client.getEnum(3721);
        for (int badItemId : replacements.getKeys())
            itemIds.remove(badItemId);
        for (int goodItemId : replacements.getIntVals())
            itemIds.add(goodItemId);

        return itemIds;
    }


    private int getVarbitValue(int varbitId) {
        VarbitComposition v = varbitCompositions.get(varbitId);
        if (v == null) {
            return -1;
        }

        int value = client.getVarpValue(v.getIndex());
        int lsb = v.getLeastSignificantBit();
        int msb = v.getMostSignificantBit();
        int mask = (1 << ((msb - lsb) + 1)) - 1;
        return (value >> lsb) & mask;
    }

    // ------------
    // ----------------
    // -------------------- Collection Log Methods End

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("TempleOSRS")) {
            if (client != null) {
                menuManager.get().removePlayerMenuItem(TEMPLE);
                if (config.playerLookup()) {
                    menuManager.get().addPlayerMenuItem(TEMPLE);
                }

                if (clans.clanAchievements != null) {
                    clans.remove(clans.clanAchievements);
                    if (config.displayClanAchievements()) {
                        clans.add(clans.clanAchievements);
                    }
                }

                if (clans.clanCurrentTop != null) {
                    clans.remove(clans.clanCurrentTop);
                    if (config.displayClanCurrentTop()) {
                        clans.add(clans.clanCurrentTop);
                    }
                }

                if (clans.clanMembers != null) {
                    clans.remove(clans.clanMembers);
                    if (config.displayClanMembers()) {
                        clans.add(clans.clanMembers);
                    }
                }

                // Collection Log Related
                if (config.clogSyncButton()) {
                    syncButtonManager.startUp();
                } else {
                    syncButtonManager.shutDown();
                }

                clans.repaint();
                clans.revalidate();
            }

            competitions.rebuildWatchlist();
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if ((event.getType() != MenuAction.CC_OP.getId() && event.getType() != MenuAction.CC_OP_LOW_PRIORITY.getId()) || !config.playerLookup()) {
            return;
        }

        String username = Text.toJagexName(Text.removeTags(event.getTarget()).toLowerCase().trim());

        final String option = event.getOption();
        final int componentId = event.getActionParam1();
        final int groupId = WidgetUtil.componentToInterface(componentId);

        if (groupId == InterfaceID.FRIEND_LIST && option.equals("Delete")
                || groupId == InterfaceID.FRIENDS_CHAT && (option.equals("Add ignore") || option.equals("Remove friend"))
                || groupId == InterfaceID.CHATBOX && (option.equals("Add ignore") || option.equals("Message"))
                || groupId == InterfaceID.IGNORE_LIST && option.equals("Delete")
                || (componentId == ComponentID.CLAN_MEMBERS || componentId == ComponentID.CLAN_GUEST_MEMBERS) && (option.equals("Add ignore") || option.equals("Remove friend"))
                || groupId == InterfaceID.PRIVATE_CHAT && (option.equals("Add ignore") || option.equals("Message"))
                || groupId == InterfaceID.GROUP_IRON && (option.equals("Add friend") || option.equals("Remove friend") || option.equals("Remove ignore"))
        ) {
            client.createMenuEntry(-2).setOption(TEMPLE).setTarget(event.getTarget()).setType(MenuAction.RUNELITE).setIdentifier(event.getIdentifier()).onClick(e -> fetchUser(username));
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER && event.getMenuOption().equals(TEMPLE)) {
            Player player = event.getMenuEntry().getPlayer();
            if (player == null) {
                return;
            }

            String username = player.getName();
            fetchUser(username);
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (fetchXp) {
            lastXp = client.getOverallExperience();
            fetchXp = false;
        }

        // Submit the collection log data two ticks after the first script prefires
        if (tickCollectionLogScriptFired != -1 &&
                tickCollectionLogScriptFired + 2 > client.getTickCount()) {
            tickCollectionLogScriptFired = -1;
            if (manifest == null) {
                client.addChatMessage(ChatMessageType.CONSOLE, "TempleOSRS", "Failed to sync collection log. Try restarting the TempleOSRS plugin.", "TempleOSRS");
                return;
            }
            scheduledExecutorService.execute(this::submitTask);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        GameState state = gameStateChanged.getGameState();
        if (state == GameState.LOGGED_IN) {
            if (lastAccount != client.getAccountHash()) {
                lastAccount = client.getAccountHash();
                fetchXp = true;
            }
        } else if (state == GameState.LOGIN_SCREEN) {
            Player local = client.getLocalPlayer();
            if (local == null) {
                return;
            }

            long totalXp = client.getOverallExperience();
            String username = local.getName();

			/* Don't submit update if xp-threshold has not been reached or username is null
			   or config option for auto-update is disabled */
            if (Math.abs(totalXp - lastXp) > XP_THRESHOLD && username != null && config.autoUpdate()) {
                updateUser(lastAccount, username.replace(" ", "+"));
                lastXp = totalXp;
            }
        }

        switch (gameStateChanged.getGameState()) {
            // When hopping, we need to clear any state related to the player
            case HOPPING:
            case LOGGING_IN:
            case CONNECTION_LOST:
                clogItemsBitSet.clear();
                clogItemsCount = null;
                break;
        }
    }

    @Provides
    TempleOSRSConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TempleOSRSConfig.class);
    }

    public void fetchUser(String username) {
        SwingUtilities.invokeLater(() -> {
            clientToolbar.openPanel(navButton);

            /* select ranks-tab */
            panel.tabGroup.select(panel.ranksTab);
            ranks.fetchUser(username);
        });
    }

    public void updateUser(long accountHash, String username) {
        /* if XpUpdaterPlugin is disabled or XpUpdaterPlugin's config option for templeosrs is disabled */
        if (!pluginManager.isPluginEnabled(xpUpdaterPlugin) || !xpUpdaterConfig.templeosrs()) {
            new Thread(() -> {
                try {
                    service.addDatapointAsync(username, accountHash);
                } catch (Exception ignored) {

                }
            }).start();
        }
    }
}
