package ma.code212.gateway.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration class to load environment variables from .env file
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            
            // Convert dotenv entries to a Map
            Map<String, Object> dotenvProperties = dotenv.entries()
                    .stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey(),
                            entry -> entry.getValue()
                    ));

            // Add the .env properties to Spring's environment with high priority
            environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenv", dotenvProperties)
            );
            
            System.out.println("Successfully loaded .env file with " + dotenvProperties.size() + " properties");
        } catch (Exception e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }
    }
}
