package com.toofifty.goaltracker.goal;

import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import lombok.Setter;

public class ManualTask extends Task
{
    @Setter
    private Boolean done = false;

    @Setter
    private String description;

    public ManualTask(Goal goal)
    {
        super(goal);
    }

    public void toggle()
    {
        done = !done;
    }

    @Override
    public boolean check()
    {
        return result = done;
    }

    @Override
    public String toString()
    {
        return description;
    }

    @Override
    public TaskType getType()
    {
        return TaskType.MANUAL;
    }

    @Override
    public JsonObject addSerializedProperties(JsonObject json)
    {
        json.addProperty("done", done);
        json.addProperty("description", description);
        return json;
    }

    @Override
    public BufferedImage getIcon()
    {
        return null;
    }
}
