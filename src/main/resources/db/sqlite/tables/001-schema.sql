-- SQLite schema generated from the MySQL schema. MySQL changelogs remain untouched.
-- SQLite uses INTEGER PRIMARY KEY AUTOINCREMENT for every former AUTO_INCREMENT key.
CREATE TABLE `accounts` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(13) COLLATE NOCASE NOT NULL DEFAULT '',
    password VARCHAR(128) NOT NULL DEFAULT '',
    pin VARCHAR(10) NOT NULL DEFAULT '',
    pic VARCHAR(26) NOT NULL DEFAULT '',
    loggedin TINYINT NOT NULL DEFAULT '0',
    lastlogin TEXT NULL DEFAULT NULL,
    createdat TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    birthday TEXT NOT NULL DEFAULT '2005-05-11 00:00:00',
    banned TINYINT NOT NULL DEFAULT '0',
    banreason TEXT,
    macs TEXT,
    nxCredit INT DEFAULT NULL,
    maplePoint INT DEFAULT NULL,
    nxPrepaid INT DEFAULT NULL,
    characterslots TINYINT NOT NULL DEFAULT '3',
    gender TINYINT NOT NULL DEFAULT '10',
    tempban TEXT NOT NULL DEFAULT '2005-05-11 00:00:00',
    greason TINYINT NOT NULL DEFAULT '0',
    tos TINYINT NOT NULL DEFAULT '0',
    sitelogged TEXT,
    webadmin INT DEFAULT '0',
    nick VARCHAR(20) DEFAULT NULL,
    mute INT DEFAULT '0',
    email VARCHAR(45) DEFAULT NULL,
    ip TEXT,
    rewardpoints INT NOT NULL DEFAULT '0',
    votepoints INT NOT NULL DEFAULT '0',
    hwid VARCHAR(12) NOT NULL DEFAULT '',
    language INT NOT NULL DEFAULT '2'
);
CREATE UNIQUE INDEX `idx_accounts_name` ON `accounts` (`name`);
CREATE INDEX `idx_accounts_ranking1` ON `accounts` (id, banned);
CREATE INDEX `idx_accounts_unnamed_1` ON `accounts` (id, `name`);
CREATE INDEX `idx_accounts_unnamed_2` ON `accounts` (id, nxCredit, maplePoint, nxPrepaid);

