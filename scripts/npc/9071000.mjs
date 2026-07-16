import { createI18n } from "../lib/i18n/index.mjs";
import { messages } from "../i18n/monsterPark.mjs";

export function start(ctx) {
    const cm = ctx.cm;
    const i18n = createI18n(cm);

    cm.sendOk(i18n.t(messages.spiegelmann.greeting));
}

export function action(ctx, mode, type, selection) {
    ctx.cm.dispose();
}
