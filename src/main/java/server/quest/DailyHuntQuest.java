package server.quest;

import client.Character;
import client.QuestStatus;
import server.life.LifeFactory;
import tools.PacketCreator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DailyHuntQuest {
    public static final int QUEST_ID = 29099;
    public static final int TARGET_MOB_ID = 9101999;
    public static final int DISPLAY_NPC_ID = 9010010;
    public static final int MIN_LEVEL = 30;
    public static final int MAX_LEVEL = 200;
    public static final int REQUIRED_KILLS = 100;

    private static final int LEVEL_RANGE = 10;
    private static final ZoneId RESET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int[] REWARD_EXP = {
            2389, 5135, 9978, 16621, 22231, 34274, 53685, 70968, 86293,
            129588, 152033, 227774, 313225, 421160, 555001, 678029, 920365
    };

    private DailyHuntQuest() {
    }

    public static int getRewardExp(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            return 0;
        }
        return REWARD_EXP[Math.min((level - MIN_LEVEL) / 10, REWARD_EXP.length - 1)];
    }

    public static long getStartOfTodayMillis() {
        return LocalDate.now(RESET_ZONE).atStartOfDay(RESET_ZONE).toInstant().toEpochMilli();
    }

    public static long getMillisUntilNextReset() {
        ZonedDateTime now = ZonedDateTime.now(RESET_ZONE);
        ZonedDateTime nextReset = now.toLocalDate().plusDays(1).atStartOfDay(RESET_ZONE);
        return Duration.between(now, nextReset).toMillis();
    }

    public static void refreshForCurrentDay(Character chr) {
        synchronized (chr) {
            Quest quest = Quest.getInstance(QUEST_ID);
            QuestStatus status = chr.getQuestNoAdd(quest);
            if (status == null || status.getStatus() == QuestStatus.Status.NOT_STARTED) {
                return;
            }

            if (status.getCompletionTime() < getStartOfTodayMillis()) {
                quest.reset(chr);
            }
        }
    }

    public static void onMobKilled(Character chr, int mobId) {
        synchronized (chr) {
            refreshForCurrentDay(chr);

            Quest quest = Quest.getInstance(QUEST_ID);
            QuestStatus status = chr.getQuestNoAdd(quest);
            if (status == null || status.getStatus() != QuestStatus.Status.STARTED) {
                return;
            }

            int mobLevel = LifeFactory.getMonsterLevel(mobId);
            if (mobLevel < 0 || Math.abs(chr.getLevel() - mobLevel) > LEVEL_RANGE) {
                return;
            }

            if (!status.progress(TARGET_MOB_ID)) {
                return;
            }

            chr.announceUpdateQuest(Character.DelayedQuestUpdate.UPDATE, status, false);
            if (quest.canComplete(chr, DISPLAY_NPC_ID)) {
                chr.sendPacket(PacketCreator.getShowQuestCompletion(QUEST_ID));
            }
        }
    }

    public static boolean isReadyToComplete(Character chr) {
        synchronized (chr) {
            refreshForCurrentDay(chr);
            Quest quest = Quest.getInstance(QUEST_ID);
            QuestStatus status = chr.getQuestNoAdd(quest);
            return status != null
                    && status.getStatus() == QuestStatus.Status.STARTED
                    && quest.canComplete(chr, DISPLAY_NPC_ID);
        }
    }

    public static void showPendingCompletion(Character chr) {
        if (isReadyToComplete(chr)) {
            chr.sendPacket(PacketCreator.getShowQuestCompletion(QUEST_ID));
        }
    }
}
