package api.service;

import client.Character;
import net.server.Server;
import net.server.world.World;
import server.CashShop;

public class OnlineSponsorCashRefresher implements SponsorCashRefresher {
    @Override
    public void increaseNxCredit(int account, int nx) {
        for (World world : Server.getInstance().getWorlds()) {
            for (Character chr : world.getPlayerStorage().getAllCharacters()) {
                if (chr.getAccountID() == account) {
                    chr.getCashShop().gainCash(CashShop.NX_CREDIT, nx);
                }
            }
        }
    }
}
