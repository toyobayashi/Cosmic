package net.server.task;

import client.Character;
import net.server.world.World;
import server.quest.DailyHuntQuest;

public class DailyHuntResetTask implements Runnable {
    private final World world;

    public DailyHuntResetTask(World world) {
        this.world = world;
    }

    @Override
    public void run() {
        for (Character chr : world.getPlayerStorage().getAllCharacters()) {
            DailyHuntQuest.refreshForCurrentDay(chr);
        }
    }
}
