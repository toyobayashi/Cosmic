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
        },
        sponsor: {
            en: "Sponsor",
            zhCN: "赞助"
        }
    },
    sponsor: {
        title: {
            en: "Sponsor",
            zhCN: "赞助"
        },
        account: {
            en: "Thank you for supporting this server. Sponsorship go toward maintenance and operating costs, allowing everyone to continue playing here. Please note that your sponsorship is a voluntary gift—not a standard top-up or product purchase—and is #rnon-refundable#k. As a token of appreciation, you will #rreceive a corresponding amount of NX#k. Please scan the payment QR code first and #renter your account name#k (#b{account}#k) in the payment message. After completing the payment, click #rCreate a sponsor record#k and enter the actual amount paid on the next page; NX are usually credited within 24 hours. You can click #rView sponsor records#k to check your sponsorship records.",
            zhCN: "感谢您愿意支持本服务器。赞助会用于服务器维护和运营成本，让大家可以继续在这里游玩。赞助前请您悉知，赞助行为属于自愿赠与，不属于充值或商品购买，#r不支持退款#k。作为感谢，赞助后您将#r获得相应数量的点券#k。请先扫赞赏码，#r留言中填写您的账号名#k #b{account}#k，完成付款后再点击#r创建赞助记录#k并在下一页填写您的实际付款金额，点券通常会在 24 小时内到账。您可以点击#r查看赞助记录#k来查看您的赞助记录。"
        },
        imageHint: {
            en: "#fUI/tutorial.img/sponsor/qrcode#",
            zhCN: "#fUI/tutorial.img/sponsor/qrcode#"
        },
        pendingHeader: {
            en: "Pending sponsor records:",
            zhCN: "待审核赞助记录："
        },
        noPending: {
            en: "You do not have pending sponsor records.",
            zhCN: "您当前没有待审核赞助记录。"
        },
        pendingLine: {
            en: "Record ID {id}: {amount} CNY, expected {nx} NX",
            zhCN: "记录ID {id}：{amount} 元，预计 {nx} 点券"
        },
        createOption: {
            en: "Create a sponsor record",
            zhCN: "创建赞助记录"
        },
        viewRecordsOption: {
            en: "View sponsor records",
            zhCN: "查看赞助记录"
        },
        recordsTitle: {
            en: "Sponsor Records",
            zhCN: "赞助记录"
        },
        noRecords: {
            en: "You do not have sponsor records yet.",
            zhCN: "您当前还没有赞助记录。"
        },
        recordLine: {
            en: "Record ID {id}: {amount} CNY, expected {expectedNx} NX, awarded {awardedNx} NX, status {status}",
            zhCN: "记录ID {id}：{amount} 元，预计 {expectedNx} 点券，已发放 {awardedNx} 点券，状态 {status}"
        },
        backOption: {
            en: "Back",
            zhCN: "返回"
        },
        amountPrompt: {
            en: "Enter the paid amount in CNY. It must match your WeChat payment amount exactly and must be a whole number from 1 to 200.\r\n\r\nRecords are reviewed manually. NX is usually awarded within 24 hours at #b100 NX per 1 CNY#k.",
            zhCN: "请输入本次实际付款金额（元）。填写金额必须与微信付款金额一致，并且只能填写 1 到 200 之间的正整数。\r\n\r\n赞助记录会由管理员人工审核，点券通常会在 24 小时内到账，默认按 #b1 元 = 100 点券#k 发放。"
        },
        created: {
            en: "Sponsor record {id} has been created.\r\nAmount: #b{amount} CNY#k\r\nExpected award: #b{nx} NX#k\r\nAfter payment is confirmed manually, a GM will award NX for this record. You can click #rView sponsor records#k to check your sponsor records.",
            zhCN: "赞助记录 {id} 已创建。\r\n金额：#b{amount} 元#k\r\n预计发放：#b{nx} 点券#k\r\n人工审核确认付款后，管理员发放点券。您可以点击#r查看赞助记录#k来查看您的赞助记录。"
        },
        createFailed: {
            en: "Failed to create sponsor record. Please try again later.",
            zhCN: "创建赞助记录失败，请稍后再试。"
        },
        viewFailed: {
            en: "Failed to load sponsor records. Please try again later.",
            zhCN: "查看赞助记录失败，请稍后再试。"
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
