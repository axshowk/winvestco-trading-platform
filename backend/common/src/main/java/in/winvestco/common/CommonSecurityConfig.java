package in.winvestco.common;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.winvestco.common.security")
public class CommonSecurityConfig {
    // This class enables component scanning for the security package in the common module
}
