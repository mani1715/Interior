package com.interior.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class PlatformApiApplication {

    private static final Logger log = LoggerFactory.getLogger(PlatformApiApplication.class);

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(PlatformApiApplication.class, args);
    }

    private static void loadDotenv() {
        List<String> candidates = List.of(
                ".env.local",
                "../../.env.local",
                "../.env.local",
                ".env",
                "../../.env",
                "../.env"
        );

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.exists() && f.isFile()) {
                log.info("Loading environment configurations from {}", f.getAbsolutePath());
                try (BufferedReader reader = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String val = line.substring(eqIdx + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not read dotenv file {}: {}", candidate, e.getMessage());
                }
            }
        }

        // Auto-configure Stability AI provider if key is provided but provider is not explicitly set
        String stabilityKey = System.getProperty("STABILITY_API_KEY");
        if (stabilityKey == null) {
            stabilityKey = System.getenv("STABILITY_API_KEY");
        }
        if (stabilityKey == null) {
            stabilityKey = System.getProperty("INTERIOR_AI_API_KEY");
        }
        if (stabilityKey == null) {
            stabilityKey = System.getenv("INTERIOR_AI_API_KEY");
        }

        if (stabilityKey != null && !stabilityKey.isBlank() && !stabilityKey.equalsIgnoreCase("unconfigured")) {
            String currentProvider = System.getProperty("INTERIOR_AI_PROVIDER");
            if (currentProvider == null) {
                currentProvider = System.getenv("INTERIOR_AI_PROVIDER");
            }
            if (currentProvider == null || currentProvider.isBlank() || "none".equalsIgnoreCase(currentProvider)) {
                System.setProperty("interior.ai.provider", "stability");
                System.setProperty("INTERIOR_AI_PROVIDER", "stability");
                log.info("Auto-configured Stability AI provider from detected API key credential");
            }
        }
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