CREATE TABLE `characters` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    accountid INT NOT NULL DEFAULT '0',
    world INT NOT NULL DEFAULT '0',
    `name` VARCHAR(13) COLLATE NOCASE NOT NULL DEFAULT '',
    level INT NOT NULL DEFAULT '1',
    exp INT NOT NULL DEFAULT '0',
    gachaexp INT NOT NULL DEFAULT '0',
    str INT NOT NULL DEFAULT '12',
    dex INT NOT NULL DEFAULT '5',
    luk INT NOT NULL DEFAULT '4',
    `int` INT NOT NULL DEFAULT '4',
    hp INT NOT NULL DEFAULT '50',
    mp INT NOT NULL DEFAULT '5',
    maxhp INT NOT NULL DEFAULT '50',
    maxmp INT NOT NULL DEFAULT '5',
    meso INT NOT NULL DEFAULT '0',
    hpMpUsed INT NOT NULL DEFAULT '0',
    job INT NOT NULL DEFAULT '0',
    skincolor INT NOT NULL DEFAULT '0',
    gender INT NOT NULL DEFAULT '0',
    fame INT NOT NULL DEFAULT '0',
    fquest INT NOT NULL DEFAULT '0',
    hair INT NOT NULL DEFAULT '0',
    face INT NOT NULL DEFAULT '0',
    ap INT NOT NULL DEFAULT '0',
    sp VARCHAR(128) NOT NULL DEFAULT '0,0,0,0,0,0,0,0,0,0',
    map INT NOT NULL DEFAULT '0',
    spawnpoint INT NOT NULL DEFAULT '0',
    gm TINYINT NOT NULL DEFAULT '0',
    party INT NOT NULL DEFAULT '0',
    buddyCapacity INT NOT NULL DEFAULT '25',
    createdate TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `rank` INT NOT NULL DEFAULT '1',
    rankMove INT NOT NULL DEFAULT '0',
    jobRank INT NOT NULL DEFAULT '1',
    jobRankMove INT NOT NULL DEFAULT '0',
    guildid INT NOT NULL DEFAULT '0',
    guildrank INT NOT NULL DEFAULT '5',
    messengerid INT NOT NULL DEFAULT '0',
    messengerposition INT NOT NULL DEFAULT '4',
    mountlevel INT NOT NULL DEFAULT '1',
    mountexp INT NOT NULL DEFAULT '0',
    mounttiredness INT NOT NULL DEFAULT '0',
    omokwins INT NOT NULL DEFAULT '0',
    omoklosses INT NOT NULL DEFAULT '0',
    omokties INT NOT NULL DEFAULT '0',
    matchcardwins INT NOT NULL DEFAULT '0',
    matchcardlosses INT NOT NULL DEFAULT '0',
    matchcardties INT NOT NULL DEFAULT '0',
    MerchantMesos INT DEFAULT '0',
    HasMerchant TINYINT DEFAULT '0',
    equipslots INT NOT NULL DEFAULT '24',
    useslots INT NOT NULL DEFAULT '24',
    setupslots INT NOT NULL DEFAULT '24',
    etcslots INT NOT NULL DEFAULT '24',
    familyId INT NOT NULL DEFAULT '-1',
    monsterbookcover INT NOT NULL DEFAULT '0',
    allianceRank INT NOT NULL DEFAULT '5',
    vanquisherStage INT NOT NULL DEFAULT '0',
    ariantPoints INT NOT NULL DEFAULT '0',
    dojoPoints INT NOT NULL DEFAULT '0',
    lastDojoStage INT NOT NULL DEFAULT '0',
    finishedDojoTutorial TINYINT NOT NULL DEFAULT '0',
    vanquisherKills INT NOT NULL DEFAULT '0',
    summonValue INT NOT NULL DEFAULT '0',
    partnerId INT NOT NULL DEFAULT '0',
    marriageItemId INT NOT NULL DEFAULT '0',
    reborns INT NOT NULL DEFAULT '0',
    PQPoints INT NOT NULL DEFAULT '0',
    dataString VARCHAR(64) NOT NULL DEFAULT '',
    lastLogoutTime TEXT NOT NULL DEFAULT '2015-01-01 05:00:00',
    lastExpGainTime TEXT NOT NULL DEFAULT '2015-01-01 05:00:00',
    partySearch TINYINT NOT NULL DEFAULT '1',
    jailexpire BIGINT NOT NULL DEFAULT '0'
);
CREATE INDEX `idx_characters_accountid` ON `characters` (accountid);
CREATE INDEX `idx_characters_party` ON `characters` (party);
CREATE INDEX `idx_characters_ranking1` ON `characters` (`level`, exp);
CREATE INDEX `idx_characters_ranking2` ON `characters` (gm, job);
CREATE INDEX `idx_characters_unnamed_1` ON `characters` (id, accountid, world);
CREATE INDEX `idx_characters_unnamed_2` ON `characters` (id, accountid, `name`);

CREATE TABLE `inventoryitems` (
    inventoryitemid INTEGER PRIMARY KEY AUTOINCREMENT,
    type TINYINT NOT NULL,
    characterid INT DEFAULT NULL,
    accountid INT DEFAULT NULL,
    itemid INT NOT NULL DEFAULT '0',
    inventorytype INT NOT NULL DEFAULT '0',
    position INT NOT NULL DEFAULT '0',
    quantity INT NOT NULL DEFAULT '0',
    owner TEXT NOT NULL,
    petid INT NOT NULL DEFAULT '-1',
    flag INT NOT NULL,
    expiration BIGINT NOT NULL DEFAULT '-1',
    giftFrom VARCHAR(26) NOT NULL
);
CREATE INDEX `idx_inventoryitems_CHARID` ON `inventoryitems` (characterid);

