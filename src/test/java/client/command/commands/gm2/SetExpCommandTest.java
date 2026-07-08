package client.command.commands.gm2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import client.Character;
import client.Client;
import client.Stat;
import config.ServerConfig;
import config.YamlConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetExpCommandTest {

    private final SetExpCommand command = new SetExpCommand();
    private final Client client = mock(Client.class);
    private final Character player = mock(Character.class);

    @BeforeEach
    void setUp() {
        YamlConfig.config = new YamlConfig();
        YamlConfig.config.server = new ServerConfig();
        when(client.getPlayer()).thenReturn(player);
        when(player.getLevel()).thenReturn(10);
        when(player.getExp()).thenReturn(200);
    }

    @Test
    void setsCurrentExpValue() {
        command.execute(client, new String[]{"100"});

        verify(player).setExp(100);
        verify(player).updateSingleStat(Stat.EXP, 100);
        verify(player).message("EXP set to 100.");
    }

    @Test
    void clampsCurrentExpToCurrentLevelMaximum() {
        command.execute(client, new String[]{"999999"});

        verify(player).setExp(1241);
        verify(player).updateSingleStat(Stat.EXP, 1241);
        verify(player).message("EXP set to 1241.");
    }

    @Test
    void setsCurrentExpFromPercent() {
        command.execute(client, new String[]{"50%"});

        verify(player).setExp(621);
        verify(player).updateSingleStat(Stat.EXP, 621);
        verify(player).message("EXP set to 621.");
    }

    @Test
    void gainsExpWhenValueStartsWithPlus() {
        command.execute(client, new String[]{"+100"});

        verify(player).gainExp(100);
        verify(player).message("EXP gained: 100.");
    }

    @Test
    void gainsPercentExpWhenPercentValueStartsWithPlus() {
        command.execute(client, new String[]{"+50%"});

        verify(player).gainExp(621);
        verify(player).message("EXP gained: 621.");
    }

    @Test
    void losesExpWhenValueStartsWithMinus() {
        command.execute(client, new String[]{"-50"});

        verify(player).setExp(150);
        verify(player).updateSingleStat(Stat.EXP, 150);
        verify(player).message("EXP reduced to 150.");
    }

    @Test
    void doesNotLoseExpBelowZero() {
        command.execute(client, new String[]{"-999999"});

        verify(player).setExp(0);
        verify(player).updateSingleStat(Stat.EXP, 0);
        verify(player).message("EXP reduced to 0.");
    }

    @Test
    void losesPercentExpWhenPercentValueStartsWithMinus() {
        command.execute(client, new String[]{"-10%"});

        verify(player).setExp(76);
        verify(player).updateSingleStat(Stat.EXP, 76);
        verify(player).message("EXP reduced to 76.");
    }
}
