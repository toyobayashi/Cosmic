CREATE TABLE IF NOT EXISTS monster_park_entries
(
    characterid       INT              NOT NULL,
    entrydate         DATE             NOT NULL,
    free_entries      TINYINT UNSIGNED NOT NULL DEFAULT 0,
    additional_entries TINYINT UNSIGNED NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (characterid, entrydate)
);

CREATE TABLE IF NOT EXISTS monster_park_extreme_entries
(
    characterid INT       NOT NULL,
    week_start  DATE      NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (characterid, week_start)
);
