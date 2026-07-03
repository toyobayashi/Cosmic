import { HENESYS_POTION_SHOP, HENESYS_STORAGE, Menu, SponsorMenu } from "../lib/mtsCustomEntry/constants.mjs";
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
    buildSponsorCreated,
    buildSponsorRecords,
    buildSponsorSelection,
    buildSkipConfirmation
} from "../lib/mtsCustomEntry/dialog.mjs";
import { createI18n } from "../lib/i18n/index.mjs";
import { messages } from "../i18n/mtsCustomEntry.mjs";

const SponsorService = Java.type("api.service.SponsorService");
const CreateSponsorOrderDTO = Java.type("api.model.dto.CreateSponsorOrderDTO");
const MAX_SPONSOR_AMOUNT_YUAN = 200;

let status = -1;
let selectedMenu = -1;
let selectedSponsorMenu = -1;
let selectedQuestSkipTarget = null;
let sponsorService = null;

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
        cm.sendSimple(buildMainSelection(i18n, isPreJobBeginner(cm), isShowSponsor(i18n)));
    } else if (status === 1) {
        selectedMenu = selection;
        handleMainSelection(cm, i18n);
    } else if (status === 2) {
        handleSubSelection(cm, i18n, selection);
    } else if (status === 3 && selectedMenu === Menu.SKIP_BEGINNER_QUESTS) {
        skipBeginnerQuests(cm, i18n);
    } else if (status === 3 && selectedMenu === Menu.SPONSOR && selectedSponsorMenu === SponsorMenu.CREATE) {
        createSponsorOrder(cm, i18n, selection);
    } else if (status === 3 && selectedMenu === Menu.SPONSOR && selectedSponsorMenu === SponsorMenu.VIEW_RECORDS) {
        returnToSponsorPrompt(cm, i18n);
    } else if (status === 4 && selectedMenu === Menu.SPONSOR && selectedSponsorMenu === SponsorMenu.CREATE) {
        handleSponsorCreatedSelection(cm, i18n, selection);
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
    } else if (selectedMenu === Menu.SPONSOR && isShowSponsor(i18n)) {
        sendSponsorPrompt(cm, i18n);
    } else {
        cm.dispose();
    }
}

function handleSubSelection(cm, i18n, selection) {
    if (selectedMenu === Menu.SKIP_BEGINNER_QUESTS) {
        handleSkipBeginnerQuestsSelection(cm, i18n, selection);
    } else if (selectedMenu === Menu.QUICK_MOVE) {
        handleQuickMoveSelection(cm, selection);
    } else if (selectedMenu === Menu.SPONSOR && isShowSponsor(i18n)) {
        handleSponsorSelection(cm, i18n, selection);
    } else {
        cm.dispose();
    }
}

function isPreJobBeginner(cm) {
    const jobId = cm.getJobId();
    return jobId === 0 || jobId === 1000 || jobId === 2000;
}

function isShowSponsor(i18n) {
    return true;
    // return i18n.locale === "zhCN";
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

function sendSponsorPrompt(cm, i18n) {
    try {
        cm.sendSimple(buildSponsorSelection(i18n, cm.getClient().getAccountName()));
    } catch (e) {
        cm.sendOk(i18n.t(messages.sponsor.createFailed));
    }
}

function handleSponsorSelection(cm, i18n, selection) {
    selectedSponsorMenu = selection;

    if (selection === SponsorMenu.CREATE) {
        cm.sendGetNumber(i18n.t(messages.sponsor.amountPrompt), 10, 1, MAX_SPONSOR_AMOUNT_YUAN);
    } else if (selection === SponsorMenu.VIEW_RECORDS) {
        sendSponsorRecords(cm, i18n);
    } else {
        status = 0;
        cm.sendSimple(buildMainSelection(i18n, isPreJobBeginner(cm), isShowSponsor(i18n)));
    }
}

function returnToSponsorPrompt(cm, i18n) {
    status = 1;
    selectedSponsorMenu = -1;
    sendSponsorPrompt(cm, i18n);
}

function sendSponsorRecords(cm, i18n) {
    try {
        const records = getSponsorService().listOrders(cm.getClient().getAccountName(), "");
        cm.sendSimple(buildSponsorRecords(i18n, records));
    } catch (e) {
        logScriptError(e);
        cm.sendOk(i18n.t(messages.sponsor.viewFailed));
    }
}

function createSponsorOrder(cm, i18n, amountYuan) {
    try {
        const request = new CreateSponsorOrderDTO();
        request.setAmountYuan(amountYuan);
        const order = getSponsorService().createOrder(cm.getClient().getAccID(), request);
        cm.sendSimple(buildSponsorCreated(i18n, order));
    } catch (e) {
        logScriptError(e);
        cm.sendOk(i18n.t(messages.sponsor.createFailed));
    }
}

function handleSponsorCreatedSelection(cm, i18n, selection) {
    if (selection === SponsorMenu.CREATED_VIEW_RECORDS) {
        selectedSponsorMenu = SponsorMenu.VIEW_RECORDS;
        sendSponsorRecords(cm, i18n);
    } else {
        returnToSponsorPrompt(cm, i18n);
    }
}

function logScriptError(error) {
    try {
        error.printStackTrace();
    } catch (_) {
        // Some script errors are JavaScript values rather than Java Throwables.
    }
}

function getSponsorService() {
    if (sponsorService === null) {
        sponsorService = new SponsorService();
    }
    return sponsorService;
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
