package ma.code212.gateway.service;

import ma.code212.gateway.config.KeycloakConfig;
import ma.code212.gateway.dto.AuthResponse;
import ma.code212.gateway.dto.LoginRequest;
import ma.code212.gateway.dto.RegisterRequest;
import ma.code212.gateway.dto.UserInfo;
import ma.code212.gateway.exception.AuthenticationException;
import ma.code212.gateway.exception.UserRegistrationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final KeycloakConfig keycloakConfig;
    private final WebClient keycloakWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    public AuthResponse authenticate(LoginRequest loginRequest) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("username", loginRequest.getUsername());
            formData.add("password", loginRequest.getPassword());
            formData.add("scope", "openid profile email");

            String response = keycloakWebClient
                    .post()
                    .uri(keycloakConfig.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);

            return AuthResponse.builder()
                    .accessToken(jsonNode.get("access_token").asText())
                    .tokenType(jsonNode.get("token_type").asText())
                    .expiresIn(jsonNode.get("expires_in").asLong())
                    .refreshToken(jsonNode.has("refresh_token") ? jsonNode.get("refresh_token").asText() : null)
                    .scope(jsonNode.has("scope") ? jsonNode.get("scope").asText() : null)
                    .build();

        } catch (WebClientResponseException e) {
            log.error("Authentication failed for user: {}, Status: {}, Response: {}", 
                     loginRequest.getUsername(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new AuthenticationException("Invalid username or password");
        } catch (Exception e) {
            log.error("Authentication error for user: {}", loginRequest.getUsername(), e);
            throw new AuthenticationException("Authentication failed");
        }
    }

    public UserInfo registerUser(RegisterRequest registerRequest) {
        try {
            // First, get admin token
            String adminToken = getAdminToken();

            // Create user payload
            Map<String, Object> userPayload = new HashMap<>();
            userPayload.put("username", registerRequest.getUsername());
            userPayload.put("email", registerRequest.getEmail());
            userPayload.put("firstName", registerRequest.getFirstName());
            userPayload.put("lastName", registerRequest.getLastName());
            userPayload.put("enabled", true);
            userPayload.put("emailVerified", false);

            // Set password
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", registerRequest.getPassword());
            credential.put("temporary", false);
            userPayload.put("credentials", new Object[]{credential});

            // Create user in Keycloak
            String response = keycloakWebClient
                    .post()
                    .uri(keycloakConfig.getUsersEndpoint())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userPayload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return UserInfo.builder()
                    .username(registerRequest.getUsername())
                    .email(registerRequest.getEmail())
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .emailVerified(false)
                    .enabled(true)
                    .build();

        } catch (WebClientResponseException e) {
            log.error("User registration failed for username: {}, Status: {}, Response: {}", 
                     registerRequest.getUsername(), e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new UserRegistrationException("Username or email already exists");
            }
            throw new UserRegistrationException("User registration failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("User registration error for username: {}", registerRequest.getUsername(), e);
            throw new UserRegistrationException("User registration failed");
        }
    }

    private String getAdminToken() {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);

            String response = keycloakWebClient
                    .post()
                    .uri(keycloakConfig.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("access_token").asText();

        } catch (Exception e) {
            log.error("Failed to get admin token", e);
            throw new RuntimeException("Failed to get admin token");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "refresh_token");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("refresh_token", refreshToken);

            String response = keycloakWebClient
                    .post()
                    .uri(keycloakConfig.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);

            return AuthResponse.builder()
                    .accessToken(jsonNode.get("access_token").asText())
                    .tokenType(jsonNode.get("token_type").asText())
                    .expiresIn(jsonNode.get("expires_in").asLong())
                    .refreshToken(jsonNode.has("refresh_token") ? jsonNode.get("refresh_token").asText() : refreshToken)
                    .scope(jsonNode.has("scope") ? jsonNode.get("scope").asText() : null)
                    .build();

        } catch (WebClientResponseException e) {
            log.error("Token refresh failed, Status: {}, Response: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            throw new AuthenticationException("Invalid refresh token");
        } catch (Exception e) {
            log.error("Token refresh error", e);
            throw new AuthenticationException("Token refresh failed");
        }
    }
}
