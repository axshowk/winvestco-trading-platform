package in.winvestco.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreSQL Database Initializer - Auto-creates the database if it doesn't
 * exist.
 * 
 * <p>
 * This configuration runs before the main DataSource is initialized and ensures
 * that the required database exists. If not, it creates the database
 * automatically.
 * </p>
 * 
 * <p>
 * Enable this by setting: {@code spring.datasource.auto-create-database=true}
 * </p>
 * 
 * @author WinVestCo
 * @since 1.0.0
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "spring.datasource.auto-create-database", havingValue = "true", matchIfMissing = true)
public class PostgresDatabaseInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(PostgresDatabaseInitializer.class);

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile(
            "jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+)(?:\\?.*)?");

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:postgres}")
    private String username;

    @Value("${spring.datasource.password:postgres}")
    private String password;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Override
    public void afterPropertiesSet() {
        if (datasourceUrl == null || datasourceUrl.isBlank() || !datasourceUrl.contains("postgresql")) {
            log.debug("[{}] Skipping database auto-creation - not a PostgreSQL datasource", applicationName);
            return;
        }

        try {
            createDatabaseIfNotExists();
        } catch (Exception e) {
            log.warn("[{}] Could not auto-create database. Error: {}. " +
                    "Proceeding anyway - database may already exist or will be created by other means.",
                    applicationName, e.getMessage());
        }
    }

    private void createDatabaseIfNotExists() {
        Matcher matcher = JDBC_URL_PATTERN.matcher(datasourceUrl);

        if (!matcher.matches()) {
            log.warn("[{}] Could not parse PostgreSQL JDBC URL: {}", applicationName, datasourceUrl);
            return;
        }

        String host = matcher.group(1);
        String port = matcher.group(2) != null ? matcher.group(2) : "5432";
        String databaseName = matcher.group(3);

        // Connect to the 'postgres' default database to create our target database
        String adminUrl = String.format("jdbc:postgresql://%s:%s/postgres", host, port);

        log.info("[{}] Checking if database '{}' exists on {}:{}...",
                applicationName, databaseName, host, port);

        try (Connection connection = DriverManager.getConnection(adminUrl, username, password)) {
            if (!databaseExists(connection, databaseName)) {
                createDatabase(connection, databaseName);
                log.info("[{}] Successfully created database '{}'", applicationName, databaseName);
            } else {
                log.debug("[{}] Database '{}' already exists", applicationName, databaseName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database: " + e.getMessage(), e);
        }
    }

    private boolean databaseExists(Connection connection, String databaseName) throws Exception {
        String sql = "SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }

    private void createDatabase(Connection connection, String databaseName) throws Exception {
        // Validate database name to prevent SQL injection
        if (!databaseName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid database name: " + databaseName);
        }

        String sql = "CREATE DATABASE " + databaseName;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
}
