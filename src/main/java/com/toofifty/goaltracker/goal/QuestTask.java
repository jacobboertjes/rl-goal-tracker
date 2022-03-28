package com.toofifty.goaltracker.goal;

import com.google.gson.JsonObject;
import com.toofifty.goaltracker.GoalTrackerPlugin;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

public class QuestTask extends Task
{
    private static final BufferedImage QUEST_ICON;

    static {
        QUEST_ICON = ImageUtil.loadImageResource(
            GoalTrackerPlugin.class, "/quest_icon.png");
    }

    private final Client client;

    @Getter
    @Setter
    private Quest quest;

    public QuestTask(Client client, Goal goal)
    {
        super(goal);
        this.client = client;
    }

    @Override
    public boolean check()
    {
        if (client.getGameState() != GameState.LOGGED_IN || !client
            .isClientThread()) {
            return result;
        }

        return result = quest.getState(client) == QuestState.FINISHED;
    }

    @Override
    public String toString()
    {
        return quest.getName();
    }

    @Override
    public TaskType getType()
    {
        return TaskType.QUEST;
    }

    @Override
    protected JsonObject addSerializedProperties(JsonObject json)
    {
        json.addProperty("quest_id", quest.getId());
        return json;
    }

    @Override
    public BufferedImage getIcon()
    {
        return QUEST_ICON;
    }
}
