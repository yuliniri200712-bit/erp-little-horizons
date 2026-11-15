package com.littlehorizons.erp.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads application settings from {@code config/app.properties} and database credentials
 * from {@code config/db.properties}. Environment variables LH_DB_URL, LH_DB_USER and
 * LH_DB_PASSWORD take precedence, so credentials never need to live in the source code.
 */
public final class AppConfig {

    private static final String CONFIG_DIR_PROPERTY = "lh.config.dir";
    private static final String DEFAULT_CONFIG_DIR = "config";
    private static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3306/erp_little_horizons?useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=America/Bogota&characterEncoding=UTF-8";

    private static volatile AppConfig instance;

    private final Properties appProperties;
    private final Properties dbProperties;
    private final Path configDir;

    private AppConfig() {
        configDir = Path.of(System.getProperty(CONFIG_DIR_PROPERTY, DEFAULT_CONFIG_DIR));
        appProperties = load(configDir.resolve("app.properties"));
        dbProperties = load(configDir.resolve("db.properties"));
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    private static Properties load(Path file) {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                System.err.println("No se pudo leer " + file + ": " + e.getMessage());
            }
        }
        return properties;
    }

    private String fromEnvOr(String envName, Properties source, String key, String defaultValue) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return source.getProperty(key, defaultValue);
    }

    public String dbUrl() {
        return fromEnvOr("LH_DB_URL", dbProperties, "db.url", DEFAULT_DB_URL);
    }

    public String dbUser() {
        return fromEnvOr("LH_DB_USER", dbProperties, "db.user", "");
    }

    public String dbPassword() {
        return fromEnvOr("LH_DB_PASSWORD", dbProperties, "db.password", "");
    }

    public boolean hasDbCredentials() {
        return !dbUser().isBlank();
    }

    public Path dbConfigFile() {
        return configDir.resolve("db.properties");
    }

    public String get(String key, String defaultValue) {
        return appProperties.getProperty(key, defaultValue);
    }

    public String companyName() {
        return get("company.name", "Colegio Internacional Little Horizons S.A.S.");
    }

    public String companyNit() {
        return get("company.nit", "000.000.000-0");
    }

    public String companyAddress() {
        return get("company.address", "");
    }

    public String companyPhone() {
        return get("company.phone", "");
    }

    public String companyEmail() {
        return get("company.email", "");
    }

    public String invoiceResolution() {
        return get("invoice.resolution", "Resolución de habilitación SIMULADA");
    }

    public Path invoiceOutputDir() {
        return Path.of(get("invoice.output.dir", "facturas"));
    }

    public Path logFile() {
        return Path.of(get("log.file", "error_log.txt"));
    }
}
