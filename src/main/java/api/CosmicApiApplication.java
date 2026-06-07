package api;

import config.YamlConfig;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@ComponentScan(basePackages = "api")
public class CosmicApiApplication {

    private static final Logger log = LoggerFactory.getLogger(CosmicApiApplication.class);

    public static void main(String[] args) {
        if (args.length > 0) {
            YamlConfig.setConfigFileName(args[0]);
        }
        YamlConfig.load();

        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");

        SpringApplication app = new SpringApplication(CosmicApiApplication.class);
        int apiPort = YamlConfig.config.server.API_PORT;
        if (apiPort <= 0) apiPort = 8686;
        app.setDefaultProperties(java.util.Map.of("server.port", String.valueOf(apiPort)));
        app.run(args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void startGameServer() {
        log.info("Starting game server...");
        try {
            Server.getInstance().init();
            log.info("Game server started successfully");
        } catch (Exception e) {
            log.error("Failed to start game server", e);
        }
    }
}
