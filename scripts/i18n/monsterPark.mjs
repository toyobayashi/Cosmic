import { defineMessages } from "../lib/i18n/index.mjs";

export const messages = defineMessages({
    shuttle: {
        prompt: {
            en: "Hey there! Need a lift back to town? That's what the Monster Park Shuttle is for!",
            zhCN: "嘿！需要搭车回去吗？怪物公园穿梭车就是为此准备的！"
        }
    },
    spiegelmann: {
        greeting: {
            en: "Welcome to Monster Park. The dungeon features are still being ported.",
            zhCN: "欢迎来到怪物公园。副本功能正在移植中。"
        }
    },
    merchant: {
        greeting: {
            en: "The Monster Park entrance is open. More features are still being ported.",
            zhCN: "怪物公园入口已经开放，更多功能正在移植中。"
        }
    },
    spiegelette: {
        greeting: {
            en: "Welcome to Monster Park! I'm Spiegelmann's younger sister, #bSpiegelette#k.\r\nNice to meet you!",
            zhCN: "欢迎来到怪物公园！我是休彼德曼的妹妹，#b休彼德拉#k。\r\n很高兴见到你！"
        }
    },
    daily: {
        levelRangeRequired: {
            en: "Only characters from level {minLevel} through {maxLevel} may use this Monster Park portal.",
            zhCN: "只有{minLevel}级至{maxLevel}级的角色才能使用这个怪物公园传送门。"
        },
        dailyLimit: {
            en: "You have used today's free entry and all 6 additional entries. Please come back tomorrow.",
            zhCN: "你今天的免费次数和6次额外入场次数都已用完，请明天再来。"
        },
        ticketRequired: {
            en: "You need a #bMonster Park Additional Entry Ticket#k for this entry.",
            zhCN: "本次入场需要一张#b怪物公园额外入场券#k。"
        },
        unavailable: {
            en: "Monster Park entry records are temporarily unavailable. Please try again later.",
            zhCN: "暂时无法读取怪物公园入场记录，请稍后再试。"
        },
        inventoryFull: {
            en: "Please make room in your ETC inventory for the Monster Park Commemorative Coins.",
            zhCN: "请先在其它栏为怪物公园纪念币腾出空间。"
        },
        selection: {
            en: "Select a Monster Park course at or below your level range.\r\n{entryNotice}\r\n\r\n{courses}",
            zhCN: "请选择当前等级范围或更低等级范围的怪物公园地图。\r\n{entryNotice}\r\n\r\n{courses}"
        },
        courseOption: {
            en: "#b#L{tier}#Level {minLevel}-{maxLevel} Monster Park#l",
            zhCN: "#b#L{tier}#{minLevel}-{maxLevel}级怪物公园#l"
        },
        freeEntryNotice: {
            en: "#dToday's free entry will be used.#k",
            zhCN: "#d本次将使用今天的免费入场次数。#k"
        },
        additionalEntryNotice: {
            en: "#rA Monster Park Additional Entry Ticket will be used. ({used}/6 additional entries used today)#k",
            zhCN: "#r本次将消耗一张怪物公园额外入场券。（今天已额外进入{used}/6次）#k"
        },
        invalidSelection: {
            en: "That course is above your current level range.",
            zhCN: "这个地图高于你当前的等级范围。"
        },
        reward: {
            en: "Challenge complete!\r\nYou received #b{exp} EXP#k and #b{coins} Monster Park Commemorative Coins#k.",
            zhCN: "挑战完成！\r\n你获得了#b{exp}点经验值#k和#b{coins}个怪物公园纪念币#k。"
        }
    },
    extremeWeekly: {
        levelRequired: {
            en: "You must be at least level 120 to enter Extreme Monster Park.",
            zhCN: "必须达到120级才能进入极限怪物公园。"
        },
        alreadyEntered: {
            en: "You have already entered Extreme Monster Park this week. Entry resets every Thursday at 12:00 AM.",
            zhCN: "你本周已经进入过极限怪物公园。入场次数于每周四00:00重置。"
        },
        unavailable: {
            en: "Extreme Monster Park entry records are temporarily unavailable. Please try again later.",
            zhCN: "暂时无法读取极限怪物公园入场记录，请稍后再试。"
        },
        selection: {
            en: "Extreme Monster Park can be entered once per week and does not consume an Additional Entry Ticket.\r\n\r\n#b#L0#Enter Extreme Monster Park#l",
            zhCN: "极限怪物公园每周只能进入一次，不消耗额外入场券。\r\n\r\n#b#L0#进入极限怪物公园#l"
        },
        reward: {
            en: "Challenge complete!\r\nYou received #b3,239,700 EXP#k.",
            zhCN: "挑战完成！\r\n你获得了#b3239700点经验值#k。"
        }
    },
    placeholder: {
        unavailable: {
            en: "Monster Park features are still being ported.",
            zhCN: "怪物公园相关功能正在移植中。"
        }
    }
});
