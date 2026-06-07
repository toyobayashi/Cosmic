package api.service;

import api.model.dto.GiveResourceDTO;
import net.server.Server;
import net.server.world.World;
import client.Character;
import client.Client;
import tools.DatabaseConnection;
import org.jdbi.v3.core.Handle;

import java.util.Map;

public class GiveService {

    public void giveResource(GiveResourceDTO dto) {
        validate(dto);

        if (dto.getCharacterId() != null) {
            giveToCharacter(dto.getCharacterId(), dto);
        } else if (dto.getCharacterName() != null) {
            Character chr = findCharacterByName(dto.getCharacterName());
            if (chr == null) {
                throw new IllegalArgumentException("Character not found: " + dto.getCharacterName());
            }
            giveToOnlineCharacter(chr, dto);
        } else if (Boolean.TRUE.equals(dto.getGlobal())) {
            giveGlobal(dto);
        } else {
            throw new IllegalArgumentException("Must specify characterId, characterName, or global=true");
        }
    }

    private void validate(GiveResourceDTO dto) {
        if (dto.getType() == null || dto.getType().isEmpty()) {
            throw new IllegalArgumentException("Type is required");
        }

        boolean hasValue = dto.getQuantity() != null || dto.getItemId() != null || dto.getRate() != null;
        if (!hasValue && !"gmLevel".equals(dto.getType()) || ("fame".equals(dto.getType()) && dto.getQuantity() == null)) {
            throw new IllegalArgumentException("Value/quantity is required for type: " + dto.getType());
        }
    }

    private void giveToCharacter(int characterId, GiveResourceDTO dto) {
        Character chr = findCharacterById(characterId);
        if (chr != null) {
            giveToOnlineCharacter(chr, dto);
        } else {
            giveToOfflineCharacter(characterId, dto);
        }
    }

    private void giveToOnlineCharacter(Character chr, GiveResourceDTO dto) {
        applyGive(chr, dto);
    }

