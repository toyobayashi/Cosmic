package provider.wz;

import config.YamlConfig;
import io.github.toyobayashi.libwz.WzEnums;
import io.github.toyobayashi.libwz.WzFile;
import io.github.toyobayashi.libwz.WzImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataProvider;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BinaryWZMapleData vs XML")
class BinaryWZMapleDataTest {

    static final String BIN = System.getProperty("wz-bin-path", Paths.get(System.getProperty("user.home"), "kinoko", "MapleStory").toAbsolutePath().toString());
    static final Map<String, WzFile> wzFiles = new HashMap<>();

    @BeforeAll
    static void openBinaryFiles() {
        YamlConfig.load();
        if (BIN == null) return;
        for (String name : List.of("Mob", "String", "Skill", "Map", "Item",
                "Quest", "Reactor", "Etc", "Character", "Npc", "UI", "Sound",
                "Effect", "Morph", "TamingMob")) {
            try {
                WzFile f = new WzFile(Path.of(BIN, name + ".wz").toString(),
                        (short) -1, WzEnums.MapleVersion.GMS);
                f.parseWzFile();
                wzFiles.put(name, f);
            } catch (RuntimeException | UnsatisfiedLinkError e) {
                System.out.println("[WZTest] Skip " + name + ": " + e);
            }
        }
    }

    static DataProvider xml(String wzDirName) {
        return new XMLWZFile(Path.of("wz", wzDirName));
    }

    static String strVal(Data d) { return d == null ? null : (String) d.getData(); }

    // =================================================================
    //  Recursive tree comparison helper
    // =================================================================

    static void assertDataEquals(Data xml, Data bin, String path) {
        assertNotNull(xml, "XML null at " + path);
        assertNotNull(bin, "Binary null at " + path);
        assertEquals(xml.getName(), bin.getName(), "Name at " + path);
        assertEquals(xml.getType(), bin.getType(), "Type at " + path);

        Object xv = xml.getData();
        Object bv = bin.getData();
        if (xv instanceof Point xp && bv instanceof Point bp) {
            assertEquals(xp.x, bp.x, "Point.x at " + path);
            assertEquals(xp.y, bp.y, "Point.y at " + path);
        } else if (xv instanceof Number xn && bv instanceof Number bn) {
            assertEquals(xn.doubleValue(), bn.doubleValue(), 0.01, "Number at " + path);
        } else {
            assertEquals(xv, bv, "Value at " + path);
        }

        List<Data> xc = xml.getChildren();
        List<Data> bc = bin.getChildren();
        assertEquals(xc.size(), bc.size(), "Child count at " + path);

        int limit = Math.min(xc.size(), 50);
        for (int i = 0; i < limit; i++) {
            assertDataEquals(xc.get(i), bc.get(i), path + "/" + xc.get(i).getName());
        }
    }

