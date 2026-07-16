import { createI18n } from "../lib/i18n/index.mjs";
import { messages } from "../i18n/monsterPark.mjs";

const RETURN_LOCATION = "MONSTER_PARK";
const MONSTER_PARK_ENTRANCE = 951000000;
const HENESYS = 100000000;

let status = -1;

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
        cm.sendYesNo(i18n.t(messages.shuttle.prompt));
        return;
    }

    if (status === 1) {
        let returnMap = cm.getPlayer().getSavedLocation(RETURN_LOCATION);
        if (returnMap < 0 || returnMap === MONSTER_PARK_ENTRANCE) {
            returnMap = HENESYS;
        }

        cm.warp(returnMap);
    }

    cm.dispose();
}