CREATE TABLE `inventoryequipment` (
    inventoryequipmentid INTEGER PRIMARY KEY AUTOINCREMENT,
    inventoryitemid INT NOT NULL DEFAULT '0',
    upgradeslots INT NOT NULL DEFAULT '0',
    level INT NOT NULL DEFAULT '0',
    str INT NOT NULL DEFAULT '0',
    dex INT NOT NULL DEFAULT '0',
    `int` INT NOT NULL DEFAULT '0',
    luk INT NOT NULL DEFAULT '0',
    hp INT NOT NULL DEFAULT '0',
    mp INT NOT NULL DEFAULT '0',
    watk INT NOT NULL DEFAULT '0',
    matk INT NOT NULL DEFAULT '0',
    wdef INT NOT NULL DEFAULT '0',
    mdef INT NOT NULL DEFAULT '0',
    acc INT NOT NULL DEFAULT '0',
    avoid INT NOT NULL DEFAULT '0',
    hands INT NOT NULL DEFAULT '0',
    speed INT NOT NULL DEFAULT '0',
    jump INT NOT NULL DEFAULT '0',
    locked INT NOT NULL DEFAULT '0',
    vicious INT NOT NULL DEFAULT '0',
    itemlevel INT NOT NULL DEFAULT '1',
    itemexp INT NOT NULL DEFAULT '0',
    ringid INT NOT NULL DEFAULT '-1'
);
CREATE INDEX `idx_inventoryequipment_INVENTORYITEMID` ON `inventoryequipment` (inventoryitemid);

CREATE TABLE `inventorymerchant` (
    inventorymerchantid INTEGER PRIMARY KEY AUTOINCREMENT,
    inventoryitemid INT NOT NULL DEFAULT '0',
    characterid INT DEFAULT NULL,
    bundles INT NOT NULL DEFAULT '0'
);
CREATE INDEX `idx_inventorymerchant_INVENTORYITEMID` ON `inventorymerchant` (inventoryitemid);

CREATE TABLE `skills` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    skillid INT NOT NULL DEFAULT '0',
    characterid INT NOT NULL DEFAULT '0',
    skilllevel INT NOT NULL DEFAULT '0',
    masterlevel INT NOT NULL DEFAULT '0',
    expiration BIGINT NOT NULL DEFAULT '-1',
    FOREIGN KEY (characterid) REFERENCES characters (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX `idx_skills_skillpair` ON `skills` (skillid, characterid);

CREATE TABLE `cooldowns` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    charid INT NOT NULL,
    SkillID INT NOT NULL,
    length BIGINT NOT NULL,
    StartTime BIGINT NOT NULL
);

CREATE TABLE `skillmacros` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL DEFAULT '0',
    position TINYINT NOT NULL DEFAULT '0',
    skill1 INT NOT NULL DEFAULT '0',
    skill2 INT NOT NULL DEFAULT '0',
    skill3 INT NOT NULL DEFAULT '0',
    name VARCHAR(13) DEFAULT NULL,
    shout TINYINT NOT NULL DEFAULT '0'
);

CREATE TABLE `pets` (
    petid INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(13) DEFAULT NULL,
    level INT NOT NULL,
    closeness INT NOT NULL,
    fullness INT NOT NULL,
    summoned TINYINT NOT NULL DEFAULT '0',
    flag INT NOT NULL DEFAULT '0'
);

CREATE TABLE `petignores` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    petid INT NOT NULL,
    itemid INT NOT NULL,
    CONSTRAINT fk_petignorepetid FOREIGN KEY (petid) REFERENCES pets (petid) ON DELETE CASCADE
);

CREATE TABLE `questactions` (
    questactionid INTEGER PRIMARY KEY AUTOINCREMENT,
    questid INT NOT NULL DEFAULT '0',
    status INT NOT NULL DEFAULT '0',
    data BLOB NOT NULL
);

CREATE TABLE `questprogress` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    queststatusid INT NOT NULL DEFAULT '0',
    progressid INT NOT NULL DEFAULT '0',
    progress VARCHAR(15) NOT NULL DEFAULT ''
);

CREATE TABLE `questrequirements` (
    questrequirementid INTEGER PRIMARY KEY AUTOINCREMENT,
    questid INT NOT NULL DEFAULT '0',
    status INT NOT NULL DEFAULT '0',
    data BLOB NOT NULL
);

CREATE TABLE `queststatus` (
    queststatusid INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL DEFAULT '0',
    quest INT NOT NULL DEFAULT '0',
    status INT NOT NULL DEFAULT '0',
    time INT NOT NULL DEFAULT '0',
    expires BIGINT NOT NULL DEFAULT '0',
    forfeited INT NOT NULL DEFAULT '0',
    completed INT NOT NULL DEFAULT '0',
    info TINYINT NOT NULL DEFAULT '0'
);

CREATE TABLE `area_info` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    charid INT NOT NULL,
    area INT NOT NULL,
    info VARCHAR(200) NOT NULL
);

CREATE TABLE `eventstats` (
    characterid INT NOT NULL,
    name VARCHAR(11) NOT NULL DEFAULT '0',
    info INT NOT NULL,
    PRIMARY KEY (characterid)
);

