import { defineMessages } from "../lib/i18n/index.mjs";

export const messages = defineMessages({
    start: {
        prompt: {
            en: "Today's hunt is available.\r\n\r\nDefeat #r100 monsters#k whose level is within #b10 levels#k of your current level. Progress and unclaimed rewards reset every day at 12:00 AM.\r\n\r\nWould you like to accept the quest?",
            zhCN: "今天的狩猎任务已经开放。\r\n\r\n请消灭#r100只#k与当前角色等级相差不超过#b10级#k的怪物。任务进度和未领取的奖励会在每天00:00重置。\r\n\r\n要接取任务吗？"
        },
        accepted: {
            en: "The Daily Hunt has begun. Hunt 100 level-appropriate monsters before midnight.",
            zhCN: "每日狩猎已经开始。请在今天结束前消灭100只符合等级要求的怪物。"
        },
        unavailable: {
            en: "The Daily Hunt is not available right now.",
            zhCN: "当前无法接取每日狩猎任务。"
        }
    },
    end: {
        prompt: {
            en: "You completed today's hunt.\r\n\r\nClaim #b{reward} EXP#k now?",
            zhCN: "你已经完成了今天的狩猎任务。\r\n\r\n现在领取#b{reward}点经验值#k吗？"
        },
        rewarded: {
            en: "Excellent work. You received #b{reward} EXP#k. Come back after midnight for the next Daily Hunt.",
            zhCN: "干得漂亮。你获得了#b{reward}点经验值#k。下一次每日狩猎会在00:00开放。"
        },
        expired: {
            en: "That Daily Hunt has expired. Please accept today's quest from the lightbulb.",
            zhCN: "这次每日狩猎已经过期，请通过灯泡重新接取今天的任务。"
        }
    }
});
