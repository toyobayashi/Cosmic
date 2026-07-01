export const Locale = Object.freeze({
    EN: "en",
    ZH_CN: "zhCN"
});

export function defineMessages(messages) {
    return deepFreeze(messages);
}

export function createI18n(cm) {
    const codePage = getClientCodePage(cm);
    const locale = getClientLanguageLocale(cm);

    return Object.freeze({
        codePage,
        locale,
        t(message, values) {
            return translate(message, locale, values);
        }
    });
}

export function localeForCodePage(codePage) {
    return Number(codePage) === 936 ? Locale.ZH_CN : Locale.EN;
}

export function translateForCodePage(message, codePage, values) {
    return translate(message, localeForCodePage(codePage), values);
}

export function translate(message, locale, values) {
    const template = message[locale] ?? message[Locale.EN] ?? "";
    return format(template, values);
}

function getClientCodePage(cm) {
    return cm.getClient().getPacketCodePage();
}

function getClientLanguageLocale(cm) {
    const client = cm.getClient();
    let language = undefined;
    try {
        language = client.getClientLanguage();
    } catch (_) {
        language = undefined;
    }
    return normalizeLocale(language);
}

function normalizeLocale(locale) {
    switch (String(locale ?? "").toLowerCase()) {
        case "zh":
        case "zh-cn":
        case "zh_cn":
        case "zhcn":
            return Locale.ZH_CN;
        default:
            return Locale.EN;
    }
}

function format(template, values) {
    if (!values) {
        return template;
    }

    return template.replace(/\{([^}]+)\}/g, function (_, key) {
        return values[key];
    });
}

function deepFreeze(value) {
    if (value === null || typeof value !== "object" || Object.isFrozen(value)) {
        return value;
    }

    for (const key of Object.keys(value)) {
        deepFreeze(value[key]);
    }

    return Object.freeze(value);
}
