var status = -1;
var selectedMenu = -1;
var selectedQuestSkipTarget = -1;

var SKIP_BEGINNER_QUESTS = 0;
var QUICK_MOVE = 1;
var RETURN_NEAREST_TOWN = 2;
var QUICK_SHOP = 3;
var QUICK_STORAGE = 4;

var HENESYS_POTION_SHOP = 1011100;
var HENESYS_STORAGE = 1012009;

var nearestTownPotionShops = [
    [1000000, 11100], // Amherst
    [104000000, 1001100], // Lith Harbor
    [100000000, 1011100], // Henesys
    [103040000, 1052116], // Kerning Square
    [102000000, 1021100], // Perion
    [101000000, 1031100], // Ellinia
    [103000000, 1051002], // Kerning City
    [105040300, 1061002], // Sleepywood
    [106020000, 1301000], // Mushroom Forest Field
    [110000000, 1081000], // Florina Beach
    [120000000, 1091002], // Nautilus Harbor
    [130000000, 1100002], // Ereve
    [140000000, 1200002], // Rien
    [200000000, 2012005], // Orbis
    [211000000, 2022001], // El Nath
    [220000000, 2041006], // Ludibrium
    [221000000, 2051000], // Omega Sector
    [222000000, 2070001], // Korean Folk Town
    [230000000, 2060004], // Aquarium
    [240000000, 2080001], // Leafre
    [250000000, 2090003], // Mu Lung
    [251000000, 2093002], // Herb Town
    [260000000, 2100004], // Ariant
    [261000000, 2110001], // Magatia
    [270000000, 2080001], // Temple of Time
    [270000100, 2080001], // Temple of Time
    [300000000, 2130000], // Altaire Camp
    [540000000, 9270021], // CBD
    [541000000, 9270022], // Boat Quay Town
    [551000000, 9270065], // Kampung Village
    [600000000, 9201060], // New Leaf City
    [800000000, 9120002], // Mushroom Shrine
    [801000000, 9120002]  // Showa Town
];

var quickMoveMaps = [
    ["Amherst", 1000000],
    ["Southperry", 2000000],
    ["Lith Harbor", 104000000],
    ["Henesys", 100000000],
    ["Ellinia", 101000000],
    ["Perion", 102000000],
    ["Kerning City", 103000000],
    ["Kerning Square", 103040000],
    ["Nautilus Harbor", 120000000],
    ["Sleepywood", 105040300],
    ["Mushroom Kingdom", 106020000],
    ["Florina Beach", 110000000],
    ["Ereve", 130000000],
    ["Rien", 140000000],
    ["Orbis", 200000000],
    ["El Nath", 211000000],
    ["Ludibrium", 220000000],
    ["Omega Sector", 221000000],
    ["Korean Folk Town", 222000000],
    ["Aquarium", 230000000],
    ["Leafre", 240000000],
    ["Neo City", 240070000],
    ["Mu Lung", 250000000],
    ["Herb Town", 251000000],
    ["Ariant", 260000000],
    ["Magatia", 261000000],
    ["Temple of Time", 270000100],
    ["Ellin Forest", 300000000],
    ["Singapore", 540000000],
    ["Boat Quay Town", 541000000],
    ["Kampung Village", 551000000],
    ["New Leaf City", 600000000],
    ["Mushroom Shrine", 800000000],
    ["Showa Town", 801000000],
    ["Happyville", 209000000],
    ["Amoria", 680000000]
];

var explorerSkipTargets = [
    ["Warrior", 10, 102000003],
    ["Magician", 8, 101000003],
    ["Bowman", 10, 100000201],
    ["Thief", 10, 103000003],
    ["Pirate", 10, 120000101]
];

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode != 1) {
        cm.dispose();
        return;
    }

    status++;

    if (status == 0) {
        cm.sendSimple(buildMainSelection());
    } else if (status == 1) {
        selectedMenu = selection;

        if (selectedMenu == SKIP_BEGINNER_QUESTS) {
            sendSkipBeginnerQuestsPrompt();
        } else if (selectedMenu == QUICK_MOVE && !isPreJobBeginner()) {
            cm.sendSimple(buildQuickMoveSelection());
        } else if (selectedMenu == RETURN_NEAREST_TOWN) {
            returnToNearestTown();
        } else if (selectedMenu == QUICK_SHOP && !isPreJobBeginner()) {
            openQuickShop();
        } else if (selectedMenu == QUICK_STORAGE && !isPreJobBeginner()) {
            openQuickStorage();
        } else {
            cm.dispose();
        }
    } else if (status == 2) {
        if (selectedMenu == SKIP_BEGINNER_QUESTS) {
            handleSkipBeginnerQuestsSelection(selection);
        } else if (selectedMenu == QUICK_MOVE) {
            handleQuickMoveSelection(selection);
        } else {
            cm.dispose();
        }
    } else if (status == 3 && selectedMenu == SKIP_BEGINNER_QUESTS) {
        skipBeginnerQuests();
    } else {
        cm.dispose();
    }
}

