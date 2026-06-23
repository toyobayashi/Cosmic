package provider.wz;

import java.nio.file.Files;
import java.nio.file.Path;

public enum WZFiles {
    QUEST("Quest"),
    ETC("Etc"),
    ITEM("Item"),
    CHARACTER("Character"),
    STRING("String"),
    LIST("List"),
    MOB("Mob"),
    MAP("Map"),
    NPC("Npc"),
    REACTOR("Reactor"),
    SKILL("Skill"),
    SOUND("Sound"),
    UI("UI");

    private final String fileName;

    WZFiles(String name) {
        this.fileName = name + ".wz";
    }

    public Path getFile() {
        return Path.of(getWzDirectory(), fileName);
    }

    public String getFilePath() {
        return getFile().toString();
    }

    public String getWzFilePath() {
        return Path.of(getWzDirectory(), fileName).toString();
    }

    private static String getWzDirectory() {
        // Either provide a custom directory path through the "wz-path" property when launching the .jar, or don't provide one to use the default "wz" directory
        String propertyPath = System.getProperty("wz-path");
        if (propertyPath != null) {
            Path configuredPath = Path.of(propertyPath);
            if (!Files.isDirectory(configuredPath)) {
                throw new IllegalArgumentException(
                        "Configured WZ directory does not exist or is not a directory: " + configuredPath
                );
            }
            return configuredPath.toString();
        }

        return "wz";
    }

    /** @return the WZ root directory path (convenience, same as getWzDirectory). */
    public static String getDirectory() {
        return getWzDirectory();
    }
}
