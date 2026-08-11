-- Normalize timestamp values written as INTEGER milliseconds by older Xerial configurations.
-- New SQLite connections use date_class=TEXT and store ISO-8601-compatible UTC text.

UPDATE accounts
SET lastlogin = CASE
    WHEN ABS(CAST(lastlogin AS INTEGER)) >= 100000000000
        THEN datetime(CAST(lastlogin AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(lastlogin AS REAL), 'unixepoch')
END
WHERE lastlogin IS NOT NULL AND typeof(lastlogin) IN ('integer', 'real');

UPDATE accounts
SET createdat = CASE
    WHEN ABS(CAST(createdat AS INTEGER)) >= 100000000000
        THEN datetime(CAST(createdat AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(createdat AS REAL), 'unixepoch')
END
WHERE typeof(createdat) IN ('integer', 'real');

UPDATE accounts
SET birthday = CASE
    WHEN ABS(CAST(birthday AS INTEGER)) >= 100000000000
        THEN datetime(CAST(birthday AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(birthday AS REAL), 'unixepoch')
END
WHERE typeof(birthday) IN ('integer', 'real');

UPDATE accounts
SET tempban = CASE
    WHEN ABS(CAST(tempban AS INTEGER)) >= 100000000000
        THEN datetime(CAST(tempban AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(tempban AS REAL), 'unixepoch')
END
WHERE typeof(tempban) IN ('integer', 'real');

UPDATE accounts
SET birthday = birthday || ' 00:00:00'
WHERE typeof(birthday) = 'text' AND length(birthday) = 10;

UPDATE characters
SET createdate = CASE
    WHEN ABS(CAST(createdate AS INTEGER)) >= 100000000000
        THEN datetime(CAST(createdate AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(createdate AS REAL), 'unixepoch')
END
WHERE typeof(createdate) IN ('integer', 'real');

UPDATE characters
SET lastLogoutTime = CASE
    WHEN ABS(CAST(lastLogoutTime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(lastLogoutTime AS REAL) / 1000, 'unixepoch')
    WHEN typeof(lastLogoutTime) IN ('integer', 'real')
        THEN datetime(CAST(lastLogoutTime AS REAL), 'unixepoch')
    ELSE lastLogoutTime
END,
lastExpGainTime = CASE
    WHEN ABS(CAST(lastExpGainTime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(lastExpGainTime AS REAL) / 1000, 'unixepoch')
    WHEN typeof(lastExpGainTime) IN ('integer', 'real')
        THEN datetime(CAST(lastExpGainTime AS REAL), 'unixepoch')
    ELSE lastExpGainTime
END
WHERE typeof(lastLogoutTime) IN ('integer', 'real')
   OR typeof(lastExpGainTime) IN ('integer', 'real');

UPDATE fredstorage
SET timestamp = CASE
    WHEN ABS(CAST(timestamp AS INTEGER)) >= 100000000000
        THEN datetime(CAST(timestamp AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(timestamp AS REAL), 'unixepoch')
END
WHERE typeof(timestamp) IN ('integer', 'real');

UPDATE famelog
SET `when` = CASE
    WHEN ABS(CAST(`when` AS INTEGER)) >= 100000000000
        THEN datetime(CAST(`when` AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(`when` AS REAL), 'unixepoch')
END
WHERE typeof(`when`) IN ('integer', 'real');

UPDATE characterexplogs
SET exp_gain_time = CASE
    WHEN ABS(CAST(exp_gain_time AS INTEGER)) >= 100000000000
        THEN datetime(CAST(exp_gain_time AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(exp_gain_time AS REAL), 'unixepoch')
END
WHERE exp_gain_time IS NOT NULL AND typeof(exp_gain_time) IN ('integer', 'real');

UPDATE namechanges
SET requestTime = CASE
    WHEN ABS(CAST(requestTime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(requestTime AS REAL) / 1000, 'unixepoch')
    WHEN typeof(requestTime) IN ('integer', 'real')
        THEN datetime(CAST(requestTime AS REAL), 'unixepoch')
    ELSE requestTime
END,
completionTime = CASE
    WHEN completionTime IS NULL THEN NULL
    WHEN ABS(CAST(completionTime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(completionTime AS REAL) / 1000, 'unixepoch')
    WHEN typeof(completionTime) IN ('integer', 'real')
        THEN datetime(CAST(completionTime AS REAL), 'unixepoch')
    ELSE completionTime
END
WHERE typeof(requestTime) IN ('integer', 'real')
   OR typeof(completionTime) IN ('integer', 'real');

UPDATE worldtransfers
SET requestTime = CASE
    WHEN ABS(CAST(requestTime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(requestTime AS REAL) / 1000, 'unixepoch')
    WHEN typeof(requestTime) IN ('integer', 'real')
        THEN datetime(CAST(requestTime AS REAL), 'unixepoch')
    ELSE requestTime
END,
completionTime = CASE
    WHEN completionTime IS NULL THEN NULL
    WHEN ABS(CAST(completionTime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(completionTime AS REAL) / 1000, 'unixepoch')
    WHEN typeof(completionTime) IN ('integer', 'real')
        THEN datetime(CAST(completionTime AS REAL), 'unixepoch')
    ELSE completionTime
END
WHERE typeof(requestTime) IN ('integer', 'real')
   OR typeof(completionTime) IN ('integer', 'real');

UPDATE hwidaccounts
SET expiresat = CASE
    WHEN ABS(CAST(expiresat AS INTEGER)) >= 100000000000
        THEN datetime(CAST(expiresat AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(expiresat AS REAL), 'unixepoch')
END
WHERE typeof(expiresat) IN ('integer', 'real');

UPDATE reports
SET reporttime = CASE
    WHEN ABS(CAST(reporttime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(reporttime AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(reporttime AS REAL), 'unixepoch')
END
WHERE typeof(reporttime) IN ('integer', 'real');

UPDATE bosslog_daily
SET attempttime = CASE
    WHEN ABS(CAST(attempttime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(attempttime AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(attempttime AS REAL), 'unixepoch')
END
WHERE typeof(attempttime) IN ('integer', 'real');

UPDATE bosslog_weekly
SET attempttime = CASE
    WHEN ABS(CAST(attempttime AS INTEGER)) >= 100000000000
        THEN datetime(CAST(attempttime AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(attempttime AS REAL), 'unixepoch')
END
WHERE typeof(attempttime) IN ('integer', 'real');

UPDATE dueypackages
SET TimeStamp = CASE
    WHEN ABS(CAST(TimeStamp AS INTEGER)) >= 100000000000
        THEN datetime(CAST(TimeStamp AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(TimeStamp AS REAL), 'unixepoch')
END
WHERE typeof(TimeStamp) IN ('integer', 'real');

UPDATE sponsor_orders
SET created_at = CASE
    WHEN ABS(CAST(created_at AS INTEGER)) >= 100000000000
        THEN datetime(CAST(created_at AS REAL) / 1000, 'unixepoch')
    WHEN typeof(created_at) IN ('integer', 'real')
        THEN datetime(CAST(created_at AS REAL), 'unixepoch')
    ELSE created_at
END,
updated_at = CASE
    WHEN updated_at IS NULL THEN NULL
    WHEN ABS(CAST(updated_at AS INTEGER)) >= 100000000000
        THEN datetime(CAST(updated_at AS REAL) / 1000, 'unixepoch')
    WHEN typeof(updated_at) IN ('integer', 'real')
        THEN datetime(CAST(updated_at AS REAL), 'unixepoch')
    ELSE updated_at
END
WHERE typeof(created_at) IN ('integer', 'real')
   OR typeof(updated_at) IN ('integer', 'real');

UPDATE monster_park_entries
SET updated_at = CASE
    WHEN ABS(CAST(updated_at AS INTEGER)) >= 100000000000
        THEN datetime(CAST(updated_at AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(updated_at AS REAL), 'unixepoch')
END
WHERE typeof(updated_at) IN ('integer', 'real');

UPDATE monster_park_extreme_entries
SET created_at = CASE
    WHEN ABS(CAST(created_at AS INTEGER)) >= 100000000000
        THEN datetime(CAST(created_at AS REAL) / 1000, 'unixepoch')
    ELSE datetime(CAST(created_at AS REAL), 'unixepoch')
END
WHERE typeof(created_at) IN ('integer', 'real');