    void assertWzEntryEquals(String xmlDir, String binKey, String path) {
        Data xmlData = xml(xmlDir).getData(path);
        assertNotNull(xmlData, "XML missing: " + xmlDir + "/" + path);

        if (!wzFiles.containsKey(binKey)) return;
        BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, binKey + ".wz").toString());
        Data binData = bf.getData(path);
        assertNotNull(binData, "Binary missing: " + binKey + "/" + path);
        assertDataEquals(xmlData, binData, binKey + "/" + path);
    }

    // =================================================================
    //  Property wrapper spot check
    // =================================================================

    @Nested @DisplayName("Property wrapper")
    class PropertyWrapper {
        @Test @DisplayName("wraps correctly")
        void wraps() {
            if (!wzFiles.containsKey("Mob")) return;
            WzImage img = wzFiles.get("Mob").getWzDirectory().getImageByName("0100100.img");
            assertNotNull(img); img.parseImage();

            BinaryWZMapleData d = new BinaryWZMapleData(img.getFromPath("info/maxHP"));
            assertEquals("maxHP", d.getName());
            assertEquals(DataType.INT, d.getType());
            assertEquals(8, d.getData());

            BinaryWZMapleData vd = new BinaryWZMapleData(img.getFromPath("stand/0/lt"));
            assertEquals(DataType.VECTOR, vd.getType());
            assertTrue(vd.getData() instanceof Point);
        }
    }

    // =================================================================
    //  Full enumeration — see FullEnumerationTest (standalone class)
    //  for the exhaustive 22K-image comparison
    // =================================================================

    @Nested @DisplayName("String.wz — all images")
    class StringWz {
        @Test void mobImg() { assertWzEntryEquals("String.wz", "String", "Mob.img"); }
        @Test void mapImg() { assertWzEntryEquals("String.wz", "String", "Map.img"); }
        @Test void npcImg() { assertWzEntryEquals("String.wz", "String", "Npc.img"); }
        @Test void skillImg() { assertWzEntryEquals("String.wz", "String", "Skill.img"); }
        @Test void cashImg() { assertWzEntryEquals("String.wz", "String", "Cash.img"); }
        @Test void consumeImg() { assertWzEntryEquals("String.wz", "String", "Consume.img"); }
        @Test void eqpImg() { assertWzEntryEquals("String.wz", "String", "Eqp.img"); }
        @Test void etcImg() { assertWzEntryEquals("String.wz", "String", "Etc.img"); }
        @Test void insImg() { assertWzEntryEquals("String.wz", "String", "Ins.img"); }
        @Test void petImg() { assertWzEntryEquals("String.wz", "String", "Pet.img"); }
    }

    @Nested @DisplayName("Mob.wz — sample entries")
    class MobWz {
        @Test void m0100100() { assertWzEntryEquals("Mob.wz", "Mob", "0100100.img"); }
        @Test void m0100101() { assertWzEntryEquals("Mob.wz", "Mob", "0100101.img"); }
        @Test void m0100120() { assertWzEntryEquals("Mob.wz", "Mob", "0100120.img"); }
        @Test void m2100100() { assertWzEntryEquals("Mob.wz", "Mob", "2100100.img"); }
    }

    @Nested @DisplayName("Skill.wz — job images")
    class SkillWz {
        @Test void beginner() { assertWzEntryEquals("Skill.wz", "Skill", "000.img"); }
        @Test void warrior()  { assertWzEntryEquals("Skill.wz", "Skill", "100.img"); }
        @Test void magician() { assertWzEntryEquals("Skill.wz", "Skill", "200.img"); }
        @Test void archer()   { assertWzEntryEquals("Skill.wz", "Skill", "300.img"); }
        @Test void rogue()    { assertWzEntryEquals("Skill.wz", "Skill", "400.img"); }
    }

    @Nested @DisplayName("Map.wz")
    class MapWz {
        @Test void effect()    { assertWzEntryEquals("Map.wz", "Map", "Effect.img"); }
        @Test void mapHelper() { assertWzEntryEquals("Map.wz", "Map", "MapHelper.img"); }
        @Test void physics()   { assertWzEntryEquals("Map.wz", "Map", "Physics.img"); }
        @Test void map0()      { assertWzEntryEquals("Map.wz", "Map", "Map/Map0/000000000.img"); }
        @Test void mapHenesys(){ assertWzEntryEquals("Map.wz", "Map", "Map/Map0/000010000.img"); }
    }

    @Nested @DisplayName("Item.wz — one per subdirectory")
    class ItemWz {
        @Test void cash()    { assertWzEntryEquals("Item.wz", "Item", "Cash/0501.img"); }
        @Test void consume() { assertWzEntryEquals("Item.wz", "Item", "Consume/0200.img"); }
        @Test void etc()     { assertWzEntryEquals("Item.wz", "Item", "Etc/0400.img"); }
        @Test void install() { assertWzEntryEquals("Item.wz", "Item", "Install/0301.img"); }
        @Test void pet()     { assertWzEntryEquals("Item.wz", "Item", "Pet/5000000.img"); }
        @Test void special() { assertWzEntryEquals("Item.wz", "Item", "Special/0900.img"); }
    }

    @Nested @DisplayName("Quest.wz")
    class QuestWz {
        @Test void questInfo() { assertWzEntryEquals("Quest.wz", "Quest", "QuestInfo.img"); }
        @Test void act()       { assertWzEntryEquals("Quest.wz", "Quest", "Act.img"); }
        @Test void check()     { assertWzEntryEquals("Quest.wz", "Quest", "Check.img"); }
    }

    @Nested @DisplayName("Reactor.wz")
    class ReactorWz {
        @Test void r2000() { assertWzEntryEquals("Reactor.wz", "Reactor", "0002000.img"); }
        @Test void r2001() { assertWzEntryEquals("Reactor.wz", "Reactor", "0002001.img"); }
    }

    @Nested @DisplayName("Etc.wz")
    class EtcWz {
        @Test void commodity()   { assertWzEntryEquals("Etc.wz", "Etc", "Commodity.img"); }
        @Test void cashPackage() { assertWzEntryEquals("Etc.wz", "Etc", "CashPackage.img"); }
    }

    @Nested @DisplayName("Character.wz — samples")
    class CharacterWz {
        @Test void c00002000() { assertWzEntryEquals("Character.wz", "Character", "00002000.img"); }
        @Test void c00012000() { assertWzEntryEquals("Character.wz", "Character", "00012000.img"); }
    }

    @Nested @DisplayName("Npc.wz — sample")
    class NpcWz {
        @Test void n2000() { assertWzEntryEquals("Npc.wz", "Npc", "0002000.img"); }
    }

    @Nested @DisplayName("UI.wz")
    class UiWz {
        @Test void basic()    { assertWzEntryEquals("UI.wz", "UI", "Basic.img"); }
        @Test void uiWindow() { assertWzEntryEquals("UI.wz", "UI", "UIWindow.img"); }
        @Test void buffIcon() { assertWzEntryEquals("UI.wz", "UI", "BuffIcon.img"); }
    }

    @Nested @DisplayName("Sound.wz")
    class SoundWz {
        @Test @DisplayName("opens")
        void opens() {
            assertNotNull(xml("Sound.wz").getRoot());
            if (!wzFiles.containsKey("Sound")) return;
            assertNotNull(new BinaryWZFile(Path.of(BIN, "Sound.wz").toString()).getRoot());
        }
    }

    // =================================================================
    //  UOL (User Object Link) resolution
    // =================================================================

    @Nested
    @DisplayName("UOL resolution")
    class UolResolution {

        @Test
        @DisplayName("OnUserEff.img: UOL sibling references match XML")
        void onUserEffSiblingUol() {
            Data xmlData = xml("Effect.wz").getData("OnUserEff.img");
            assertNotNull(xmlData);

            if (!wzFiles.containsKey("Effect")) return;
            BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, "Effect.wz").toString());
            Data binData = bf.getData("OnUserEff.img");
            assertNotNull(binData);

            // tutorialArrow1: frames 0-4 are canvases, 5-9 are UOLs → "0"
            String base = "guideEffect/aranTutorial/tutorialArrow1";

            // XML: frame 5 is UOL type with value "0"
            Data xml5 = xmlData.getChildByPath(base + "/5");
            assertNotNull(xml5);
            assertEquals(DataType.UOL, xml5.getType());
            assertEquals("0", xml5.getData());

            // Binary: same — UOL resolution from image root can't find
            // a sibling "0", so it stays as UOL (matching XML behavior)
            Data bin5 = binData.getChildByPath(base + "/5");
            assertNotNull(bin5);
            assertEquals("5", bin5.getName());
            assertEquals(DataType.UOL, bin5.getType());
            assertEquals("0", bin5.getData());
        }

        @Test
        @DisplayName("OnUserEff.img: child count and names match")
        void onUserEffChildrenMatch() {
            Data xmlData = xml("Effect.wz").getData("OnUserEff.img");
            assertNotNull(xmlData);

            if (!wzFiles.containsKey("Effect")) return;
            BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, "Effect.wz").toString());
            Data binData = bf.getData("OnUserEff.img");
            assertNotNull(binData);

            String base = "guideEffect/aranTutorial/tutorialArrow1";
            List<Data> xc = xmlData.getChildByPath(base).getChildren();
            List<Data> bc = binData.getChildByPath(base).getChildren();

            assertEquals(xc.size(), bc.size(), "child count");
            for (int i = 0; i < xc.size(); i++) {
                assertEquals(xc.get(i).getName(), bc.get(i).getName(),
                        "child[" + i + "] name");
                assertEquals(xc.get(i).getType(), bc.get(i).getType(),
                        "child[" + i + "] type");
            }
        }

        @Test
        @DisplayName("Direction1.img: UOL types match XML")
        void direction1ParentUol() {
            Data xmlData = xml("Effect.wz").getData("Direction1.img");
            assertNotNull(xmlData);

            if (!wzFiles.containsKey("Effect")) return;
            BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, "Effect.wz").toString());
            Data binData = bf.getData("Direction1.img");
            assertNotNull(binData);

            // Direction1.img has UOLs using "..".  Both modes must see
            // the same node types and names.
            assertEquals(xmlData.getChildren().size(),
                    binData.getChildren().size(), "child count");

            Data firstXml = xmlData.getChildren().get(0);
            Data firstBin = binData.getChildren().get(0);
            assertEquals(firstXml.getName(), firstBin.getName());
            assertEquals(firstXml.getChildren().size(),
                    firstBin.getChildren().size(),
                    "subdir child count");

            for (int i = 0; i < firstXml.getChildren().size(); i++) {
                assertEquals(firstXml.getChildren().get(i).getName(),
                        firstBin.getChildren().get(i).getName(),
                        "child[" + i + "] name");
                assertEquals(firstXml.getChildren().get(i).getType(),
                        firstBin.getChildren().get(i).getType(),
                        "child[" + i + "] type");
            }
        }

        @Test
        @DisplayName("SkillName1.img: parent-relative UOL matches XML")
        void skillNameParentUol() {
            Data xmlData = xml("Effect.wz").getData("SkillName1.img");
            assertNotNull(xmlData);

            if (!wzFiles.containsKey("Effect")) return;
            BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, "Effect.wz").toString());
            Data binData = bf.getData("SkillName1.img");
            assertNotNull(binData);

            // SkillName1.img/1100003/0 is a UOL pointing to ../1100002/0
            Data xmlUol = xmlData.getChildByPath("1100003/0");
            Data binUol = binData.getChildByPath("1100003/0");
            assertNotNull(xmlUol);
            assertNotNull(binUol);
            assertEquals(xmlUol.getType(), binUol.getType(),
                    "UOL type must match XML");
            assertEquals(xmlUol.getData(), binUol.getData(),
                    "UOL value must match XML");
        }

        @Test
        @DisplayName("Install/0399.img: UOL values match XML")
        void installItemUol() {
            Data xmlData = xml("Item.wz").getData("Install/0399.img");
            assertNotNull(xmlData);

            if (!wzFiles.containsKey("Item")) return;
            BinaryWZFile bf = new BinaryWZFile(Path.of(BIN, "Item.wz").toString());
            Data binData = bf.getData("Install/0399.img");
            assertNotNull(binData);

            assertEquals(xmlData.getChildren().size(),
                    binData.getChildren().size(), "child count");

            for (int i = 0; i < xmlData.getChildren().size(); i++) {
                Data xc = xmlData.getChildren().get(i);
                Data bc = binData.getChildren().get(i);
                assertEquals(xc.getName(), bc.getName(), "child[" + i + "] name");
                assertEquals(xc.getType(), bc.getType(), "child[" + i + "] type");
                if (xc.getType() == DataType.UOL) {
                    assertEquals(xc.getData(), bc.getData(),
                            "child[" + i + "] UOL value");
                }
            }
        }
    }

    // =================================================================
    //  getRoot() navigation
    // =================================================================

    @Nested @DisplayName("getRoot()")
    class RootNav {
        @Test void stringRoot() { assertRoot("String.wz", "String"); }
        @Test void mobRoot()    { assertRoot("Mob.wz", "Mob"); }
        @Test void itemRoot()   { assertRoot("Item.wz", "Item"); }
        @Test void mapRoot()    { assertRoot("Map.wz", "Map"); }
        @Test void skillRoot()  { assertRoot("Skill.wz", "Skill"); }
        @Test void questRoot()  { assertRoot("Quest.wz", "Quest"); }
        @Test void etcRoot()    { assertRoot("Etc.wz", "Etc"); }

        void assertRoot(String xmlDir, String binKey) {
            DataDirectoryEntry xr = xml(xmlDir).getRoot();
            if (!wzFiles.containsKey(binKey)) return;
            DataDirectoryEntry br = new BinaryWZFile(
                    Path.of(BIN, binKey + ".wz").toString()).getRoot();
            assertNotNull(br);
            assertEquals(xr.getSubdirectories().size(), br.getSubdirectories().size(),
                    binKey + " subdirs");
            assertEquals(xr.getFiles().size(), br.getFiles().size(),
                    binKey + " files");
        }
    }
}
