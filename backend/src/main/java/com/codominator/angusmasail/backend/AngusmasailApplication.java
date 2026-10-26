package com.codominator.angusmasail.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableScheduling
public class AngusmasailApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(AngusmasailApplication.class, args);
    }

    public static void loadDotenv() {
        File[] candidates = new File[] {
                new File(".env"),
                new File("../.env"),
                new File("../../.env"),
                new File("backend/.env")
        };
        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                System.out.println("🌱 Loading environment configuration from: " + file.getAbsolutePath());
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                if (val.length() >= 2) {
                                    val = val.substring(1, val.length() - 1).trim();
                                }
                            }
                            if ("SPRING_MAIL_PASSWORD".equalsIgnoreCase(key)) {
                                val = val.replace(" ", "");
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not load .env file: " + e.getMessage());
                }
                break;
            }
        }
    }
}
