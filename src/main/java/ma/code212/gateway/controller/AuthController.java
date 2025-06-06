package ma.code212.gateway.controller;

import ma.code212.gateway.dto.AuthResponse;
import ma.code212.gateway.dto.LoginRequest;
import ma.code212.gateway.dto.RegisterRequest;
import ma.code212.gateway.dto.UserInfo;
import ma.code212.gateway.service.KeycloakService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private final KeycloakService keycloakService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a new user in Keycloak")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or user already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registering new user: {}", registerRequest.getUsername());
        
        UserInfo userInfo = keycloakService.registerUser(registerRequest);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User registered successfully");
        response.put("user", userInfo);
        response.put("timestamp", LocalDateTime.now().toString());
        
        log.info("User registered successfully: {}", registerRequest.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticate user and return JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Authentication attempt for user: {}", loginRequest.getUsername());
        
        AuthResponse authResponse = keycloakService.authenticate(loginRequest);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Authentication successful");
        response.put("auth", authResponse);
        response.put("timestamp", LocalDateTime.now().toString());
        
        log.info("User authenticated successfully: {}", loginRequest.getUsername());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Refresh JWT token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestParam String refreshToken) {
        log.info("Token refresh attempt");
        
        AuthResponse authResponse = keycloakService.refreshToken(refreshToken);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Token refreshed successfully");
        response.put("auth", authResponse);
        response.put("timestamp", LocalDateTime.now().toString());
        
        log.info("Token refreshed successfully");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Check authentication service health", description = "Health check endpoint for authentication service")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "Authentication Service");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