CREATE TABLE `medalmaps` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    queststatusid INT NOT NULL,
    mapid INT NOT NULL
);
CREATE INDEX `idx_medalmaps_queststatusid` ON `medalmaps` (queststatusid);

CREATE TABLE `guilds` (
    guildid INTEGER PRIMARY KEY AUTOINCREMENT,
    leader INT NOT NULL DEFAULT '0',
    GP INT NOT NULL DEFAULT '0',
    logo INT DEFAULT NULL,
    logoColor SMALLINT NOT NULL DEFAULT '0',
    name VARCHAR(45) COLLATE NOCASE NOT NULL,
    rank1title VARCHAR(45) NOT NULL DEFAULT 'Master',
    rank2title VARCHAR(45) NOT NULL DEFAULT 'Jr. Master',
    rank3title VARCHAR(45) NOT NULL DEFAULT 'Member',
    rank4title VARCHAR(45) NOT NULL DEFAULT 'Member',
    rank5title VARCHAR(45) NOT NULL DEFAULT 'Member',
    capacity INT NOT NULL DEFAULT '10',
    logoBG INT DEFAULT NULL,
    logoBGColor SMALLINT NOT NULL DEFAULT '0',
    notice VARCHAR(101) DEFAULT NULL,
    signature INT NOT NULL DEFAULT '0',
    allianceId INT NOT NULL DEFAULT '0'
);
CREATE INDEX `idx_guilds_unnamed_1` ON `guilds` (guildid, name);

CREATE TABLE `bbs_replies` (
    replyid INTEGER PRIMARY KEY AUTOINCREMENT,
    threadid INT NOT NULL,
    postercid INT NOT NULL,
    timestamp BIGINT NOT NULL,
    content VARCHAR(26) NOT NULL DEFAULT ''
);

CREATE TABLE `bbs_threads` (
    threadid INTEGER PRIMARY KEY AUTOINCREMENT,
    postercid INT NOT NULL,
    name VARCHAR(26) NOT NULL DEFAULT '',
    timestamp BIGINT NOT NULL,
    icon SMALLINT NOT NULL,
    replycount SMALLINT NOT NULL DEFAULT '0',
    startpost TEXT NOT NULL,
    guildid INT NOT NULL,
    localthreadid INT NOT NULL
);

CREATE TABLE `alliance` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(13) COLLATE NOCASE NOT NULL,
    capacity INT NOT NULL DEFAULT '2',
    notice VARCHAR(20) NOT NULL DEFAULT '',
    rank1 VARCHAR(11) NOT NULL DEFAULT 'Master',
    rank2 VARCHAR(11) NOT NULL DEFAULT 'Jr. Master',
    rank3 VARCHAR(11) NOT NULL DEFAULT 'Member',
    rank4 VARCHAR(11) NOT NULL DEFAULT 'Member',
    rank5 VARCHAR(11) NOT NULL DEFAULT 'Member'
);
CREATE INDEX `idx_alliance_unnamed_1` ON `alliance` (name);

CREATE TABLE `allianceguilds` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    allianceid INT NOT NULL DEFAULT '-1',
    guildid INT NOT NULL DEFAULT '-1'
);

CREATE TABLE `keymap` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL DEFAULT '0',
    `key` INT NOT NULL DEFAULT '0',
    type INT NOT NULL DEFAULT '0',
    action INT NOT NULL DEFAULT '0'
);

CREATE TABLE `quickslotkeymapped` (
    accountid INT NOT NULL,
    keymap BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (accountid),
    FOREIGN KEY (accountid) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE TABLE `drop_data` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dropperid INT NOT NULL,
    itemid INT NOT NULL DEFAULT '0',
    minimum_quantity INT NOT NULL DEFAULT '1',
    maximum_quantity INT NOT NULL DEFAULT '1',
    questid INT NOT NULL DEFAULT '0',
    chance INT NOT NULL DEFAULT '0'
);
CREATE INDEX `idx_drop_data_mobid` ON `drop_data` (dropperid);
CREATE INDEX `idx_drop_data_unnamed_1` ON `drop_data` (dropperid, itemid);

CREATE TABLE `drop_data_global` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    continent TINYINT NOT NULL DEFAULT '-1',
    itemid INT NOT NULL DEFAULT '0',
    minimum_quantity INT NOT NULL DEFAULT '1',
    maximum_quantity INT NOT NULL DEFAULT '1',
    questid INT NOT NULL DEFAULT '0',
    chance INT NOT NULL DEFAULT '0',
    comments VARCHAR(45) DEFAULT NULL
);
CREATE INDEX `idx_drop_data_global_mobid` ON `drop_data_global` (continent);

