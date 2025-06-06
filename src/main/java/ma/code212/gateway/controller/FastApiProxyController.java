package ma.code212.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/fastapi")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FastAPI Proxy", description = "Proxy endpoints that forward requests to FastAPI service")
public class FastApiProxyController {

    private final RestTemplate restTemplate;

    @Value("${external.fastapi.url}")
    private String fastApiUrl;

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    @Operation(summary = "FastAPI Proxy", description = "Forwards all requests to FastAPI service")
    public ResponseEntity<Object> proxyToFastApi(
            HttpServletRequest request,
            @RequestBody(required = false) Object body) {
        
        try {
            // Extract the path after /api/v1/fastapi/
            String requestPath = request.getRequestURI().replaceFirst("/api/v1/fastapi", "");
            if (requestPath.isEmpty()) {
                requestPath = "/";
            }
            
            // Build the target URL
            String targetUrl = fastApiUrl + requestPath;
            
            // Add query parameters if they exist
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                targetUrl += "?" + queryString;
            }
            
            log.info("Proxying {} {} to FastAPI: {}", request.getMethod(), request.getRequestURI(), targetUrl);
            
            // Copy headers from the original request
            HttpHeaders headers = new HttpHeaders();
            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                // Skip certain headers that shouldn't be forwarded
                if (!headerName.equalsIgnoreCase("host") && 
                    !headerName.equalsIgnoreCase("content-length") &&
                    !headerName.equalsIgnoreCase("connection") &&
                    !headerName.equalsIgnoreCase("transfer-encoding") &&
                    !headerName.equalsIgnoreCase("accept-encoding")) {
                    headers.add(headerName, request.getHeader(headerName));
                }
            });
            
            // Create the request entity
            HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);
            
            // Determine the HTTP method
            HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
            
            // Forward the request to FastAPI
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl,
                httpMethod,
                requestEntity,
                String.class
            );
            
            log.info("FastAPI responded with status: {}", response.getStatusCode());
            
            // Return the response with the original headers and body as String
            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());
            
        } catch (Exception e) {
            log.error("Error proxying request to FastAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to proxy request to FastAPI: " + e.getMessage(), e);
        }
    }
}
