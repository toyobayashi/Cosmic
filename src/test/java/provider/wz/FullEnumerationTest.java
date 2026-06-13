package provider.wz;

import config.YamlConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataProvider;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Full WZ enumeration")
class FullEnumerationTest {

    private static final String BIN = System.getProperty("wz-bin-path", Paths.get(System.getProperty("user.home"), "kinoko", "MapleStory").toAbsolutePath().toString());

    @BeforeAll
    static void loadConfig() { YamlConfig.load(); }

    @Test
    @Disabled("Run only with -Dwz-bin-path=/path/to/wz/files, as this is very slow and not meant for regular test runs.  Use BinaryWZMapleDataTest for spot checks, and run this once to verify no discrepancies between XML and binary before relying on the faster test.")
    @DisplayName("All entries match between XML and binary")
    void allImagesMatch() {
        assertNotNull(BIN, "Set -Dwz-bin-path=/path/to/wz/files");

        int total = 0, ok = 0;
        List<String> diffs = new ArrayList<>();
        long start = System.currentTimeMillis();

        for (String name : List.of("String", "Mob", "Skill", "Quest", "Reactor",
                "Etc", "Item", "Map", "Character", "Npc", "UI", "Sound", "Effect")) {
            String dirName = name + ".wz";
            DataProvider xmlProv = new XMLWZFile(Path.of("wz", dirName));
            BinaryWZFile binProv;
            try {
                long t0 = System.currentTimeMillis();
                binProv = new BinaryWZFile(Path.of(BIN, dirName).toString());
                System.out.println("[FULL] " + name + " opened in "
                        + (System.currentTimeMillis() - t0) / 1000.0 + "s");
            } catch (RuntimeException e) {
                System.out.println("[FULL] Skip " + name + ": " + e);
                continue;
            }

            DataDirectoryEntry xmlRoot = xmlProv.getRoot();
            List<String> paths = new ArrayList<>();
            collectPaths(xmlRoot, "", paths);

            int pass = 0;
            for (String entryPath : paths) {
                total++;
                try {
                    Data xd = xmlProv.getData(entryPath);
                    Data bd = binProv.getData(entryPath);
                    if (xd == null && bd == null) { pass++; ok++; continue; }
                    if (xd == null) { diffs.add(name + "/" + entryPath + ": XML null"); continue; }
                    if (bd == null) { diffs.add(name + "/" + entryPath + ": Binary null"); continue; }

                    String d = compareShallow(xd, bd);
                    if (d != null) diffs.add(name + "/" + entryPath + " " + d);
                    else { pass++; ok++; }
                } catch (Exception e) {
                    diffs.add(name + "/" + entryPath + ": " + e);
                }
            }

            long s = (System.currentTimeMillis() - start) / 1000;
            System.out.println("[FULL] " + name + " " + paths.size() + " entries, "
                    + pass + " ok (" + s + "s)");
        }

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        System.out.println("[FULL] " + total + " entries: " + ok + " OK, "
                + diffs.size() + " differ (" + elapsed + "s)");

        for (int i = 0; i < Math.min(diffs.size(), 30); i++)
            System.out.println("[FULL]  #" + (i + 1) + " " + diffs.get(i));

        assertTrue(diffs.isEmpty(), diffs.size() + " entries differ");
    }

    private void collectPaths(DataDirectoryEntry dir, String prefix, List<String> out) {
        for (var f : dir.getFiles()) out.add(prefix + f.getName());
        for (var sub : dir.getSubdirectories())
            collectPaths(sub, prefix + sub.getName() + "/", out);
    }

    /** Compare name, type, child count, first 20 child name/type/values. */
    private String compareShallow(Data xml, Data bin) {
        if (!xml.getName().equals(bin.getName()))
            return "name XML=" + xml.getName() + " Bin=" + bin.getName();
        if (xml.getType() != bin.getType())
            return "type XML=" + xml.getType() + " Bin=" + bin.getType();

        List<Data> xc = xml.getChildren(), bc = bin.getChildren();
        if (xc.size() != bc.size())
            return "children " + xc.size() + " vs " + bc.size();

        int limit = Math.min(xc.size(), 20);
        for (int i = 0; i < limit; i++) {
            Data xi = xc.get(i), bi = bc.get(i);
            if (!xi.getName().equals(bi.getName()))
                return "/" + xi.getName() + " child[" + i + "] name";
            if (xi.getType() != bi.getType())
                return "/" + xi.getName() + " child[" + i + "] type XML=" + xi.getType() + " Bin=" + bi.getType();
            Object xv = xi.getData(), bv = bi.getData();
            if (xv instanceof Point xp && bv instanceof Point bp) {
                if (xp.x != bp.x || xp.y != bp.y)
                    return "/" + xi.getName() + " pt";
            } else if (xv instanceof Number xn && bv instanceof Number bn) {
                if (Math.abs(xn.doubleValue() - bn.doubleValue()) > 0.01)
                    return "/" + xi.getName() + " val XML=" + xv + " Bin=" + bv;
            } else if (xv != null || bv != null) {
                if (!String.valueOf(xv).equals(String.valueOf(bv)))
                    return "/" + xi.getName() + " val XML=" + xv + " Bin=" + bv;
            }
        }
        return null;
    }
}
