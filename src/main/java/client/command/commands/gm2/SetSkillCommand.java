package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.Skill;
import client.SkillFactory;
import client.command.Command;

public class SetSkillCommand extends Command {
    {
        setDescription("Set a skill level. Args: <skill id> <level>");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 2) {
            player.yellowMessage("Syntax: @setskill <skill id> <level>");
            return;
        }

        final int skillId;
        final int level;
        try {
            skillId = Integer.parseInt(params[0]);
            level = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Skill id and level must be numbers.");
            return;
        }

        Skill skill = SkillFactory.getSkill(skillId);
        if (skill == null) {
            player.yellowMessage("Unknown skill id: " + skillId);
            return;
        }

        int maxLevel = skill.getMaxLevel();
        if (level < 0 || level > maxLevel) {
            player.yellowMessage("Skill level must be between 0 and " + maxLevel + ".");
            return;
        }

        player.changeSkillLevel(skill, (byte) level, maxLevel, -1);
        player.yellowMessage("Set skill " + skillId + " to level " + level + ".");
    }
}
