package ma.code212.gateway.service;

import ma.code212.gateway.config.KeycloakConfig;
import ma.code212.gateway.dto.AuthResponse;
import ma.code212.gateway.dto.LoginRequest;
import ma.code212.gateway.dto.RegisterRequest;
import ma.code212.gateway.dto.UserInfo;
import ma.code212.gateway.exception.AuthenticationException;
import ma.code212.gateway.exception.UserRegistrationException;
import ma.code212.gateway.model.User;

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
import java.util.Base64;
import java.util.Optional;

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
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    public AuthResponse authenticate(LoginRequest loginRequest) {
        try {
            // First, check if user exists in local database
            // Try to find by username first, then by email as fallback
            Optional<User> localUser = userService.findByUsername(loginRequest.getUsername());
            
            if (localUser.isEmpty()) {
                // Fallback: try to find by email in case username is actually an email
                localUser = userService.findByEmail(loginRequest.getUsername());
            }
            
            if (localUser.isEmpty()) {
                // User doesn't exist in local database
                log.warn("Authentication attempt for non-existent user in local database: {}", loginRequest.getUsername());
                throw new AuthenticationException("User not found in system. Please contact administrator.");
            }
            
            // Check if user is active
            if (!localUser.get().getIsActive()) {
                log.warn("Authentication attempt for inactive user: {}", loginRequest.getUsername());
                throw new AuthenticationException("User account is inactive");
            }
            
            log.info("User found in local database, proceeding with Keycloak authentication: {}", loginRequest.getUsername());

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

            String accessToken = jsonNode.get("access_token").asText();
            
            // Extract user info from token and sync with database
            extractAndSyncUserFromToken(accessToken);

            return AuthResponse.builder()
                    .accessToken(accessToken)
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

            log.debug("Creating user in Keycloak with payload: {}", userPayload);
            
            // Create user in Keycloak and get Location header
            String locationHeader = keycloakWebClient
                    .post()
                    .uri(keycloakConfig.getUsersEndpoint())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .block()
                    .getHeaders()
                    .getLocation()
                    .toString();

            log.debug("User created successfully, location: {}", locationHeader);
            // Extract Keycloak user ID from Location header (e.g., .../users/{userId})
            String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
            
            log.info("Keycloak user created with ID: {}", keycloakUserId);

            // Create User entity in our database
            userService.createUser(
                    registerRequest.getEmail(),
                    registerRequest.getUsername(),
                    registerRequest.getFirstName(),
                    registerRequest.getLastName(),
                    keycloakUserId
            );

            return UserInfo.builder()
                    .id(keycloakUserId)
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
            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("403 Forbidden - Client '{}' doesn't have admin permissions. " +
                         "Please check Keycloak client configuration and service account roles.", clientId);
                throw new UserRegistrationException("Registration failed: Insufficient permissions. " +
                         "Please contact administrator.");
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new UserRegistrationException("Registration failed: Authentication error. " +
                         "Please check client credentials.");
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
            String token = jsonNode.get("access_token").asText();
            
            log.debug("Successfully obtained admin token for client: {}", clientId);
            return token;

        } catch (WebClientResponseException e) {
            log.error("Failed to get admin token - Status: {}, Response: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get admin token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get admin token", e);
            throw new RuntimeException("Failed to get admin token: " + e.getMessage());
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

            String accessToken = jsonNode.get("access_token").asText();
            
            // Extract user info from token and sync with database
            extractAndSyncUserFromToken(accessToken);

            return AuthResponse.builder()
                    .accessToken(accessToken)
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

    /**
     * Find or create a user in the database by Keycloak ID
     */
    public User findOrCreateUserByKeycloakId(String keycloakId, String email, String username, String firstName, String lastName) {
        return userService.findOrCreateByKeycloakId(keycloakId, email, username, firstName, lastName);
    }

    /**
     * Update user's last login time
     */
    public void updateLastLogin(String keycloakId) {
        userService.updateLastLogin(keycloakId);
    }

    /**
     * Extract user information from JWT access token
     */
    private void extractAndSyncUserFromToken(String accessToken) {
        try {
            // JWT tokens have 3 parts separated by dots: header.payload.signature
            String[] parts = accessToken.split("\\.");
            if (parts.length >= 2) {
                // Decode the payload (second part)
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                JsonNode payloadNode = objectMapper.readTree(payload);
                
                String keycloakId = payloadNode.get("sub").asText();
                String email = payloadNode.has("email") ? payloadNode.get("email").asText() : null;
                String username = payloadNode.has("preferred_username") ? payloadNode.get("preferred_username").asText() : email;
                String firstName = payloadNode.has("given_name") ? payloadNode.get("given_name").asText() : null;
                String lastName = payloadNode.has("family_name") ? payloadNode.get("family_name").asText() : null;
                
                // Find or create user in our database
                if (keycloakId != null) {
                    findOrCreateUserByKeycloakId(keycloakId, email, username, firstName, lastName);
                    updateLastLogin(keycloakId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract user info from JWT token", e);
        }
    }
}