CREATE TABLE `reactordrops` (
    reactordropid INTEGER PRIMARY KEY AUTOINCREMENT,
    reactorid INT NOT NULL,
    itemid INT NOT NULL,
    chance INT NOT NULL,
    questid INT NOT NULL DEFAULT '-1'
);
CREATE INDEX `idx_reactordrops_reactorid` ON `reactordrops` (reactorid);

CREATE TABLE `storages` (
    storageid INTEGER PRIMARY KEY AUTOINCREMENT,
    accountid INT NOT NULL DEFAULT '0',
    world INT NOT NULL,
    slots INT NOT NULL DEFAULT '0',
    meso INT NOT NULL DEFAULT '0'
);

CREATE TABLE `fredstorage` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cid INT NOT NULL,
    daynotes INT NOT NULL,
    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX `idx_fredstorage_cid_2` ON `fredstorage` (cid);

CREATE TABLE `shops` (
    shopid INTEGER PRIMARY KEY AUTOINCREMENT,
    npcid INT NOT NULL DEFAULT '0'
);

CREATE TABLE `shopitems` (
    shopitemid INTEGER PRIMARY KEY AUTOINCREMENT,
    shopid INT NOT NULL,
    itemid INT NOT NULL,
    price INT NOT NULL,
    pitch INT NOT NULL DEFAULT '0',
    position INT NOT NULL
);

CREATE TABLE `playerdiseases` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    charid INT NOT NULL,
    disease INT NOT NULL,
    mobskillid INT NOT NULL,
    mobskilllv INT NOT NULL,
    length INT NOT NULL DEFAULT '1'
);

CREATE TABLE `buddies` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    buddyid INT NOT NULL,
    pending TINYINT NOT NULL DEFAULT '0',
    `group` VARCHAR(17) DEFAULT '0'
);

CREATE TABLE `savedlocations` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    locationtype TEXT NOT NULL CHECK (locationtype IN ('FREE_MARKET','WORLDTOUR','FLORINA','INTRO','SUNDAY_MARKET','MIRROR','EVENT','BOSSPQ','HAPPYVILLE','MONSTER_CARNIVAL','MONSTER_PARK','DEVELOPER')),
    map INT NOT NULL,
    portal INT NOT NULL
);

CREATE TABLE `famelog` (
    famelogid INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL DEFAULT '0',
    characterid_to INT NOT NULL DEFAULT '0',
    `when` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (characterid) REFERENCES characters (id) ON DELETE CASCADE
);
CREATE INDEX `idx_famelog_characterid` ON `famelog` (characterid);

CREATE TABLE `trocklocations` (
    trockid INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    mapid INT NOT NULL,
    vip INT NOT NULL
);

CREATE TABLE `characterexplogs` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world_exp_rate REAL,
    exp_coupon REAL,
    gained_exp BIGINT,
    current_exp INT,
    exp_gain_time TEXT,
    charid INT NOT NULL,
    FOREIGN KEY (charid) REFERENCES characters (id) ON DELETE CASCADE
);

CREATE TABLE `wishlists` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    charid INT NOT NULL,
    sn INT NOT NULL
);

CREATE TABLE `specialcashitems` (
    id INT NOT NULL,
    sn INT NOT NULL,
    modifier INT NOT NULL,
    info INT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE `nxcode` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(17) NOT NULL UNIQUE,
    retriever VARCHAR(13) DEFAULT NULL,
    expiration BIGINT NOT NULL DEFAULT '0'
);

CREATE TABLE `nxcode_items` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codeid INT NOT NULL,
    type INT NOT NULL DEFAULT '5',
    item INT NOT NULL DEFAULT '4000000',
    quantity INT NOT NULL DEFAULT '1'
);

CREATE TABLE `nxcoupons` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    couponid INT NOT NULL DEFAULT '0',
    rate INT NOT NULL DEFAULT '0',
    activeday INT NOT NULL DEFAULT '0',
    starthour INT NOT NULL DEFAULT '0',
    endhour INT NOT NULL DEFAULT '0'
);

CREATE TABLE `gifts` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    `to` INT NOT NULL,
    `from` VARCHAR(13) NOT NULL,
    message TEXT NOT NULL,
    sn INT NOT NULL,
    ringid INT NOT NULL
);

