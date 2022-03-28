package com.toofifty.goaltracker;

import com.google.inject.Provides;
import com.toofifty.goaltracker.goal.*;
import com.toofifty.goaltracker.ui.GoalTrackerPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
@PluginDescriptor(name = "Goal Tracker")
public class GoalTrackerPlugin extends Plugin
{
    @Getter
    @Inject
    private Client client;

    @Getter
    @Inject
    private SkillIconManager skillIconManager;

    @Getter
    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Getter
    @Inject
    private GoalTrackerConfig config;

    @Getter
    private GoalManager goalManager;

    private NavigationButton uiNavigationButton;

    private GoalTrackerPanel goalTrackerPanel;

    @Setter
    private boolean validateAll = true;

    @Provides
    GoalTrackerConfig getGoalTrackerConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GoalTrackerConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        goalManager = new GoalManager(this);
        goalManager.load();

        final BufferedImage icon = itemManager.getImage(ItemID.TODO_LIST);
        goalTrackerPanel = new GoalTrackerPanel(this);

        uiNavigationButton = NavigationButton.builder().tooltip("Goal Tracker")
                                             .icon(icon).priority(7).panel(
                goalTrackerPanel).build();

        clientToolbar.addNavigation(uiNavigationButton);
    }

    @Override
    protected void shutDown() throws Exception
    {
        goalManager.save();

        clientToolbar.removeNavigation(uiNavigationButton);
    }

    @Subscribe
    public void onSessionOpen(SessionOpen event)
    {
        goalManager.load();
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        List<SkillLevelTask> skillLevelTasks = goalManager
            .getAllIncompleteTasksOfType(TaskType.SKILL_LEVEL);
        for (SkillLevelTask task : skillLevelTasks) {
            if (event.getSkill() == task.getSkill() && event.getLevel() >= task
                .getLevel()) {
                notifyTask(task);
                TaskUIStatusManager.getInstance().refresh(task);
            }
        }

        List<SkillXpTask> skillXpTasks = goalManager
            .getAllIncompleteTasksOfType(TaskType.SKILL_XP);
        for (SkillXpTask task : skillXpTasks) {
            if (event.getSkill() == task.getSkill() && event.getXp() >= task
                .getXp()) {
                notifyTask(task);
                TaskUIStatusManager.getInstance().refresh(task);
            }
        }
    }

    public void notifyTask(Task task)
    {
        if (client.getGameState() != GameState.LOGGED_IN || task
            .hasBeenNotified()) {
            return;
        }

        System.out.println(
            "Notify: " + "[Goal Tracker] You have completed a task: " + task
                .toString() + "!");

        String message = "[Goal Tracker] You have completed a task: " + task
            .toString() + "!";
        String formattedMessage = new ChatMessageBuilder().append(
            ColorScheme.PROGRESS_COMPLETE_COLOR, message).build();
        chatMessageManager.queue(QueuedMessage.builder().type(
            ChatMessageType.CONSOLE).name("Goal Tracker")
                                              .runeLiteFormattedMessage(
                                                  formattedMessage).build());

        task.hasBeenNotified(true);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (client.getGameState() != GameState.LOGGED_IN) return;

        // redo the login check on the next game tick
        validateAll = true;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!validateAll) return;
        if (client.getGameState() != GameState.LOGGED_IN) return;

        validateAll = false;
        // perform a full refresh just once on login
        // onGameStateChanged reports incorrect quest statuses,
        // so this need to be done in this subscriber
        goalTrackerPanel.refresh();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        // attempt to refresh
        if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getMessage()
                                                                   .contains(
                                                                       "Quest complete")) {

            List<QuestTask> questTasks = goalManager
                .getAllIncompleteTasksOfType(TaskType.QUEST);
            for (QuestTask task : questTasks) {
                if (task.check()) {
                    notifyTask(task);
                    TaskUIStatusManager.getInstance().refresh(task);
                }
            }
        }
    }
}
