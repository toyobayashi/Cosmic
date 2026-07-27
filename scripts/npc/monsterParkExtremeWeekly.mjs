import { createI18n } from "../lib/i18n/index.mjs";
import { messages } from "../i18n/monsterPark.mjs";

const MIN_LEVEL = 120;
const REWARD_EXP = 3239700;
const ENTRY_AVAILABLE = 0;
const ENTRY_ALREADY_USED = 1;
const ENTRY_ERROR = -1;

let status = -1;
let selectionShown = false;

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
        showSelection(cm, i18n);
    } else if (status === 1 && selectionShown) {
        completeEntry(cm, i18n, selection);
    } else {
        cm.dispose();
    }
}

function showSelection(cm, i18n) {
    if (cm.getLevel() < MIN_LEVEL) {
        cm.sendOk(i18n.t(messages.extremeWeekly.levelRequired));
        return;
    }

    const entryStatus = cm.getMonsterParkExtremeEntryStatus();
    if (entryStatus === ENTRY_ERROR) {
        cm.sendOk(i18n.t(messages.extremeWeekly.unavailable));
        return;
    }
    if (entryStatus === ENTRY_ALREADY_USED) {
        cm.sendOk(i18n.t(messages.extremeWeekly.alreadyEntered));
        return;
    }

    selectionShown = true;
    cm.sendSimple(i18n.t(messages.extremeWeekly.selection));
}

function completeEntry(cm, i18n, selection) {
    if (cm.getLevel() < MIN_LEVEL || selection !== 0) {
        cm.dispose();
        return;
    }

    const entryResult = cm.registerMonsterParkExtremeEntry();
    if (entryResult === ENTRY_ALREADY_USED) {
        cm.sendOk(i18n.t(messages.extremeWeekly.alreadyEntered));
        return;
    }
    if (entryResult !== ENTRY_AVAILABLE) {
        cm.sendOk(i18n.t(messages.extremeWeekly.unavailable));
        return;
    }

    cm.gainExp(REWARD_EXP);
    cm.sendOk(i18n.t(messages.extremeWeekly.reward));
}