    private void giveToOfflineCharacter(int characterId, GiveResourceDTO dto) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            switch (dto.getType()) {
                case "meso" -> handle.createUpdate("UPDATE characters SET meso = meso + ? WHERE id = ?")
                        .bind(0, dto.getQuantity()).bind(1, characterId).execute();
                case "exp" -> handle.createUpdate("UPDATE characters SET exp = exp + ? WHERE id = ?")
                        .bind(0, dto.getQuantity()).bind(1, characterId).execute();
                case "fame" -> handle.createUpdate("UPDATE characters SET fame = fame + ? WHERE id = ?")
                        .bind(0, dto.getQuantity()).bind(1, characterId).execute();
                case "nxCredit" -> {
                    int accountId = getAccountIdByCharId(handle, characterId);
                    handle.createUpdate("UPDATE accounts SET nxCredit = COALESCE(nxCredit,0) + ? WHERE id = ?")
                            .bind(0, dto.getQuantity()).bind(1, accountId).execute();
                }
                case "nxPrepaid" -> {
                    int accountId = getAccountIdByCharId(handle, characterId);
                    handle.createUpdate("UPDATE accounts SET nxPrepaid = COALESCE(nxPrepaid,0) + ? WHERE id = ?")
                            .bind(0, dto.getQuantity()).bind(1, accountId).execute();
                }
                case "maplePoint" -> {
                    int accountId = getAccountIdByCharId(handle, characterId);
                    handle.createUpdate("UPDATE accounts SET maplePoint = COALESCE(maplePoint,0) + ? WHERE id = ?")
                            .bind(0, dto.getQuantity()).bind(1, accountId).execute();
                }
                default -> throw new IllegalArgumentException("Cannot give '" + dto.getType() + "' to offline character. Character must be online.");
            }
        }
    }

    private void giveGlobal(GiveResourceDTO dto) {
        try (Handle handle = DatabaseConnection.getHandle()) {
            switch (dto.getType()) {
                case "meso" -> handle.createUpdate("UPDATE characters SET meso = meso + ?").bind(0, dto.getQuantity()).execute();
                case "exp" -> handle.createUpdate("UPDATE characters SET exp = exp + ?").bind(0, dto.getQuantity()).execute();
                case "fame" -> handle.createUpdate("UPDATE characters SET fame = fame + ?").bind(0, dto.getQuantity()).execute();
                case "nxCredit" -> handle.createUpdate("UPDATE accounts SET nxCredit = COALESCE(nxCredit,0) + ?").bind(0, dto.getQuantity()).execute();
                case "nxPrepaid" -> handle.createUpdate("UPDATE accounts SET nxPrepaid = COALESCE(nxPrepaid,0) + ?").bind(0, dto.getQuantity()).execute();
                case "maplePoint" -> handle.createUpdate("UPDATE accounts SET maplePoint = COALESCE(maplePoint,0) + ?").bind(0, dto.getQuantity()).execute();
                default -> throw new IllegalArgumentException("Global give not supported for type: " + dto.getType());
            }
        }

        // Also apply to online players
        for (World world : Server.getInstance().getWorlds()) {
            for (Character chr : world.getPlayerStorage().getAllCharacters()) {
                try {
                    applyGive(chr, dto);
                } catch (Exception ignored) {}
            }
        }
    }

    private void applyGive(Character chr, GiveResourceDTO dto) {
        Client c = chr.getClient();
        switch (dto.getType()) {
            case "meso" -> {
                chr.gainMeso(dto.getQuantity() != null ? dto.getQuantity() : 0, true);
            }
            case "exp" -> {
                chr.gainExp(dto.getQuantity() != null ? dto.getQuantity() : 0, true, false);
            }
            case "fame" -> {
                chr.setFame(chr.getFame() + (dto.getQuantity() != null ? dto.getQuantity() : 0));
                chr.updateSingleStat(client.Stat.FAME, chr.getFame());
            }
            case "nxCredit" -> {
                chr.getCashShop().gainCash(1, dto.getQuantity() != null ? dto.getQuantity() : 0);
            }
            case "nxPrepaid" -> {
                chr.getCashShop().gainCash(4, dto.getQuantity() != null ? dto.getQuantity() : 0);
            }
            case "maplePoint" -> {
                chr.getCashShop().gainCash(2, dto.getQuantity() != null ? dto.getQuantity() : 0);
            }
            case "item" -> {
                if (dto.getItemId() != null) {
                    client.inventory.manipulator.InventoryManipulator.addById(
                            c, dto.getItemId(), (short)(dto.getQuantity() != null ? dto.getQuantity() : 1), "", -1);
                }
            }
            case "expRate" -> {
                if (dto.getRate() != null) {
                    chr.getWorldServer().setExpRate(dto.getRate());
                }
            }
            case "mesoRate" -> {
                if (dto.getRate() != null) {
                    chr.getWorldServer().setMesoRate(dto.getRate());
                }
            }
            case "dropRate" -> {
                if (dto.getRate() != null) {
                    chr.getWorldServer().setDropRate(dto.getRate());
                }
            }
            case "gmLevel" -> {
                if (dto.getQuantity() != null) chr.setGM(dto.getQuantity());
            }
            default -> throw new IllegalArgumentException("Unsupported resource type: " + dto.getType());
        }
    }

    private int getAccountIdByCharId(Handle handle, int charId) {
        return handle.createQuery("SELECT accountid FROM characters WHERE id = ?")
                .bind(0, charId).mapTo(Integer.class).one();
    }

    private Character findCharacterById(int id) {
        for (World world : Server.getInstance().getWorlds()) {
            Character chr = world.getPlayerStorage().getCharacterById(id);
            if (chr != null) return chr;
        }
        return null;
    }

    private Character findCharacterByName(String name) {
        for (World world : Server.getInstance().getWorlds()) {
            Character chr = world.getPlayerStorage().getCharacterByName(name);
            if (chr != null) return chr;
        }
        return null;
    }
}
