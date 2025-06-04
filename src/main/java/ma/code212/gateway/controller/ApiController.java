package ma.code212.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Gateway API", description = "Simple API endpoints for the gateway application")
public class ApiController {

    @GetMapping("/hello")
    @Operation(summary = "Get a simple hello message", description = "Returns a simple hello world message with timestamp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation")
    })
    public ResponseEntity<Map<String, Object>> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello World from Gateway API!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hello/{name}")
    @Operation(summary = "Get a personalized hello message", description = "Returns a personalized hello message with the provided name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation")
    })
    public ResponseEntity<Map<String, Object>> helloWithName(@PathVariable String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello " + name + " from Gateway API!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "success");
        response.put("name", name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Get application status", description = "Returns the current status of the gateway application")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application is running")
    })
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Gateway");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
}
