package config;

import com.esotericsoftware.yamlbeans.YamlReader;
import constants.string.CharsetConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class YamlConfig {
    private static String CONFIG_FILE_NAME = "config.yaml";
    public static YamlConfig config;

    public List<WorldConfig> worlds;
    public ServerConfig server;

    public static String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    public static void setConfigFileName(String fileName) {
        CONFIG_FILE_NAME = fileName;
    }

    public static void load() {
        config = loadConfig();
    }

    private static YamlConfig loadConfig() {
        try {
            YamlReader reader = new YamlReader(Files.newBufferedReader(Path.of(CONFIG_FILE_NAME), CharsetConstants.CHARSET));
            YamlConfig config = reader.read(YamlConfig.class);
            reader.close();
            return config;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not read config file " + CONFIG_FILE_NAME + ": " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Could not successfully parse config file " + CONFIG_FILE_NAME + ": " + e.getMessage());
        }
    }
}
