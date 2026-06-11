package provider.wz;

import io.github.toyobayashi.libwz.WzEnums;
import io.github.toyobayashi.libwz.WzFile;
import io.github.toyobayashi.libwz.WzImage;
import io.github.toyobayashi.libwz.WzImageProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataProvider;

import java.awt.Point;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link BinaryWZMapleData} faithfully mirrors XML WZ data.
 *
 * <p>Binary tests require {@code -Dwz-bin-path=/path/to/wz/files} and
 * only run when the native libwz library can be loaded successfully.</p>
 */
@DisplayName("BinaryWZMapleData vs XML equivalence")
class BinaryWZMapleDataTest {

    private static final String BIN = System.getProperty("wz-bin-path");
    private static WzFile mobWz, stringWz;

    @BeforeAll
    static void openBinaryFiles() {
        if (BIN == null) return;
        try {
            mobWz = new WzFile(Path.of(BIN, "Mob.wz").toString(),
                    (short) -1, WzEnums.MapleVersion.GMS);
            mobWz.parseWzFile();
            stringWz = new WzFile(Path.of(BIN, "String.wz").toString(),
                    (short) -1, WzEnums.MapleVersion.GMS);
            stringWz.parseWzFile();
        } catch (RuntimeException | UnsatisfiedLinkError e) {
            System.out.println("[BinaryWZTest] Skipping binary — " + e);
        }
    }

    @Test
    @DisplayName("BinaryWZMapleData wraps WzImageProperty correctly")
    void binaryMapleDataWrapsCorrectly() {
        if (mobWz == null) return;

        WzImage img = mobWz.getWzDirectory().getImageByName("0100100.img");
        assertNotNull(img);
        img.parseImage();

        WzImageProperty info = img.getFromPath("info/maxHP");
        assertNotNull(info);
        BinaryWZMapleData d = new BinaryWZMapleData(info);
        assertEquals("maxHP", d.getName());
        assertEquals(DataType.INT, d.getType());
        assertEquals(8, d.getData());

        WzImageProperty head = img.getFromPath("stand/0/lt");
        assertNotNull(head);
        BinaryWZMapleData vd = new BinaryWZMapleData(head);
        assertEquals(DataType.VECTOR, vd.getType());
        assertTrue(vd.getData() instanceof Point);
    }

    @Nested
    @DisplayName("Mob 0100100.img info subtree")
    class MobInfoSubtree {
        @Test
        @DisplayName("info fields match XML")
        void infoFields() {
            Data xmlData = xml("Mob.wz").getData("0100100.img");
            assertNotNull(xmlData);

            if (mobWz == null) return;
            WzImage img = mobWz.getWzDirectory().getImageByName("0100100.img");
            assertNotNull(img);
            img.parseImage();

            for (String f : java.util.List.of(
                    "maxHP", "maxMP", "level", "exp", "PADamage",
                    "PDDamage", "MADamage", "MDDamage", "acc", "eva",
                    "undead", "pushed", "speed")) {
                Data xmlChild = xmlData.getChildByPath("info/" + f);
                WzImageProperty binChild = img.getFromPath("info/" + f);
                assertNotNull(xmlChild, "XML: " + f);
                assertNotNull(binChild, "Bin: " + f);
                Object xv = xmlChild.getData();
                Object bv = new BinaryWZMapleData(binChild).getData();
                if (xv instanceof Number xn && bv instanceof Number bn) {
                    assertEquals(xn.doubleValue(), bn.doubleValue(), 0.01, f);
                } else {
                    assertEquals(xv, bv, f);
                }
            }
        }
    }

    @Nested
    @DisplayName("String.wz Mob.img names")
    class StringMobNames {
        @Test
        @DisplayName("known mob names match")
        void mobNames() {
            Data xmlData = xml("String.wz").getData("Mob.img");
            assertNotNull(xmlData);

            if (stringWz == null) return;
            WzImage mobImg = stringWz.getWzDirectory().getImageByName("Mob.img");
            assertNotNull(mobImg);
            mobImg.parseImage();

            record Pair(int id, String name) {}
            for (var p : java.util.List.of(
                    new Pair(100100, "Snail"),
                    new Pair(100101, "Blue Snail"),
                    new Pair(100120, "Stump"),
                    new Pair(210100, "Slime"),
                    new Pair(1110100, "Green Mushroom"))) {
                assertEquals(p.name,
                        strVal(xmlData.getChildByPath(p.id + "/name")), "XML " + p.id);
                WzImageProperty n = mobImg.getFromPath(p.id + "/name");
                assertNotNull(n, "Bin " + p.id);
                assertEquals(p.name, n.getString(), "Bin " + p.id);
            }
        }
    }

    @Nested
    @DisplayName("BinaryWZFile adapter smoke")
    class AdapterSmoke {
        @Test
        @DisplayName("String.wz adapter matches raw")
        void stringAdapter() {
            Data xmlData = xml("String.wz").getData("Mob.img");
            assertNotNull(xmlData);

            if (stringWz == null) return;
            BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, "String.wz").toString());
            Data binData = bf.getData("Mob.img");
            assertNotNull(binData);
            assertEquals("Mob.img", binData.getName());
            assertEquals(DataType.PROPERTY, binData.getType());
            assertEquals("Snail", strVal(binData.getChildByPath("100100/name")));
        }

        @Test
        @DisplayName("Mob.wz adapter matches raw")
        void mobAdapter() {
            Data xmlData = xml("Mob.wz").getData("0100100.img");
            assertNotNull(xmlData);

            if (mobWz == null) return;
            BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, "Mob.wz").toString());
            Data binData = bf.getData("0100100.img");
            assertNotNull(binData);
            assertEquals("0100100.img", binData.getName());
            assertEquals(xmlData.getChildByPath("info/maxHP").getData(),
                    binData.getChildByPath("info/maxHP").getData());
        }

        @Test
        @DisplayName("Item.wz root has subdirectories")
        void itemRoot() {
            DataProvider xmlProv = xml("Item.wz");
            assertTrue(xmlProv.getRoot().getSubdirectories().size() > 0);

            if (stringWz == null) return;
            BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, "Item.wz").toString());
            DataDirectoryEntry root = bf.getRoot();
            assertNotNull(root);
            assertTrue(root.getSubdirectories().size() > 0 || root.getFiles().size() > 0);
        }
    }

    // ---- helpers ----------------------------------------------------------

    static DataProvider xml(String wzDirName) {
        return new XMLWZFile(Path.of("wz", wzDirName));
    }

    static String strVal(Data d) {
        return d == null ? null : (String) d.getData();
    }
}
