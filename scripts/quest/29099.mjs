import { createI18n } from "../lib/i18n/index.mjs";
import { messages } from "../i18n/dailyHunt.mjs";

const DailyHuntQuest = Java.type("server.quest.DailyHuntQuest");

let startStatus = -1;
let endStatus = -1;

export function start(ctx, mode) {
    const qm = ctx.qm;
    const i18n = createI18n(qm);

    if (mode !== 1) {
        qm.dispose();
        return;
    }

    startStatus++;
    if (startStatus === 0) {
        DailyHuntQuest.refreshForCurrentDay(qm.getPlayer());
        if (qm.getQuestStatus(DailyHuntQuest.QUEST_ID) !== 0) {
            qm.sendOk(i18n.t(messages.start.unavailable));
            return;
        }
        qm.sendYesNo(i18n.t(messages.start.prompt));
    } else if (startStatus === 1) {
        if (!qm.forceStartQuest(DailyHuntQuest.QUEST_ID, DailyHuntQuest.DISPLAY_NPC_ID)) {
            qm.sendOk(i18n.t(messages.start.unavailable));
            return;
        }
        qm.sendOk(i18n.t(messages.start.accepted));
    } else {
        qm.dispose();
    }
}

export function end(ctx, mode) {
    const qm = ctx.qm;
    const i18n = createI18n(qm);

    if (mode !== 1) {
        qm.dispose();
        return;
    }

    endStatus++;
    if (endStatus === 0) {
        if (!DailyHuntQuest.isReadyToComplete(qm.getPlayer())) {
            qm.sendOk(i18n.t(messages.end.expired));
            return;
        }
        const reward = DailyHuntQuest.getRewardExp(qm.getLevel());
        qm.sendYesNo(i18n.t(messages.end.prompt, { reward }));
    } else if (endStatus === 1) {
        if (!DailyHuntQuest.isReadyToComplete(qm.getPlayer())) {
            qm.sendOk(i18n.t(messages.end.expired));
            return;
        }

        const reward = DailyHuntQuest.getRewardExp(qm.getLevel());
        if (reward <= 0 || !qm.forceCompleteQuest(DailyHuntQuest.QUEST_ID, DailyHuntQuest.DISPLAY_NPC_ID)) {
            qm.sendOk(i18n.t(messages.end.expired));
            return;
        }

        qm.gainExp(reward);
        qm.sendOk(i18n.t(messages.end.rewarded, { reward }));
    } else {
        qm.dispose();
    }
}
