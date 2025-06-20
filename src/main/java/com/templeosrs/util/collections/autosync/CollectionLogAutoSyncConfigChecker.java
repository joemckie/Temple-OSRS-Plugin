package com.templeosrs.util.collections.autosync;

import com.templeosrs.TempleOSRSPlugin;
import com.templeosrs.util.collections.CollectionLogManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.util.Set;

@Slf4j
public class CollectionLogAutoSyncConfigChecker {
    @Inject
    private Client client;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private TempleOSRSPlugin templeOSRSPlugin;

    @Inject
    private CollectionLogManager collectionLogManager;

    @Inject
    private LoggedInState loggedInState;

    private int lastShownCollectionLogSettingWarningTick = -1;

    /**
     * Potential values are:
     * 0 = no notification
     * 1 = chat notification only
     * 2 = popup notification only
     * 3 = chat and popup
     * <p>
     * We only care about 1 and 3 to indicate chat message notifications are enabled.
     */
    private static final Set<Integer> enabledCollectionLogNotificationSettingValues = Set.of(1, 3);

    void startUp()
    {
        if (client.getGameState() == GameState.LOGGED_IN) {
            collectionLogManager.getClientThread().invoke(() -> {
                checkAndWarnForCollectionLogNotificationSetting(client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM));
            });
        }
    }

    void shutDown()
    {
        lastShownCollectionLogSettingWarningTick = -1;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState gameState = event.getGameState();

        if (gameState == GameState.CONNECTION_LOST || gameState == GameState.HOPPING) {
            lastShownCollectionLogSettingWarningTick = client.getTickCount(); // avoid warning during DC or hopping
        }

        if (gameState == GameState.LOGIN_SCREEN) {
            lastShownCollectionLogSettingWarningTick = -1;
        }
    }

    /**
     * Checks for OPTION_COLLECTION_NEW_ITEM varbit value changes and runs the warning message check.
     * This is also ran when logging in, as the game doesn't load varbits immediately.
     * Don't read varbits during a world hop because they seem to always be 0
     */
    @Subscribe
    private void onVarbitChanged(VarbitChanged varbitChanged) {
        if (varbitChanged.getVarbitId() == VarbitID.OPTION_COLLECTION_NEW_ITEM && client.getGameState() != GameState.HOPPING) {
            checkAndWarnForCollectionLogNotificationSetting(varbitChanged.getValue());
        }
    }

    /**
     * If the auto-sync clog option is enabled, the warning has not been shown recently, and the
     * clog notification option is disabled, shows a warning to the player to tell them to enable the in-game option.
     */
    private void checkAndWarnForCollectionLogNotificationSetting(int collectionLogOptionVarbitValue) {
        if (
                !templeOSRSPlugin.getConfig().autoSyncClog() ||
                loggedInState.isLoggedOut() ||
                (lastShownCollectionLogSettingWarningTick != -1 &&
                        client.getTickCount() - lastShownCollectionLogSettingWarningTick < 16) ||
                enabledCollectionLogNotificationSettingValues.contains(collectionLogOptionVarbitValue)
        ) {
            return;
        }

        lastShownCollectionLogSettingWarningTick = client.getTickCount();
        sendEnableCollectionLogSettingsMessage();
    }

    private void sendEnableCollectionLogSettingsMessage() {
        String highlightedMessage = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append("Please enable \"Collection log - New addition notification\" in your game settings for " +
                        "TempleOSRS to automatically sync your collection log!")
                .build();

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(highlightedMessage)
                .build());
    }
}
