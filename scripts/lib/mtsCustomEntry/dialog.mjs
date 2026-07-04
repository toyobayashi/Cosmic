import { Menu, SponsorMenu } from "./constants.mjs";
import { messages } from "../../i18n/mtsCustomEntry.mjs";

export function buildMainSelection(i18n, preJobBeginner, showSponsor) {
    let text = i18n.t(messages.main.question) + "\r\n\r\n#b";

    if (preJobBeginner) {
        text += option(Menu.SKIP_BEGINNER_QUESTS, i18n.t(messages.main.skipBeginnerQuests));
    } else {
        text += option(Menu.QUICK_MOVE, i18n.t(messages.main.quickMove));
        text += "\r\n" + option(Menu.QUICK_SHOP, i18n.t(messages.main.quickShop));
        text += "\r\n" + option(Menu.QUICK_STORAGE, i18n.t(messages.main.quickStorage));
    }
    text += "\r\n" + option(Menu.RETURN_NEAREST_TOWN, i18n.t(messages.main.returnNearestTown));
    if (showSponsor) {
        text += "\r\n" + option(Menu.SPONSOR, i18n.t(messages.main.sponsor));
    }

    return text;
}

export function buildSponsorSelection(i18n, account, supportsRemoteAssets) {
    let text = ''; // "#e" + i18n.t(messages.sponsor.title) + "#n\r\n";
    text += i18n.t(messages.sponsor.account, { account }) + "\r\n";

    text += "#b" + option(SponsorMenu.CREATE, i18n.t(messages.sponsor.createOption));
    text += "  " + option(SponsorMenu.VIEW_RECORDS, i18n.t(messages.sponsor.viewRecordsOption));
    text += "  " + option(SponsorMenu.BACK, i18n.t(messages.sponsor.backOption));

    text += "#k";
    if (supportsRemoteAssets) {
        text += "\r\n\r\n\r\n" + i18n.t(messages.sponsor.imageHint);
    }
    return text;
}

export function buildSponsorRecords(i18n, records) {
    let text = "#e" + i18n.t(messages.sponsor.recordsTitle) + "#n\r\n";

    if (records.size() > 0) {
        for (let i = 0; i < records.size(); i++) {
            const order = records.get(i);
            const amountCents = Number(order.getAmountCents());
            const awardedNx = order.getAwardedNx();
            text += "\r\n#d" + i18n.t(messages.sponsor.recordLine, {
                id: order.getOrderId(),
                amount: Math.floor(amountCents / 100),
                expectedNx: order.getExpectedNx(),
                awardedNx: awardedNx == null ? "-" : awardedNx,
                status: order.getStatus()
            }) + "#k";
        }
    } else {
        text += i18n.t(messages.sponsor.noRecords);
    }

    text += "\r\n\r\n#b" + option(0, i18n.t(messages.sponsor.backOption));
    return text;
}

export function buildSponsorCreated(i18n, order) {
    let text = i18n.t(messages.sponsor.created, {
        id: order.getOrderId(),
        amount: Math.floor(order.getAmountCents() / 100),
        nx: order.getExpectedNx()
    });
    text += "\r\n\r\n#b" + option(SponsorMenu.CREATED_VIEW_RECORDS, i18n.t(messages.sponsor.viewRecordsOption));
    text += "  " + option(SponsorMenu.CREATED_BACK, i18n.t(messages.sponsor.backOption));
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
