package com.templeosrs.collectionlog.autosync;

import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CollectionLogAutoSyncConfigCheckerTest extends MockedCollectionLogAutoSyncTest
{
    @Test
    @DisplayName("Ensure a warning is not displayed on login when the \"new collection log\" option is enabled")
    void doesNotEmitWarningWhenNewCollectionLogOptionIsEnabled()
    {
        setCollectionLogOptionValue(1);

        logIn();

        verify(chatMessageManager, never()).queue(any(QueuedMessage.class));
    }

    @Test
    @DisplayName("Ensure a warning is displayed on login when the \"new collection log\" option is disabled")
    void emitsWarningWhenNewCollectionLogOptionIsDisabled()
    {
        logIn();
        setCollectionLogOptionValue(0);

        verifyChatMessageHasBeenDisplayedTimes(1);
    }

    @Test
    @DisplayName("Ensure a warning is not displayed if it has already been shown for the current session")
    void doesNotEmitWarningWhenWarningHasBeenShownForCurrentSession()
    {
        logIn();
        setCollectionLogOptionValue(0);

        verifyChatMessageHasBeenDisplayedTimes(1);

        hopWorld();
        verifyChatMessageHasBeenDisplayedTimes(1);
    }

    @Test
    @DisplayName("Ensure a warning is displayed when starting a new login session when the \"new collection log\"  option is disabled")
    void emitsWarningWhenStartingNewSession()
    {
        logIn();
        setCollectionLogOptionValue(0);

        verifyChatMessageHasBeenDisplayedTimes(1);

        logOut();
        logIn();
        setCollectionLogOptionValue(0);
        verifyChatMessageHasBeenDisplayedTimes(2);
    }

    private void verifyChatMessageHasBeenDisplayedTimes(int expectedTimes)
    {
        String highlightedMessage = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append("Please enable \"Collection log - New addition notification\" in your game settings for " +
                        "TempleOSRS to automatically sync your collection log!")
                .build();

        final QueuedMessage expectedQueuedMessage = QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(highlightedMessage)
                .build();

        verify(chatMessageManager, times(expectedTimes)).queue(expectedQueuedMessage);
    }
}
