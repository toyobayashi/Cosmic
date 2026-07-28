import { createI18n } from "../i18n/index.mjs";
import { messages } from "../../i18n/monsterPark.mjs";

const MONSTER_PARK_COIN = 4310020;
const FREE_ENTRY_AVAILABLE = -1;
const ENTRY_STATE_ERROR = -2;
const MAX_ADDITIONAL_ENTRIES = 6;
const ENTRY_RESULT_FREE = 0;
const ENTRY_RESULT_ADDITIONAL = 1;
const ENTRY_RESULT_TICKET_REQUIRED = 2;
const ENTRY_RESULT_DAILY_LIMIT = 3;

export function createCourseEntryState() {
    return {
        status: -1,
        courseSelectionShown: false,
        pendingReward: null
    };
}

export function handleCourseEntryAction(ctx, mode, selection, state, courses) {
    const cm = ctx.cm;
    const i18n = createI18n(cm);

    if (mode !== 1) {
        cm.dispose();
        return;
    }

    state.status++;
    if (state.status === 0) {
        showCourseSelection(cm, i18n, state, courses);
    } else if (state.status === 1 && state.courseSelectionShown) {
        prepareCourseReward(cm, i18n, selection, state, courses);
    } else if (state.status === 2 && state.pendingReward !== null) {
        completeCourse(cm, i18n, state);
    } else {
        cm.dispose();
    }
}

function showCourseSelection(cm, i18n, state, courses) {
    const courseIndex = getCourseIndex(cm.getLevel(), courses);
    if (courseIndex < 0) {
        const first = courses[0];
        const last = courses[courses.length - 1];
        cm.sendOk(i18n.t(messages.daily.levelRangeRequired, {
            minLevel: first.minLevel,
            maxLevel: last.maxLevel
        }));
        return;
    }

    const entryStatus = cm.getMonsterParkEntryStatus();
    if (entryStatus === ENTRY_STATE_ERROR) {
        cm.sendOk(i18n.t(messages.daily.unavailable));
        return;
    }
    if (entryStatus >= MAX_ADDITIONAL_ENTRIES) {
        cm.sendOk(i18n.t(messages.daily.dailyLimit));
        return;
    }

    const entryNotice = entryStatus === FREE_ENTRY_AVAILABLE
        ? i18n.t(messages.daily.freeEntryNotice)
        : i18n.t(messages.daily.additionalEntryNotice, { used: entryStatus });
    const courseOptions = courses
        .slice(0, courseIndex + 1)
        .map((course, tier) => i18n.t(messages.daily.courseOption, {
            tier,
            minLevel: course.minLevel,
            maxLevel: course.maxLevel
        }))
        .join("\r\n");

    state.courseSelectionShown = true;
    cm.sendSimple(i18n.t(messages.daily.selection, {
        entryNotice,
        courses: courseOptions
    }));
}

function prepareCourseReward(cm, i18n, selection, state, courses) {
    const courseIndex = getCourseIndex(cm.getLevel(), courses);
    if (courseIndex < 0 || selection < 0 || selection > courseIndex) {
        cm.sendOk(i18n.t(messages.daily.invalidSelection));
        return;
    }

    const course = courses[selection];
    const coins = randomInt(course.minCoins, course.maxCoins);
    if (!cm.canHold(MONSTER_PARK_COIN, coins)) {
        cm.sendOk(i18n.t(messages.daily.inventoryFull));
        return;
    }

    state.pendingReward = { course, coins };
    cm.sendOk(i18n.t(messages.daily.reward, {
        exp: course.exp,
        coins
    }));
}

function completeCourse(cm, i18n, state) {
    const reward = state.pendingReward;
    state.pendingReward = null;

    if (!cm.canHold(MONSTER_PARK_COIN, reward.coins)) {
        cm.sendOk(i18n.t(messages.daily.inventoryFull));
        return;
    }

    const entryResult = cm.registerMonsterParkEntry();
    if (entryResult === ENTRY_RESULT_TICKET_REQUIRED) {
        cm.sendOk(i18n.t(messages.daily.ticketRequired));
        return;
    }
    if (entryResult === ENTRY_RESULT_DAILY_LIMIT) {
        cm.sendOk(i18n.t(messages.daily.dailyLimit));
        return;
    }
    if (entryResult !== ENTRY_RESULT_FREE && entryResult !== ENTRY_RESULT_ADDITIONAL) {
        cm.sendOk(i18n.t(messages.daily.unavailable));
        return;
    }

    const player = cm.getPlayer();
    cm.gainItem(MONSTER_PARK_COIN, reward.coins);
    cm.dispose();
    player.gainExp(reward.course.exp, true, true);
}

function getCourseIndex(level, courses) {
    return courses.findIndex(course => level >= course.minLevel && level <= course.maxLevel);
}

function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}
