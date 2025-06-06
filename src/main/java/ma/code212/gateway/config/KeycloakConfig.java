package ma.code212.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Bean
    public WebClient keycloakWebClient() {
        return WebClient.builder()
                .baseUrl(keycloakServerUrl)
                .build();
    }

    public String getKeycloakServerUrl() {
        return keycloakServerUrl;
    }

    public String getRealm() {
        return realm;
    }

    public String getTokenEndpoint() {
        return String.format("%s/realms/%s/protocol/openid-connect/token", keycloakServerUrl, realm);
    }

    public String getUsersEndpoint() {
        return String.format("%s/admin/realms/%s/users", keycloakServerUrl, realm);
    }
}
