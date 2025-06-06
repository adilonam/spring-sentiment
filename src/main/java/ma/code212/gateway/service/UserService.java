package ma.code212.gateway.service;

import ma.code212.gateway.model.User;
import ma.code212.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by Keycloak ID
     */
    public Optional<User> findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Check if user exists by email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if user exists by username
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Save user
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Create a new user
     */
    @Transactional
    public User createUser(String email, String username, String firstName, String lastName, String keycloakId) {
        User user = User.builder()
                .email(email)
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .keycloakId(keycloakId)
                .isActive(true)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Created new user with ID: {} and Keycloak ID: {}", savedUser.getId(), keycloakId);
        return savedUser;
    }

    /**
     * Update user's last login time
     */
    @Transactional
    public void updateLastLogin(String keycloakId) {
        userRepository.findByKeycloakId(keycloakId)
                .ifPresent(user -> {
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                    log.debug("Updated last login for user with Keycloak ID: {}", keycloakId);
                });
    }

    /**
     * Find or create user by Keycloak ID
     */
    @Transactional
    public User findOrCreateByKeycloakId(String keycloakId, String email, String username, String firstName, String lastName) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    log.info("Creating new user for Keycloak ID: {}", keycloakId);
                    return createUser(email, username, firstName, lastName, keycloakId);
                });
    }

    /**
     * Activate/Deactivate user
     */
    @Transactional
    public void setUserActive(UUID userId, boolean isActive) {
        userRepository.findById(userId)
                .ifPresent(user -> {
                    user.setIsActive(isActive);
                    userRepository.save(user);
                    log.info("Set user {} active status to: {}", userId, isActive);
                });
    }

    /**
     * Update user profile
     */
    @Transactional
    public User updateProfile(UUID userId, String firstName, String lastName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setFirstName(firstName);
        user.setLastName(lastName);
        
        User updatedUser = userRepository.save(user);
        log.info("Updated profile for user: {}", userId);
        return updatedUser;
    }
}
