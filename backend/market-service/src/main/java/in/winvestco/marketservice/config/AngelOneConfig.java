package in.winvestco.marketservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "angelone")
@Data
public class AngelOneConfig {

    private String apiKey;
    private String clientCode;
    private String password;
    private String totpSecret;
    private String baseUrl = "https://apiconnect.angelbroking.com";
    private int tokenRefreshMinutes = 30;

    // Symbol token mappings for common indices
    private String nifty50Token = "99926000";
    private String bankNiftyToken = "99926009";
    private String sensexToken = "99919000";
    private String niftyItToken = "99926074";
}
