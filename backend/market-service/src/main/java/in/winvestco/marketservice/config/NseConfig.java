package in.winvestco.marketservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Configuration for NSE India API.
 * This API is free and requires no authentication - just browser-like headers.
 */
@Configuration
@ConfigurationProperties(prefix = "nse")
@Data
public class NseConfig {

    private String baseUrl = "https://www.nseindia.com";
    private String apiBaseUrl = "https://www.nseindia.com/api";
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private int connectionTimeoutMs = 10000;
    private int readTimeoutMs = 10000;
    private int cookieRefreshIntervalMs = 300000; // 5 minutes
}
