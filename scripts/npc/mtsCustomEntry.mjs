import { HENESYS_POTION_SHOP, HENESYS_STORAGE, Menu } from "../lib/mtsCustomEntry/constants.mjs";
import { prepareBeginnerQuests } from "../lib/mtsCustomEntry/beginnerQuests.mjs";
import {
    aranSkipTarget,
    cygnusSkipTarget,
    explorerSkipTargets,
    nearestTownPotionShops,
    quickMoveMaps
} from "../lib/mtsCustomEntry/data.mjs";
import {
    buildExplorerSkipSelection,
    buildMainSelection,
    buildQuickMoveSelection,
    buildSkipConfirmation
} from "../lib/mtsCustomEntry/dialog.mjs";
import { createI18n } from "../lib/i18n/index.mjs";
import { messages } from "../i18n/mtsCustomEntry.mjs";

let status = -1;
let selectedMenu = -1;
let selectedQuestSkipTarget = null;

export function start(ctx) {
    action(ctx, 1, 0, 0);
}

export function action(ctx, mode, type, selection) {
    const cm = ctx.cm;
    const i18n = createI18n(cm);

    if (mode !== 1) {
        cm.dispose();
        return;
    }

    status++;

    if (status === 0) {
        cm.sendSimple(buildMainSelection(i18n, isPreJobBeginner(cm)));
    } else if (status === 1) {
        selectedMenu = selection;
        handleMainSelection(cm, i18n);
    } else if (status === 2) {
        handleSubSelection(cm, i18n, selection);
    } else if (status === 3 && selectedMenu === Menu.SKIP_BEGINNER_QUESTS) {
        skipBeginnerQuests(cm, i18n);
    } else {
        cm.dispose();
    }
}

function handleMainSelection(cm, i18n) {
    if (selectedMenu === Menu.SKIP_BEGINNER_QUESTS) {
        sendSkipBeginnerQuestsPrompt(cm, i18n);
    } else if (selectedMenu === Menu.QUICK_MOVE && !isPreJobBeginner(cm)) {
        cm.sendSimple(buildQuickMoveSelection(i18n, quickMoveMaps));
    } else if (selectedMenu === Menu.RETURN_NEAREST_TOWN) {
        returnToNearestTown(cm);
    } else if (selectedMenu === Menu.QUICK_SHOP && !isPreJobBeginner(cm)) {
        openQuickShop(cm);
    } else if (selectedMenu === Menu.QUICK_STORAGE && !isPreJobBeginner(cm)) {
        openQuickStorage(cm, i18n);
    } else {
        cm.dispose();
    }
}

function handleSubSelection(cm, i18n, selection) {
    if (selectedMenu === Menu.SKIP_BEGINNER_QUESTS) {
        handleSkipBeginnerQuestsSelection(cm, i18n, selection);
    } else if (selectedMenu === Menu.QUICK_MOVE) {
        handleQuickMoveSelection(cm, selection);
    } else {
        cm.dispose();
    }
}

function isPreJobBeginner(cm) {
    const jobId = cm.getJobId();
    return jobId === 0 || jobId === 1000 || jobId === 2000;
}

function sendSkipBeginnerQuestsPrompt(cm, i18n) {
    const jobId = cm.getJobId();

    if (jobId === 0) {
        cm.sendSimple(buildExplorerSkipSelection(i18n, explorerSkipTargets));
    } else if (jobId === 1000) {
        selectedQuestSkipTarget = cygnusSkipTarget;
        cm.sendOk(buildSkipConfirmation(i18n, selectedQuestSkipTarget));
    } else if (jobId === 2000) {
        selectedQuestSkipTarget = aranSkipTarget;
        cm.sendOk(buildSkipConfirmation(i18n, selectedQuestSkipTarget));
    } else {
        cm.dispose();
    }
}

function handleSkipBeginnerQuestsSelection(cm, i18n, selection) {
    const jobId = cm.getJobId();

    if (jobId === 1000 || jobId === 2000) {
        skipBeginnerQuests(cm, i18n);
        return;
    }

    if (jobId !== 0 || selection < 0 || selection >= explorerSkipTargets.length) {
        cm.dispose();
        return;
    }

    selectedQuestSkipTarget = explorerSkipTargets[selection];
    cm.sendOk(buildSkipConfirmation(i18n, selectedQuestSkipTarget));
}

function skipBeginnerQuests(cm, i18n) {
    if (selectedQuestSkipTarget === null) {
        cm.dispose();
        return;
    }

    if (!prepareBeginnerQuests(cm, i18n, cm.getJobId())) {
        cm.dispose();
        return;
    }

    while (cm.getLevel() < selectedQuestSkipTarget.level) {
        cm.getPlayer().levelUp(false);
    }

    if (selectedQuestSkipTarget.portal !== undefined) {
        cm.warp(selectedQuestSkipTarget.mapId, selectedQuestSkipTarget.portal);
    } else {
        cm.warp(selectedQuestSkipTarget.mapId);
    }
    cm.dispose();
}

function handleQuickMoveSelection(cm, selection) {
    if (selection < 0 || selection >= quickMoveMaps.length) {
        cm.dispose();
        return;
    }

    cm.warp(quickMoveMaps[selection]);
    cm.dispose();
}

function returnToNearestTown(cm) {
    cm.warp(cm.getPlayer().getMap().getReturnMapId());
    cm.dispose();
}

function openQuickShop(cm) {
    cm.openShopNPC(getNearestTownPotionShop(cm));
    cm.dispose();
}

function openQuickStorage(cm, i18n) {
    if (cm.getPlayer().getShop() !== null) {
        cm.dropMessage(1, i18n.t(messages.storage.closeShopFirst));
        cm.dispose();
        return;
    }

    cm.getPlayer().getStorage().sendStorage(cm.getClient(), HENESYS_STORAGE);
    cm.dispose();
}

function getNearestTownPotionShop(cm) {
    const nearestTown = cm.getPlayer().getMap().getReturnMapId();

    for (const townShop of nearestTownPotionShops) {
        if (townShop[0] === nearestTown) {
            return townShop[1];
        }
    }

    return HENESYS_POTION_SHOP;
}
