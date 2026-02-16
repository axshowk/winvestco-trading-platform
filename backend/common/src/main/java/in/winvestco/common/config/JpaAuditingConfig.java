package in.winvestco.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Common JPA Auditing configuration.
 * Enables automatic population of @CreatedDate and @LastModifiedDate fields.
 * 
 * <p>
 * This configuration is enabled by default. To disable it, set:
 * {@code jpa.auditing.enabled=false} in application.yml
 * 
 * <p>
 * Services using this configuration should:
 * <ul>
 * <li>Remove @EnableJpaAuditing from their main application class</li>
 * <li>Delete any local JpaAuditingConfig class</li>
 * <li>Ensure they have component scanning for in.winvestco.common package</li>
 * </ul>
 */
@Configuration("commonJpaAuditingConfig")
@EnableJpaAuditing
@ConditionalOnProperty(name = "jpa.auditing.enabled", havingValue = "true", matchIfMissing = true)
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(javax.sql.DataSource.class)
public class JpaAuditingConfig {
}