function buildMainSelection() {
    var text = "What would you like to do?\r\n\r\n#b";

    if (isPreJobBeginner()) {
        text += "#L" + SKIP_BEGINNER_QUESTS + "#Skip Beginner Quests#l";
    } else {
        text += "#L" + QUICK_MOVE + "#Quick Move#l";
        text += "\r\n#L" + QUICK_SHOP + "#Quick Shop#l";
        text += "\r\n#L" + QUICK_STORAGE + "#Quick Storage#l";
    }
    text += "\r\n#L" + RETURN_NEAREST_TOWN + "#Return to Nearest Town#l";

    return text;
}

function isPreJobBeginner() {
    var jobId = cm.getJobId();

    return jobId == 0 || jobId == 1000 || jobId == 2000;
}

function sendSkipBeginnerQuestsPrompt() {
    var jobId = cm.getJobId();

    if (jobId == 0) {
        cm.sendSimple(buildExplorerSkipSelection());
    } else if (jobId == 1000) {
        selectedQuestSkipTarget = ["Cygnus Knight", 10, 130000000];
        cm.sendOk(buildSkipConfirmation());
    } else if (jobId == 2000) {
        selectedQuestSkipTarget = ["Aran", 10, 140000000];
        cm.sendOk(buildSkipConfirmation());
    } else {
        cm.dispose();
    }
}

function buildExplorerSkipSelection() {
    var text = "Which job instructor would you like to visit?#b";

    for (var i = 0; i < explorerSkipTargets.length; i++) {
        text += "\r\n#L" + i + "#" + explorerSkipTargets[i][0] + " - #m" + explorerSkipTargets[i][2] + "##l";
    }

    return text;
}

function handleSkipBeginnerQuestsSelection(selection) {
    if (cm.getJobId() != 0) {
        skipBeginnerQuests();
        return;
    }

    if (selection < 0 || selection >= explorerSkipTargets.length) {
        cm.dispose();
        return;
    }

    selectedQuestSkipTarget = explorerSkipTargets[selection];
    cm.sendOk(buildSkipConfirmation());
}

function buildSkipConfirmation() {
    return "I'll skip the beginner quests, raise you to level #b" + selectedQuestSkipTarget[1] + "#k, and send you to #b#m" + selectedQuestSkipTarget[2] + "##k.";
}

function skipBeginnerQuests() {
    if (selectedQuestSkipTarget == -1) {
        cm.dispose();
        return;
    }

    while (cm.getLevel() < selectedQuestSkipTarget[1]) {
        cm.getPlayer().levelUp(false);
    }

    if (cm.getJobId() == 1000) {
        prepareCygnusFirstJobQuests();
    } else if (cm.getJobId() == 2000) {
        prepareAranFirstJobQuests();
    }

    cm.warp(selectedQuestSkipTarget[2]);
    cm.dispose();
}

function prepareCygnusFirstJobQuests() {
    var completedPreludeQuests = [
        20000, 20001, 20002, 20003, 20004, 20005, 20006, 20007, 20008,
        20010, 20011, 20012, 20013, 20015, 20016, 20017, 20020, 20100
    ];
    var firstJobQuests = [20101, 20102, 20103, 20104, 20105];

    for (var i = 0; i < completedPreludeQuests.length; i++) {
        cm.forceCompleteQuest(completedPreludeQuests[i]);
    }

    for (var j = 0; j < firstJobQuests.length; j++) {
        cm.forceStartQuest(firstJobQuests[j]);
    }
}

function prepareAranFirstJobQuests() {
    var completedPreludeQuests = [21000, 21001, 21010, 21011, 21012, 21013, 21100];

    for (var i = 0; i < completedPreludeQuests.length; i++) {
        cm.forceCompleteQuest(completedPreludeQuests[i]);
    }
}

function handleQuickMoveSelection(selection) {
    if (selection < 0 || selection >= quickMoveMaps.length) {
        cm.dispose();
        return;
    }

    cm.warp(quickMoveMaps[selection][1]);
    cm.dispose();
}

function returnToNearestTown() {
    cm.warp(cm.getPlayer().getMap().getReturnMapId());
    cm.dispose();
}

function openQuickShop() {
    cm.openShopNPC(getNearestTownPotionShop());
    cm.dispose();
}

function openQuickStorage() {
    if (cm.getPlayer().getShop() != null) {
        cm.dropMessage(1, "Please close the shop UI first.");
        cm.dispose();
        return;
    }

    cm.getPlayer().getStorage().sendStorage(cm.getClient(), HENESYS_STORAGE);
    cm.dispose();
}

function getNearestTownPotionShop() {
    var nearestTown = cm.getPlayer().getMap().getReturnMapId();

    for (var i = 0; i < nearestTownPotionShops.length; i++) {
        if (nearestTownPotionShops[i][0] == nearestTown) {
            return nearestTownPotionShops[i][1];
        }
    }

    return HENESYS_POTION_SHOP;
}

function buildQuickMoveSelection() {
    var text = "Where would you like to go?#b";

    for (var i = 0; i < quickMoveMaps.length; i++) {
        text += "\r\n#L" + i + "##m" + quickMoveMaps[i][1] + "##l";
    }

    return text;
}
