package ma.code212.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/protected")
@Slf4j
@Tag(name = "Protected Resources", description = "Endpoints requiring authentication")
@SecurityRequirement(name = "Bearer Authentication")
public class ProtectedController {

    @GetMapping("/user-info")
    @Operation(summary = "Get current user information", description = "Get information about the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getUserInfo(Authentication authentication) {
        log.info("Getting user info for authenticated user");
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", jwt.getClaimAsString("preferred_username"));
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("name", jwt.getClaimAsString("name"));
        userInfo.put("firstName", jwt.getClaimAsString("given_name"));
        userInfo.put("lastName", jwt.getClaimAsString("family_name"));
        userInfo.put("roles", jwt.getClaimAsStringList("realm_access"));
        userInfo.put("subject", jwt.getSubject());
        userInfo.put("issuer", jwt.getIssuer().toString());
        userInfo.put("issuedAt", jwt.getIssuedAt());
        userInfo.put("expiresAt", jwt.getExpiresAt());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User information retrieved successfully");
        response.put("user", userInfo);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    @Operation(summary = "Test authentication", description = "Simple test endpoint to verify JWT authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication test successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    public ResponseEntity<Map<String, Object>> testAuth(Authentication authentication) {
        log.info("Authentication test for user: {}", authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Authentication successful! You are authorized to access this resource.");
        response.put("authenticatedUser", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin only endpoint", description = "Test endpoint that requires ADMIN role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Admin access successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    })
    public ResponseEntity<Map<String, Object>> adminOnly(Authentication authentication) {
        log.info("Admin endpoint accessed by user: {}", authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Welcome Admin! You have administrative privileges.");
        response.put("adminUser", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "User role endpoint", description = "Test endpoint that requires USER or ADMIN role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User access successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - USER or ADMIN role required")
    })
    public ResponseEntity<Map<String, Object>> userOnly(Authentication authentication) {
        log.info("User endpoint accessed by user: {}", authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Welcome User! You have user privileges.");
        response.put("user", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Protected service health check", description = "Health check for protected endpoints")
    @ApiResponse(responseCode = "200", description = "Protected service is healthy")
    public ResponseEntity<Map<String, Object>> health(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "Protected Resources Service");
        response.put("authenticatedUser", authentication.getName());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
