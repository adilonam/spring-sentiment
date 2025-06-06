package ma.code212.gateway.service;

import ma.code212.gateway.dto.CommentResponse;
import ma.code212.gateway.dto.UrlInputRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FastApiService {

    private final RestTemplate restTemplate;

    @Value("${external.fastapi.url}")
    private String fastApiUrl;

    public CommentResponse extractComments(UrlInputRequest urlInputRequest) {
        try {
            String endpoint = fastApiUrl + "/extract-comments";
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create the request entity
            HttpEntity<UrlInputRequest> requestEntity = new HttpEntity<>(urlInputRequest, headers);
            
            log.info("Sending request to FastAPI: {} with URL: {}", endpoint, urlInputRequest.getUrl());
            
            // Make the POST request
            ResponseEntity<CommentResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                requestEntity,
                CommentResponse.class
            );
            
            log.info("Received response from FastAPI with {} comments", 
                response.getBody() != null ? response.getBody().getTotalComments() : 0);
            
            return response.getBody();
            
        } catch (ResourceAccessException e) {
            log.error("FastAPI service is not reachable: {}", e.getMessage());
            throw new RuntimeException("FastAPI service is not available. Please try again later.", e);
        } catch (HttpClientErrorException e) {
            log.error("Client error when calling FastAPI: {} - {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Invalid request to FastAPI service: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            log.error("Server error when calling FastAPI: {} - {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("FastAPI service error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error when calling FastAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract comments: " + e.getMessage(), e);
        }
    }
}
