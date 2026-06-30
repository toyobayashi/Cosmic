export type Locale = "en" | "zhCN";

export type MessageEntry = Readonly<{
    en: string;
    zhCN?: string;
}>;

export type MessageTree = Readonly<{
    [key: string]: MessageEntry | MessageTree;
}>;

export declare function defineMessages<const T extends MessageTree>(messages: T): Readonly<T>;

export declare function createI18n(cm: {
    getClient(): {
        getPacketCodePage(): number;
    };
}): Readonly<{
    codePage: number;
    locale: Locale;
    t(message: MessageEntry, values?: Record<string, string | number>): string;
}>;

export declare function localeForCodePage(codePage: number): Locale;

export declare function translateForCodePage(
    message: MessageEntry,
    codePage: number,
    values?: Record<string, string | number>
): string;

export declare function translate(
    message: MessageEntry,
    locale: Locale,
    values?: Record<string, string | number>
): string;
