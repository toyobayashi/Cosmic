package tools;

import client.BuddylistEntry;
import client.Character;
import client.Client;
import client.Job;
import client.MonsterBook;
import client.SkinColor;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Pet;
import config.ServerConfig;
import config.YamlConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.netty.buffer.Unpooled;
import net.packet.ByteBufInPacket;
import net.packet.Packet;
import net.packet.PerClientPacket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.CashShop;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PacketCreatorCharsetTest {
    private static final Charset JAPANESE_CHARSET = Charset.forName("windows-932");

    @BeforeAll
    static void loadConfig() {
        if (YamlConfig.config == null) {
            YamlConfig.config = new YamlConfig();
            YamlConfig.config.server = new ServerConfig();
        }
    }

    @Test
    void showAllCharacterInfoUsesClientCharsetForFixedNameField() {
        Client client = japaneseClient();
        Character chr = mock(Character.class);
        when(chr.getId()).thenReturn(1);
        when(chr.getName()).thenReturn("テスト名");
        when(chr.getGender()).thenReturn(0);
        when(chr.getSkinColor()).thenReturn(SkinColor.LIGHT);
        when(chr.getFace()).thenReturn(20000);
        when(chr.getHair()).thenReturn(30000);
        when(chr.getPet(0)).thenReturn(null);
        when(chr.getPet(1)).thenReturn(null);
        when(chr.getPet(2)).thenReturn(null);
        when(chr.getLevel()).thenReturn(1);
        when(chr.getJob()).thenReturn(Job.BEGINNER);
        when(chr.getStr()).thenReturn(12);
        when(chr.getDex()).thenReturn(5);
        when(chr.getInt()).thenReturn(4);
        when(chr.getLuk()).thenReturn(4);
        when(chr.getHp()).thenReturn(50);
        when(chr.getClientMaxHp()).thenReturn(50);
        when(chr.getMp()).thenReturn(5);
        when(chr.getClientMaxMp()).thenReturn(5);
        when(chr.getRemainingAp()).thenReturn(0);
        when(chr.getRemainingSp()).thenReturn(0);
        when(chr.getExp()).thenReturn(0);
        when(chr.getFame()).thenReturn(0);
        when(chr.getGachaExp()).thenReturn(0);
        when(chr.getMapId()).thenReturn(0);
        when(chr.getInitialSpawnpoint()).thenReturn(0);
        when(chr.isGM()).thenReturn(false);
        when(chr.isGmJob()).thenReturn(false);
        when(chr.getRank()).thenReturn(0);
        when(chr.getRankMove()).thenReturn(0);
        when(chr.getJobRank()).thenReturn(0);
        when(chr.getJobRankMove()).thenReturn(0);
        Inventory equip = mock(Inventory.class);
        when(equip.list()).thenReturn(Collections.emptyList());
        when(chr.getInventory(InventoryType.EQUIPPED)).thenReturn(equip);

        byte[] expected = new byte[13];
        byte[] encodedName = "テスト名".getBytes(JAPANESE_CHARSET);
        System.arraycopy(encodedName, 0, expected, 0, encodedName.length);

        try (AutoCloseable ignored = installMockConnectionPool()) {
            Packet packet = PacketCreator.showAllCharacterInfo(client, 1, Collections.singletonList(chr), false);

            int fixedNameOffset = indexOf(packet.getBytes(), expected);
            assertTrue(fixedNameOffset >= 0);
            assertArrayEquals(expected, Arrays.copyOfRange(packet.getBytes(), fixedNameOffset, fixedNameOffset + expected.length));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void charNameResponseDowngradesUnsupportedCharactersForTargetClient() {
        ByteBufInPacket packet = packetReader(PacketCreator.charNameResponse(japaneseClient(), "汉", false));

        packet.skip(2);

        assertEquals("?", packet.readString());
        assertEquals(0, packet.readByte());
    }

    @Test
    void sendNameTransferCheckDowngradesUnsupportedCharactersForTargetClient() {
        ByteBufInPacket packet = packetReader(PacketCreator.sendNameTransferCheck(japaneseClient(), "汉", true));

        packet.skip(2);

        assertEquals("?", packet.readString());
        assertEquals(0, packet.readByte());
    }

    @Test
    void charInfoUsesClientCharsetForPetNames() {
        Client client = japaneseClient();
        Character chr = spy(Character.getDefault(client));
        CashShop cashShop = mock(CashShop.class);
        Pet pet = mock(Pet.class);

        when(chr.getJob()).thenReturn(Job.BEGINNER);
        when(chr.getCashShop()).thenReturn(cashShop);
        when(chr.getMonsterBook()).thenReturn(new MonsterBook());
        when(chr.getCompletedQuests()).thenReturn(Collections.emptyList());
        when(cashShop.getWishList()).thenReturn(Collections.emptyList());
        when(pet.getUniqueId()).thenReturn(1);
        when(pet.getItemId()).thenReturn(5000000);
        when(pet.getName()).thenReturn("汉");
        when(pet.getLevel()).thenReturn((byte) 1);
        when(pet.getTameness()).thenReturn(0);
        when(pet.getFullness()).thenReturn(100);
        chr.addPet(pet);

        ByteBufInPacket packet = packetReader(PacketCreator.charInfo(client, chr));

        packet.skip(2);
        packet.readInt();
        packet.readByte();
        packet.readShort();
        packet.readShort();
        packet.readByte();

        assertEquals("", packet.readString());
        assertEquals("", packet.readString());
        assertEquals(0, packet.readByte());
        assertEquals(1, packet.readByte());
        assertEquals(5000000, packet.readInt());
        assertEquals("?", packet.readString());
    }

    @Test
    void updateBuddylistUsesTargetClientCharsetForFixedNameAndGroupFields() {
        BuddylistEntry buddy = new BuddylistEntry("汉", "汉组", 7, 2, true);

        Packet japanesePacket = resolvePacket(PacketCreator.updateBuddylist(Collections.singletonList(buddy)), japaneseClient());
        Packet chinesePacket = resolvePacket(PacketCreator.updateBuddylist(Collections.singletonList(buddy)), chineseClient());

        byte[] japaneseBytes = japanesePacket.getBytes();
        byte[] chineseBytes = chinesePacket.getBytes();

        assertArrayEquals(fixedFieldBytes("?", JAPANESE_CHARSET, 13), Arrays.copyOfRange(japaneseBytes, 8, 21));
        assertArrayEquals(fixedFieldBytes("汉", Charset.forName("windows-936"), 13), Arrays.copyOfRange(chineseBytes, 8, 21));
        assertArrayEquals(fixedFieldBytes("?组", JAPANESE_CHARSET, 13), Arrays.copyOfRange(japaneseBytes, 26, 39));
        assertArrayEquals(fixedFieldBytes("汉组", Charset.forName("windows-936"), 13), Arrays.copyOfRange(chineseBytes, 26, 39));
    }

    @Test
    void requestBuddylistAddUsesTargetClientCharsetAndFixed13ByteFields() {
        ByteBufInPacket packet = packetReader(resolvePacket(PacketCreator.requestBuddylistAdd(1001, 2002, "汉"), japaneseClient()));

        packet.skip(2);

        assertEquals(9, packet.readByte());
        assertEquals(1001, packet.readInt());
        assertEquals("?", packet.readString());
        assertEquals(1001, packet.readInt());
        assertArrayEquals(fixedFieldBytes("?", JAPANESE_CHARSET, 13), packet.readBytes(13));
        assertEquals(0x09, packet.readUnsignedByte());
        assertEquals(0xF0, packet.readUnsignedByte());
        assertEquals(0x01, packet.readUnsignedByte());
        assertEquals(0x0F, packet.readInt());
        assertArrayEquals(fixedFieldBytes("Default Group", JAPANESE_CHARSET, 13), packet.readBytes(13));
        assertEquals(0, packet.readUnsignedByte());
        assertEquals(2002, packet.readInt());
    }

    @Test
    void partyInviteUsesTargetClientCharsetForInviterName() {
        client.Client invitedClient = japaneseClient();
        client.Character inviter = mock(client.Character.class);
        net.server.world.Party party = mock(net.server.world.Party.class);
        when(inviter.getParty()).thenReturn(party);
        when(party.getId()).thenReturn(123);
        when(inviter.getName()).thenReturn("汉");

        ByteBufInPacket packet = packetReader(resolvePacket(PacketCreator.partyInvite(inviter), invitedClient));

        packet.skip(2);
        assertEquals(4, packet.readByte());
        assertEquals(123, packet.readInt());
        assertEquals("?", packet.readString());
        assertEquals(0, packet.readByte());
    }

    @Test
    void npcTalkUsesTargetClientCharset() {
        byte[] japaneseBytes = resolvePacket(PacketCreator.getNPCTalk(1012000, (byte) 0, "テスト", "00 00", (byte) 0), japaneseClient()).getBytes();
        byte[] chineseBytes = resolvePacket(PacketCreator.getNPCTalk(1012000, (byte) 0, "中文", "00 00", (byte) 0), chineseClient()).getBytes();

        byte[] expectedJapanese = "テスト".getBytes(JAPANESE_CHARSET);
        byte[] expectedChinese = "中文".getBytes(Charset.forName("windows-936"));

        assertArrayEquals(expectedJapanese, Arrays.copyOfRange(japaneseBytes, 11, 11 + expectedJapanese.length));
        assertArrayEquals(expectedChinese, Arrays.copyOfRange(chineseBytes, 11, 11 + expectedChinese.length));
    }

    @Test
    void generalChatUsesTargetClientCharset() {
        byte[] japaneseBytes = resolvePacket(PacketCreator.getChatText(1001, "テスト", false, 0), japaneseClient()).getBytes();
        byte[] chineseBytes = resolvePacket(PacketCreator.getChatText(1001, "中文", false, 0), chineseClient()).getBytes();

        byte[] expectedJapanese = "テスト".getBytes(JAPANESE_CHARSET);
        byte[] expectedChinese = "中文".getBytes(Charset.forName("windows-936"));

        assertArrayEquals(expectedJapanese, Arrays.copyOfRange(japaneseBytes, 9, 9 + expectedJapanese.length));
        assertArrayEquals(expectedChinese, Arrays.copyOfRange(chineseBytes, 9, 9 + expectedChinese.length));
    }

    private static Client japaneseClient() {
        Client client = Client.createMock();
        client.setPacketCodePage(932);
        return client;
    }

    private static Client chineseClient() {
        Client client = Client.createMock();
        client.setPacketCodePage(936);
        return client;
    }

    private static ByteBufInPacket packetReader(Packet packet) {
        return new ByteBufInPacket(Unpooled.wrappedBuffer(packet.getBytes()), JAPANESE_CHARSET);
    }

    private static Packet resolvePacket(Packet packet, Client client) {
        if (packet instanceof PerClientPacket perClientPacket) {
            return perClientPacket.forClient(client);
        }
        return packet;
    }

    private static byte[] fixedFieldBytes(String value, Charset charset, int length) {
        byte[] bytes = new byte[length];
        byte[] encoded = value.getBytes(charset);
        System.arraycopy(encoded, 0, bytes, 0, Math.min(encoded.length, Math.max(length - 1, 0)));
        return bytes;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static AutoCloseable installMockConnectionPool() throws Exception {
        Field dataSourceField = DatabaseConnection.class.getDeclaredField("dataSource");
        Field jdbiField = DatabaseConnection.class.getDeclaredField("jdbi");
        dataSourceField.setAccessible(true);
        jdbiField.setAccessible(true);

        Object previousDataSource = dataSourceField.get(null);
        Object previousJdbi = jdbiField.get(null);

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT cardid, mobid FROM monstercarddata")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        HikariDataSource dataSource = mock(HikariDataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        dataSourceField.set(null, dataSource);
        jdbiField.set(null, null);

        return () -> {
            dataSourceField.set(null, previousDataSource);
            jdbiField.set(null, previousJdbi);
        };
    }
}
