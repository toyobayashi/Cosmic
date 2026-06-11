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

    public static final String DIRECTORY = getWzDirectory();

    private final String fileName;

    WZFiles(String name) {
        this.fileName = name + ".wz";
    }

    public Path getFile() {
        return Path.of(DIRECTORY, fileName);
    }

    public String getFilePath() {
        return getFile().toString();
    }

    /**
     * Returns the absolute path to the {@code .wz} binary file for use in
     * binary WZ mode.  Falls back to XML mode's directory path if no
     * individual file is found (e.g. when using extracted XMLs).
     */
    public String getWzFilePath() {
        return Path.of(DIRECTORY, fileName).toString();
    }

    private static String getWzDirectory() {
        // Either provide a custom directory path through the "wz-path" property when launching the .jar, or don't provide one to use the default "wz" directory
        String propertyPath = System.getProperty("wz-path");
        if (propertyPath != null && Files.isDirectory(Path.of(propertyPath))) {
            return propertyPath;
        }

        return "wz";
    }
}
