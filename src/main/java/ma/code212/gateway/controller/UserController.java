package ma.code212.gateway.controller;

import ma.code212.gateway.model.User;
import ma.code212.gateway.service.UserService;
import ma.code212.gateway.dto.UserInfo;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile/{keycloakId}")
    @Operation(summary = "Get user profile by Keycloak ID", description = "Retrieve user profile information by Keycloak ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String keycloakId) {
        log.info("Getting user profile for Keycloak ID: {}", keycloakId);
        
        Optional<User> userOpt = userService.findByKeycloakId(keycloakId);
        
        if (userOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "User not found");
            response.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        UserInfo userInfo = UserInfo.builder()
                .id(user.getKeycloakId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getIsActive())
                .build();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("user", userInfo);
        response.put("lastLogin", user.getLastLogin());
        response.put("createdAt", user.getCreatedAt());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/email/{email}")
    @Operation(summary = "Get user profile by email", description = "Retrieve user profile information by email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserProfileByEmail(@PathVariable String email) {
        log.info("Getting user profile for email: {}", email);
        
        Optional<User> userOpt = userService.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "User not found");
            response.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        UserInfo userInfo = UserInfo.builder()
                .id(user.getKeycloakId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getIsActive())
                .build();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("user", userInfo);
        response.put("lastLogin", user.getLastLogin());
        response.put("createdAt", user.getCreatedAt());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "Update user active status", description = "Activate or deactivate a user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam boolean isActive) {
        
        log.info("Updating user status for ID: {} to active: {}", userId, isActive);
        
        userService.setUserActive(userId, isActive);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User status updated successfully");
        response.put("userId", userId);
        response.put("isActive", isActive);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{keycloakId}/sync")
    @Operation(summary = "Sync user from Keycloak", description = "Create or update user in database from Keycloak data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User synced successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncUser(
            @PathVariable String keycloakId,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        
        log.info("Syncing user with Keycloak ID: {}", keycloakId);

        User user = userService.findOrCreateByKeycloakId(keycloakId, email, username, firstName, lastName);

        UserInfo userInfo = UserInfo.builder()
                .id(user.getKeycloakId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getIsActive())
                .build();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User synced successfully");
        response.put("user", userInfo);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
