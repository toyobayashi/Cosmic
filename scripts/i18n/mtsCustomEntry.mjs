import { defineMessages } from "../lib/i18n/index.mjs";

export const messages = defineMessages({
    main: {
        question: {
            en: "What would you like to do?",
            zhCN: "你想做什么？"
        },
        skipBeginnerQuests: {
            en: "Skip Beginner Quests",
            zhCN: "跳过新手任务"
        },
        quickMove: {
            en: "Quick Move",
            zhCN: "快速移动"
        },
        quickShop: {
            en: "Quick Shop",
            zhCN: "快速商店"
        },
        quickStorage: {
            en: "Quick Storage",
            zhCN: "快速仓库"
        },
        returnNearestTown: {
            en: "Return to Nearest Town",
            zhCN: "返回最近城镇"
        }
    },
    skip: {
        chooseInstructor: {
            en: "Which job instructor would you like to visit?",
            zhCN: "你想去找哪位职业教官？"
        },
        confirmation: {
            en: "I'll skip the beginner quests, raise you to level #b{level}#k, and send you to #b#m{mapId}##k.",
            zhCN: "我会帮你跳过新手任务，把你提升到 #b{level}#k 级，并送你前往 #b#m{mapId}##k。"
        },
        noRewardSpace: {
            en: "Please make room for the beginner quest rewards first.",
            zhCN: "请先为新手任务奖励腾出背包空间。"
        }
    },
    storage: {
        closeShopFirst: {
            en: "Please close the shop UI first.",
            zhCN: "请先关闭商店界面。"
        }
    },
    quickMove: {
        question: {
            en: "Where would you like to go?",
            zhCN: "你想去哪里？"
        }
    },
    job: {
        warrior: {
            en: "Warrior",
            zhCN: "战士"
        },
        magician: {
            en: "Magician",
            zhCN: "魔法师"
        },
        bowman: {
            en: "Bowman",
            zhCN: "弓箭手"
        },
        thief: {
            en: "Thief",
            zhCN: "飞侠"
        },
        pirate: {
            en: "Pirate",
            zhCN: "海盗"
        },
        cygnusKnight: {
            en: "Cygnus Knight",
            zhCN: "骑士团"
        },
        aran: {
            en: "Aran",
            zhCN: "战神"
        }
    }
});
