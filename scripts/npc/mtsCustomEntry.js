var status = -1;
var selectedMenu = -1;
var selectedQuestSkipTarget = -1;

var SKIP_BEGINNER_QUESTS = 0;
var QUICK_MOVE = 1;

var quickMoveMaps = [
    ["Lith Harbor", 104000000],
    ["Henesys", 100000000],
    ["Ellinia", 101000000],
    ["Perion", 102000000],
    ["Kerning City", 103000000],
    ["Nautilus Harbor", 120000000],
    ["Sleepywood", 105040300],
    ["Orbis", 200000000],
    ["El Nath", 211000000],
    ["Ludibrium", 220000000],
    ["Omega Sector", 221000000],
    ["Korean Folk Town", 222000000],
    ["Aquarium", 230000000],
    ["Leafre", 240000000],
    ["Mu Lung", 250000000],
    ["Herb Town", 251000000],
    ["Ariant", 260000000],
    ["Magatia", 261000000],
    ["New Leaf City", 600000000],
    ["Mushroom Shrine", 800000000],
    ["Showa Town", 801000000]
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
    }

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

function buildQuickMoveSelection() {
    var text = "Where would you like to go?#b";

    for (var i = 0; i < quickMoveMaps.length; i++) {
        text += "\r\n#L" + i + "##m" + quickMoveMaps[i][1] + "##l";
    }

    return text;
}
