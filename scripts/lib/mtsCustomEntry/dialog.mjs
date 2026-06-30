import { Menu } from "./constants.mjs";
import { messages } from "../../i18n/mtsCustomEntry.mjs";

export function buildMainSelection(i18n, preJobBeginner) {
    let text = i18n.t(messages.main.question) + "\r\n\r\n#b";

    if (preJobBeginner) {
        text += option(Menu.SKIP_BEGINNER_QUESTS, i18n.t(messages.main.skipBeginnerQuests));
    } else {
        text += option(Menu.QUICK_MOVE, i18n.t(messages.main.quickMove));
        text += "\r\n" + option(Menu.QUICK_SHOP, i18n.t(messages.main.quickShop));
        text += "\r\n" + option(Menu.QUICK_STORAGE, i18n.t(messages.main.quickStorage));
    }
    text += "\r\n" + option(Menu.RETURN_NEAREST_TOWN, i18n.t(messages.main.returnNearestTown));

    return text;
}

export function buildExplorerSkipSelection(i18n, targets) {
    let text = i18n.t(messages.skip.chooseInstructor) + "#b";

    for (let i = 0; i < targets.length; i++) {
        text += "\r\n" + option(i, i18n.t(messages.job[targets[i].label]) + " - #m" + targets[i].mapId + "#");
    }

    return text;
}

export function buildSkipConfirmation(i18n, target) {
    return i18n.t(messages.skip.confirmation, {
        level: target.level,
        mapId: target.mapId
    });
}

export function buildQuickMoveSelection(i18n, mapIds) {
    let text = i18n.t(messages.quickMove.question) + "#b";

    for (let i = 0; i < mapIds.length; i++) {
        text += "\r\n" + option(i, "#m" + mapIds[i] + "#");
    }

    return text;
}

function option(id, label) {
    return "#L" + id + "#" + label + "#l";
}
