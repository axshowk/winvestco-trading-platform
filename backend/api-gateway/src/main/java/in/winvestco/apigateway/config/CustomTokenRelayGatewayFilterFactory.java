package in.winvestco.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
public class CustomTokenRelayGatewayFilterFactory
        extends AbstractGatewayFilterFactory<CustomTokenRelayGatewayFilterFactory.Config> {

    private final ReactiveOAuth2AuthorizedClientManager clientManager;

    public CustomTokenRelayGatewayFilterFactory(ReactiveOAuth2AuthorizedClientManager clientManager) {
        super(Config.class);
        this.clientManager = clientManager;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> exchange.getPrincipal()
                .filter(principal -> principal instanceof OAuth2AuthenticationToken)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(authentication -> {
                    String clientRegistrationId = authentication.getAuthorizedClientRegistrationId();
                    OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                            .withClientRegistrationId(clientRegistrationId)
                            .principal(authentication)
                            .build();

                    return clientManager.authorize(request)
                            .map(OAuth2AuthorizedClient::getAccessToken)
                            .map(token -> withBearerAuth(exchange, token))
                            .defaultIfEmpty(exchange);
                })
                .flatMap(chain::filter);
    }

    private static org.springframework.web.server.ServerWebExchange withBearerAuth(
            org.springframework.web.server.ServerWebExchange exchange,
            OAuth2AccessToken accessToken) {
        return exchange.mutate()
                .request(r -> r.headers(headers -> headers.setBearerAuth(accessToken.getTokenValue())))
                .build();
    }

    public static class Config {
        // Configuration properties if needed
    }
}
