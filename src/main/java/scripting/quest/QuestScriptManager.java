/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.quest;

import client.Client;
import client.QuestStatus;
import constants.game.GameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractScriptManager;
import scripting.ScriptHandle;
import scripting.ScriptInvocationContext;
import server.quest.Quest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author RMZero213
 */
public class QuestScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(QuestScriptManager.class);
    private static final QuestScriptManager instance = new QuestScriptManager();

    private final Map<Client, QuestActionManager> qms = new HashMap<>();
    private final Map<Client, ScriptHandle> scripts = new HashMap<>();
    private final Map<Client, String> scriptIdentifiers = new HashMap<>();

    public static QuestScriptManager getInstance() {
        return instance;
    }

    private LoadedQuestScript getQuestScript(Client c, short questid) {
        ScriptHandle handle = loadScript("quest", String.valueOf(questid), c);
        String identifier = String.valueOf(questid);
        if (handle == null && GameConstants.isMedalQuest(questid)) {
            identifier = "medalQuest";
            handle = loadScript("quest", identifier, c);   // start generic medal quest
        }

        return handle == null ? null : new LoadedQuestScript(handle, identifier);
    }

    public void start(Client c, short questid, int npc) {
        Quest quest = Quest.getInstance(questid);
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, true);
            if (qms.containsKey(c)) {
                return;
            }
            if (c.canClickNPC()) {
                qms.put(c, qm);

                if (!quest.hasScriptRequirement(false)) {   // lack of scripted quest checks found thanks to Mali, Resinate
                    qm.dispose();
                    return;
                }

                LoadedQuestScript script = getQuestScript(c, questid);
                if (script == null) {
                    log.warn("START Quest {} is uncoded.", questid);
                    qm.dispose();
                    return;
                }

                scripts.put(c, script.handle());
                scriptIdentifiers.put(c, script.identifier());
                c.setClickedNPC();
                script.handle().invoke("start", ScriptInvocationContext.of("qm", qm), (byte) 1, (byte) 0, 0);
            }
        } catch (final Throwable t) {
            log.error("Error starting quest script: {}", questid, t);
            dispose(c);
        }
    }

    public void start(Client c, byte mode, byte type, int selection) {
        ScriptHandle handle = scripts.get(c);
        if (handle != null) {
            try {
                c.setClickedNPC();
                handle.invoke("start", ScriptInvocationContext.of("qm", getQM(c)), mode, type, selection);
            } catch (final Exception e) {
                log.error("Error starting quest script: {}", getQM(c).getQuest(), e);
                dispose(c);
            }
        }
    }

    public void end(Client c, short questid, int npc) {
        Quest quest = Quest.getInstance(questid);
        if (!c.getPlayer().getQuest(quest).getStatus().equals(QuestStatus.Status.STARTED) || (!c.getPlayer().getMap().containsNPC(npc) && !quest.isAutoComplete())) {
            dispose(c);
            return;
        }
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, false);
            if (qms.containsKey(c)) {
                return;
            }
            if (c.canClickNPC()) {
                qms.put(c, qm);

                if (!quest.hasScriptRequirement(true)) {
                    qm.dispose();
                    return;
                }

                LoadedQuestScript script = getQuestScript(c, questid);
                if (script == null) {
                    log.warn("END Quest {} is uncoded.", questid);
                    qm.dispose();
                    return;
                }

                scripts.put(c, script.handle());
                scriptIdentifiers.put(c, script.identifier());
                c.setClickedNPC();
                script.handle().invoke("end", ScriptInvocationContext.of("qm", qm), (byte) 1, (byte) 0, 0);
            }
        } catch (final Throwable t) {
            log.error("Error starting quest script: {}", questid, t);
            dispose(c);
        }
    }

    public void end(Client c, byte mode, byte type, int selection) {
        ScriptHandle handle = scripts.get(c);
        if (handle != null) {
            try {
                c.setClickedNPC();
                handle.invoke("end", ScriptInvocationContext.of("qm", getQM(c)), mode, type, selection);
            } catch (final Exception e) {
                log.error("Error ending quest script: {}", getQM(c).getQuest(), e);
                dispose(c);
            }
        }
    }

    public void raiseOpen(Client c, short questid, int npc) {
        try {
            QuestActionManager qm = new QuestActionManager(c, questid, npc, true);
            if (qms.containsKey(c)) {
                return;
            }
            if (c.canClickNPC()) {
                qms.put(c, qm);

                LoadedQuestScript script = getQuestScript(c, questid);
                if (script == null) {
                    //FilePrinter.printError(FilePrinter.QUEST_UNCODED, "RAISE Quest " + questid + " is uncoded.");
                    qm.dispose();
                    return;
                }

                scripts.put(c, script.handle());
                scriptIdentifiers.put(c, script.identifier());
                c.setClickedNPC();
                script.handle().invoke("raiseOpen", ScriptInvocationContext.of("qm", qm));
            }
        } catch (final Throwable t) {
            log.error("Error during quest script raiseOpen for quest: {}", questid, t);
            dispose(c);
        }
    }

    public void dispose(QuestActionManager qm, Client c) {
        qms.remove(c);
        scripts.remove(c);
        String scriptIdentifier = scriptIdentifiers.remove(c);
        c.getPlayer().setNpcCooldown(System.currentTimeMillis());
        resetContext("quest", scriptIdentifier == null ? String.valueOf(qm.getQuest()) : scriptIdentifier, c);
        c.getPlayer().flushDelayedUpdateQuests();
    }

    public void dispose(Client c) {
        QuestActionManager qm = qms.get(c);
        if (qm != null) {
            dispose(qm, c);
        }
    }

    public QuestActionManager getQM(Client c) {
        return qms.get(c);
    }

    public void reloadQuestScripts() {
        scripts.clear();
        scriptIdentifiers.clear();
        qms.clear();
    }

    private record LoadedQuestScript(ScriptHandle handle, String identifier) {
    }
}