CREATE TABLE `notes` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    `to` VARCHAR(13) NOT NULL DEFAULT '',
    `from` VARCHAR(13) NOT NULL DEFAULT '',
    message TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    fame INT NOT NULL DEFAULT '0',
    deleted INT NOT NULL DEFAULT '0'
);

CREATE TABLE `newyear` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    senderid INT NOT NULL DEFAULT '-1',
    sendername VARCHAR(13) DEFAULT '',
    receiverid INT NOT NULL DEFAULT '-1',
    receivername VARCHAR(13) DEFAULT '',
    message VARCHAR(120) DEFAULT '',
    senderdiscard TINYINT NOT NULL DEFAULT '0',
    receiverdiscard TINYINT NOT NULL DEFAULT '0',
    received TINYINT NOT NULL DEFAULT '0',
    timesent BIGINT NOT NULL,
    timereceived BIGINT NOT NULL
);

CREATE TABLE `marriages` (
    marriageid INTEGER PRIMARY KEY AUTOINCREMENT,
    husbandid INT NOT NULL DEFAULT '0',
    wifeid INT NOT NULL DEFAULT '0'
);

CREATE TABLE `rings` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    partnerRingId INT NOT NULL DEFAULT '0',
    partnerChrId INT NOT NULL DEFAULT '0',
    itemid INT NOT NULL DEFAULT '0',
    partnername VARCHAR(255) NOT NULL
);

CREATE TABLE `monsterbook` (
    charid INT NOT NULL,
    cardid INT NOT NULL,
    level INT NOT NULL DEFAULT '1',
    PRIMARY KEY (charid, cardid),
    FOREIGN KEY (charid) REFERENCES characters (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE `monstercarddata` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cardid INT NOT NULL DEFAULT '0',
    mobid INT NOT NULL DEFAULT '0'
);
CREATE UNIQUE INDEX `idx_monstercarddata_id` ON `monstercarddata` (id);

CREATE TABLE `family_character` (
    cid INT NOT NULL,
    familyid INT NOT NULL,
    seniorid INT NOT NULL,
    reputation INT NOT NULL DEFAULT '0',
    todaysrep INT NOT NULL DEFAULT '0',
    totalreputation INT NOT NULL DEFAULT '0',
    reptosenior INT NOT NULL DEFAULT '0',
    precepts VARCHAR(200) DEFAULT NULL,
    lastresettime BIGINT NOT NULL DEFAULT '0',
    PRIMARY KEY (cid),
    FOREIGN KEY (cid) REFERENCES characters (`id`) ON DELETE CASCADE
);
CREATE INDEX `idx_family_character_unnamed_1` ON `family_character` (cid, familyid);

CREATE TABLE `family_entitlement` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    charid INT NOT NULL,
    entitlementid INT NOT NULL,
    timestamp BIGINT NOT NULL DEFAULT '0'
);
CREATE INDEX `idx_family_entitlement_unnamed_1` ON `family_entitlement` (charid);

CREATE TABLE `namechanges` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    old VARCHAR(13) NOT NULL,
    new VARCHAR(13) NOT NULL,
    requestTime TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completionTime TEXT NULL
);
CREATE INDEX `idx_namechanges_unnamed_1` ON `namechanges` (characterid);

CREATE TABLE `worldtransfers` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    `from` TINYINT NOT NULL,
    `to` TINYINT NOT NULL,
    requestTime TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completionTime TEXT NULL
);
CREATE INDEX `idx_worldtransfers_unnamed_1` ON `worldtransfers` (characterid);

CREATE TABLE `mts_cart` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cid INT NOT NULL,
    itemid INT NOT NULL
);

