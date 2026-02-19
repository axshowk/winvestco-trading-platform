package in.winvestco.common.config;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlywayConfigTest {

    @Test
    void flywayMigration_WhenEnabled_ShouldReturnFlyway() {
        FlywayConfig config = new FlywayConfig();
        DataSource dataSource = mock(DataSource.class);

        var flyway = config.flywayMigration(dataSource);

        assertNotNull(flyway);
    }

    @Test
    void flywayMigration_WhenDisabled_ShouldReturnNull() {
        FlywayConfig config = new FlywayConfig() {
            {
                try {
                    var field = FlywayConfig.class.getDeclaredField("flywayEnabled");
                    field.setAccessible(true);
                    field.set(this, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        DataSource dataSource = mock(DataSource.class);

        var flyway = config.flywayMigration(dataSource);

        assertNull(flyway);
    }

    @Test
    void flywayMigration_ShouldUseDataSource() {
        FlywayConfig config = new FlywayConfig();
        DataSource dataSource = mock(DataSource.class);

        var flyway = config.flywayMigration(dataSource);

        assertNotNull(flyway);
    }
}
