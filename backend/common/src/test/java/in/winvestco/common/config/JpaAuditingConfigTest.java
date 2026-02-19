package in.winvestco.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    JpaRepositoriesAutoConfiguration.class
            ));

    @Test
    void jpaAuditingConfig_WhenEnabled_ShouldCreateBean() {
        contextRunner
                .withBean(JpaAuditingConfig.class)
                .withPropertyValues("jpa.auditing.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(JpaAuditingConfig.class);
                });
    }

    @Test
    void jpaAuditingConfig_WhenDisabled_ShouldNotCreateBean() {
        contextRunner
                .withBean(JpaAuditingConfig.class)
                .withPropertyValues("jpa.auditing.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JpaAuditingConfig.class);
                });
    }
}