CREATE TABLE `mts_items` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tab INT NOT NULL DEFAULT '0',
    type INT NOT NULL DEFAULT '0',
    itemid INT NOT NULL DEFAULT '0',
    quantity INT NOT NULL DEFAULT '1',
    seller INT NOT NULL DEFAULT '0',
    price INT NOT NULL DEFAULT '0',
    bid_incre INT DEFAULT '0',
    buy_now INT DEFAULT '0',
    position INT DEFAULT '0',
    upgradeslots INT DEFAULT '0',
    level INT DEFAULT '0',
    itemlevel INT NOT NULL DEFAULT '1',
    itemexp INT NOT NULL DEFAULT '0',
    ringid INT NOT NULL DEFAULT '-1',
    str INT DEFAULT '0',
    dex INT DEFAULT '0',
    `int` INT DEFAULT '0',
    luk INT DEFAULT '0',
    hp INT DEFAULT '0',
    mp INT DEFAULT '0',
    watk INT DEFAULT '0',
    matk INT DEFAULT '0',
    wdef INT DEFAULT '0',
    mdef INT DEFAULT '0',
    acc INT DEFAULT '0',
    avoid INT DEFAULT '0',
    hands INT DEFAULT '0',
    speed INT DEFAULT '0',
    jump INT DEFAULT '0',
    locked INT DEFAULT '0',
    isequip INT DEFAULT '0',
    owner VARCHAR(16) DEFAULT '',
    sellername VARCHAR(16) NOT NULL,
    sell_ends VARCHAR(16) NOT NULL,
    transfer INT DEFAULT '0',
    vicious INT NOT NULL DEFAULT '0',
    flag INT NOT NULL DEFAULT '0',
    expiration BIGINT NOT NULL DEFAULT '-1',
    giftFrom VARCHAR(26) NOT NULL
);

CREATE TABLE `makercreatedata` (
    id TINYINT NOT NULL,
    itemid INT NOT NULL,
    req_level TINYINT NOT NULL,
    req_maker_level TINYINT NOT NULL,
    req_meso INT NOT NULL,
    req_item INT NOT NULL,
    req_equip INT NOT NULL,
    catalyst INT NOT NULL,
    quantity SMALLINT NOT NULL,
    tuc TINYINT NOT NULL,
    PRIMARY KEY (id, itemid)
);

CREATE TABLE `makerrecipedata` (
    itemid INT NOT NULL,
    req_item INT NOT NULL,
    count SMALLINT NOT NULL,
    PRIMARY KEY (itemid, req_item)
);

CREATE TABLE `makerrewarddata` (
    itemid INT NOT NULL,
    rewardid INT NOT NULL,
    quantity SMALLINT NOT NULL,
    prob TINYINT NOT NULL DEFAULT '100',
    PRIMARY KEY (itemid, rewardid)
);

CREATE TABLE `makerreagentdata` (
    itemid INT NOT NULL,
    stat VARCHAR(20) NOT NULL,
    value SMALLINT NOT NULL,
    PRIMARY KEY (itemid)
);

CREATE TABLE `playernpcs` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(13) NOT NULL,
    hair INT NOT NULL,
    face INT NOT NULL,
    skin INT NOT NULL,
    gender INT NOT NULL DEFAULT '0',
    x INT NOT NULL,
    cy INT NOT NULL DEFAULT '0',
    world INT NOT NULL DEFAULT '0',
    map INT NOT NULL DEFAULT '0',
    dir INT NOT NULL DEFAULT '0',
    scriptid INT NOT NULL DEFAULT '0',
    fh INT NOT NULL DEFAULT '0',
    rx0 INT NOT NULL DEFAULT '0',
    rx1 INT NOT NULL DEFAULT '0',
    worldrank INT NOT NULL DEFAULT '0',
    overallrank INT NOT NULL DEFAULT '0',
    worldjobrank INT NOT NULL DEFAULT '0',
    job INT NOT NULL DEFAULT '0'
);

CREATE TABLE `playernpcs_equip` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    npcid INT NOT NULL DEFAULT '0',
    equipid INT NOT NULL,
    type INT NOT NULL DEFAULT '0',
    equippos INT NOT NULL
);

CREATE TABLE `playernpcs_field` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world INT NOT NULL,
    map INT NOT NULL,
    step TINYINT NOT NULL DEFAULT '0',
    podium SMALLINT NOT NULL DEFAULT '0'
);

CREATE TABLE `plife` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    world INT NOT NULL DEFAULT '-1',
    map INT NOT NULL DEFAULT '0',
    life INT NOT NULL DEFAULT '0',
    type VARCHAR(1) NOT NULL DEFAULT 'n',
    cy INT NOT NULL DEFAULT '0',
    f INT NOT NULL DEFAULT '0',
    fh INT NOT NULL DEFAULT '0',
    rx0 INT NOT NULL DEFAULT '0',
    rx1 INT NOT NULL DEFAULT '0',
    x INT NOT NULL DEFAULT '0',
    y INT NOT NULL DEFAULT '0',
    hide INT NOT NULL DEFAULT '0',
    mobtime INT NOT NULL DEFAULT '0',
    team INT NOT NULL DEFAULT '0'
);

