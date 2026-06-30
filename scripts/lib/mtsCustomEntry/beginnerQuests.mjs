import {
    aranPreludeQuestIds,
    aranRewards,
    cygnusPreludeQuestIds,
    cygnusRewards,
    explorerBeginnerQuestIds,
    explorerRewards
} from "./data.mjs";
import { messages } from "../../i18n/mtsCustomEntry.mjs";

export function prepareBeginnerQuests(cm, i18n, jobId) {
    if (jobId === 0) {
        return completeQuestsAndGiveRewards(cm, i18n, explorerBeginnerQuestIds, explorerRewards);
    }
    if (jobId === 1000) {
        return completeQuestsAndGiveRewards(cm, i18n, cygnusPreludeQuestIds, cygnusRewards);
    }
    if (jobId === 2000) {
        return completeQuestsAndGiveRewards(cm, i18n, aranPreludeQuestIds, aranRewards);
    }

    return false;
}

function completeQuestsAndGiveRewards(cm, i18n, questIds, rewards) {
    if (!canHoldRewards(cm, rewards)) {
        cm.dropMessage(1, i18n.t(messages.skip.noRewardSpace));
        return false;
    }

    for (const questId of questIds) {
        cm.forceCompleteQuest(questId);
    }

    for (const reward of rewards) {
        cm.gainItem(reward[0], reward[1]);
    }
    return true;
}

function canHoldRewards(cm, rewards) {
    const itemIds = [];
    const quantities = [];

    for (const reward of rewards) {
        itemIds.push(reward[0]);
        quantities.push(reward[1]);
    }

    return cm.canHoldAll(itemIds, quantities);
}
