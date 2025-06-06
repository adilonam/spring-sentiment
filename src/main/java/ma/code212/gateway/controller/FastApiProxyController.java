package ma.code212.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fastapi")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FastAPI Services", description = "Comment scraping and sentiment analysis services")
public class FastApiProxyController {

    private final RestTemplate restTemplate;

    @Value("${external.fastapi.url}")
    private String fastApiUrl;

    // DTO classes for request/response
    public static class UrlInput {
        public String url;
        
        public UrlInput() {}
        
        public UrlInput(String url) {
            this.url = url;
        }
    }
    
    public static class CommentInput {
        public String comment;
        
        public CommentInput() {}
        
        public CommentInput(String comment) {
            this.comment = comment;
        }
    }
    
    public static class CommentResponse {
        public List<String> comments;
        public int total_comments;
    }
    
    public static class SentimentResult {
        public List<Map<String, Object>> results;
        public String sentiment;
        public double execution_time;
    }

    @PostMapping("/scrape-comments")
    @Operation(
        summary = "Scrape Comments", 
        description = "Scrapes comments from a given URL using Tor proxy",
        responses = {
            @ApiResponse(responseCode = "200", description = "Comments scraped successfully",
                content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<Object> scrapeComments(@RequestBody UrlInput urlInput) {
        return proxyToFastApi("/scrape-comments", urlInput, HttpMethod.POST);
    }

    @PostMapping("/comment-classification")
    @Operation(
        summary = "Comment Classification", 
        description = "Analyzes sentiment of a comment using AI model",
        responses = {
            @ApiResponse(responseCode = "200", description = "Sentiment analysis completed successfully",
                content = @Content(schema = @Schema(implementation = SentimentResult.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<Object> classifyComment(@RequestBody CommentInput commentInput) {
        return proxyToFastApi("/comment-classification", commentInput, HttpMethod.POST);
    }

    private ResponseEntity<Object> proxyToFastApi(String endpoint, Object body, HttpMethod httpMethod) {
        try {
            // Build the target URL
            String targetUrl = fastApiUrl + endpoint;
            
            log.info("Proxying {} to FastAPI: {}", httpMethod, targetUrl);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Accept", "application/json");
            
            // Create the request entity
            HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);
            
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