CREATE TABLE `hwidaccounts` (
    accountid INT NOT NULL DEFAULT '0',
    hwid VARCHAR(40) NOT NULL DEFAULT '',
    relevance TINYINT NOT NULL DEFAULT '0',
    expiresat TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (accountid, hwid)
);

CREATE TABLE `hwidbans` (
    hwidbanid INTEGER PRIMARY KEY AUTOINCREMENT,
    hwid VARCHAR(30) NOT NULL
);
CREATE UNIQUE INDEX `idx_hwidbans_hwid_2` ON `hwidbans` (hwid);

CREATE TABLE `ipbans` (
    ipbanid INTEGER PRIMARY KEY AUTOINCREMENT,
    ip VARCHAR(40) NOT NULL DEFAULT '',
    aid VARCHAR(40) DEFAULT NULL
);

CREATE TABLE `macbans` (
    macbanid INTEGER PRIMARY KEY AUTOINCREMENT,
    mac VARCHAR(30) NOT NULL,
    aid VARCHAR(40) DEFAULT NULL
);
CREATE UNIQUE INDEX `idx_macbans_mac_2` ON `macbans` (mac);

CREATE TABLE `macfilters` (
    macfilterid INTEGER PRIMARY KEY AUTOINCREMENT,
    filter VARCHAR(30) NOT NULL
);

CREATE TABLE `reports` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reporttime TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reporterid INT NOT NULL,
    victimid INT NOT NULL,
    reason TINYINT NOT NULL,
    chatlog TEXT NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE `bosslog_daily` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    bosstype TEXT NOT NULL CHECK (bosstype IN ('ZAKUM','HORNTAIL','PINKBEAN','SCARGA','PAPULATUS')),
    attempttime TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `bosslog_weekly` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterid INT NOT NULL,
    bosstype TEXT NOT NULL CHECK (bosstype IN ('ZAKUM','HORNTAIL','PINKBEAN','SCARGA','PAPULATUS')),
    attempttime TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `dueypackages` (
    PackageId INTEGER PRIMARY KEY AUTOINCREMENT,
    ReceiverId INT NOT NULL,
    SenderName VARCHAR(13) NOT NULL,
    Mesos INT DEFAULT '0',
    TimeStamp TEXT NOT NULL DEFAULT '2015-01-01 05:00:00',
    Message VARCHAR(200) NULL,
    Checked TINYINT DEFAULT '1',
    Type TINYINT DEFAULT '0'
);

CREATE TABLE `dueyitems` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    PackageId INT NOT NULL DEFAULT '0',
    inventoryitemid INT NOT NULL DEFAULT '0',
    FOREIGN KEY (PackageId) REFERENCES dueypackages (PackageId) ON DELETE CASCADE
);
CREATE INDEX `idx_dueyitems_INVENTORYITEMID` ON `dueyitems` (inventoryitemid);
CREATE INDEX `idx_dueyitems_PackageId` ON `dueyitems` (PackageId);

CREATE TABLE `sponsor_orders` (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id CHAR(24) NOT NULL,
    account VARCHAR(13) NOT NULL,
    amount_cents INT NOT NULL,
    expected_nx INT NOT NULL,
    awarded_nx INT DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    note VARCHAR(255) DEFAULT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NULL DEFAULT NULL,
    updated_by INT DEFAULT NULL,
    CONSTRAINT fk_sponsor_account FOREIGN KEY (account) REFERENCES accounts (`name`)
);
CREATE UNIQUE INDEX `idx_sponsor_orders_order_id` ON `sponsor_orders` (order_id);
CREATE INDEX `idx_sponsor_orders_idx_sponsor_status_created` ON `sponsor_orders` (status, created_at);
CREATE INDEX `idx_sponsor_orders_idx_sponsor_account_status` ON `sponsor_orders` (account, status);

CREATE TABLE `monster_park_entries` (
    `characterid` INTEGER NOT NULL,
    `entrydate` TEXT NOT NULL,
    `free_entries` INTEGER NOT NULL DEFAULT 0,
    `additional_entries` INTEGER NOT NULL DEFAULT 0,
    `updated_at` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`characterid`, `entrydate`)
);
CREATE TABLE `monster_park_extreme_entries` (
    `characterid` INTEGER NOT NULL,
    `week_start` TEXT NOT NULL,
    `created_at` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`characterid`, `week_start`)
);
