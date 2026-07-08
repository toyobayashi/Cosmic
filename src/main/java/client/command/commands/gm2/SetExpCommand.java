/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.
*/
package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.Stat;
import client.command.Command;
import constants.game.ExpTable;

public class SetExpCommand extends Command {
    {
        setDescription("Set, gain, or reduce your current exp value.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !exp [+|-]<exp|max|percent%>");
            return;
        }

        int expNeeded = ExpTable.getExpNeededForLevel(player.getLevel());
        int maxSetExp = Math.max(0, expNeeded - 1);
        String value = params[0];
        long amount = parseAmount(value, expNeeded, maxSetExp);

        if (value.startsWith("+")) {
            int gain = clampToInt(amount);
            player.gainExp(gain);
            player.message("EXP gained: " + gain + ".");
        } else if (value.startsWith("-")) {
            int newExp = (int) Math.max(0, player.getExp() - amount);
            setExp(player, newExp);
            player.message("EXP reduced to " + newExp + ".");
        } else {
            int newExp = (int) Math.min(amount, maxSetExp);
            setExp(player, newExp);
            player.message("EXP set to " + newExp + ".");
        }
    }

    private static long parseAmount(String value, int expNeeded, int maxSetExp) {
        String unsignedValue = stripSign(value);
        if (value.contentEquals("max")) {
            return maxSetExp;
        }
        if (unsignedValue.endsWith("%")) {
            double percent = Double.parseDouble(unsignedValue.substring(0, unsignedValue.length() - 1));
            return Math.max(0, (long) Math.floor(expNeeded * percent / 100.0d));
        }

        return Math.max(0, Long.parseLong(unsignedValue));
    }

    private static String stripSign(String value) {
        if (value.startsWith("+") || value.startsWith("-")) {
            return value.substring(1);
        }
        return value;
    }

    private static int clampToInt(long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    private static void setExp(Character player, int newExp) {
        player.setExp(newExp);
        player.updateSingleStat(Stat.EXP, newExp);
    }
}
