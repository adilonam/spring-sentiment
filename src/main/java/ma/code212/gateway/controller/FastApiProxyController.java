package ma.code212.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.code212.gateway.dto.*;
import ma.code212.gateway.enums.Sentiment;
import ma.code212.gateway.model.Article;
import ma.code212.gateway.model.Comment;
import ma.code212.gateway.model.ScrapingJob;
import ma.code212.gateway.model.SentimentAnalysisResult;
import ma.code212.gateway.model.User;
import ma.code212.gateway.service.ArticleService;
import ma.code212.gateway.service.CommentService;
import ma.code212.gateway.service.ScrapingJobService;
import ma.code212.gateway.service.SentimentAnalysisResultService;
import ma.code212.gateway.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fastapi")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FastAPI Services", description = "Comment scraping and sentiment analysis services")
@SecurityRequirement(name = "Bearer Authentication")
public class FastApiProxyController {

    private final RestTemplate restTemplate;
    private final ArticleService articleService;
    private final CommentService commentService;
    private final SentimentAnalysisResultService sentimentAnalysisResultService;
    private final UserService userService;
    private final ScrapingJobService scrapingJobService;
    private final ObjectMapper objectMapper;

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
        description = "Scrapes comments from a given URL using Tor proxy and creates Article with associated Comments",
        responses = {
            @ApiResponse(responseCode = "200", description = "Comments scraped successfully",
                content = @Content(schema = @Schema(implementation = ScrapeCommentsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<ScrapeCommentsResponse> scrapeComments(
            @Valid @RequestBody ScrapeCommentsRequest request,
            Authentication authentication) {
        
        try {
            log.info("Scraping comments for URL: {} with title: {}", request.getUrl(), request.getTitle());
            
            // Extract user from JWT token
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();
            
            Optional<User> userOpt = userService.findByKeycloakId(keycloakId);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("User not found in database");
            }
            User user = userOpt.get();
            
            // Create scraping job configuration
            Map<String, Object> jobConfiguration = Map.of(
                "title", request.getTitle(),
                "userAgent", "Spring-Gateway-Bot",
                "maxPages", 1,
                "timeout", 30000
            );
            
            // Create scraping job
            ScrapingJob scrapingJob = scrapingJobService.createScrapingJob(user, request.getUrl(), jobConfiguration);
            
            // Start the scraping job
            scrapingJob = scrapingJobService.startScrapingJob(scrapingJob.getId());
            
            // Find or create article
            Article article = articleService.findOrCreateArticle(request.getUrl(), request.getTitle(), user);
            
            try {
                // Call FastAPI to scrape comments
                UrlInput urlInput = new UrlInput(request.getUrl());
                ResponseEntity<String> fastApiResponse = proxyToFastApiForScraping("/scrape-comments", urlInput, HttpMethod.POST);
                
                // Parse FastAPI response
                JsonNode responseJson = objectMapper.readTree(fastApiResponse.getBody());
                JsonNode commentsArray = responseJson.get("comments");
                int totalComments = responseJson.get("total_comments").asInt();
                
                // Create comment entities
                List<String> commentTexts = objectMapper.convertValue(commentsArray, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                
                List<Comment> savedComments = commentService.createComments(commentTexts, article);
                
                // Update article total comments
                articleService.updateTotalComments(article.getId(), totalComments);
                
                // Complete the scraping job successfully
                scrapingJob = scrapingJobService.completeScrapingJob(
                    scrapingJob.getId(), 
                    1, // pages scraped
                    savedComments.size() // comments found
                );
                
                // Build response DTOs
                ArticleDto articleDto = buildArticleDto(article, user);
                List<CommentDto> commentDtos = savedComments.stream()
                        .map(this::buildCommentDto)
                        .toList();
                ScrapingJobDto scrapingJobDto = buildScrapingJobDto(scrapingJob, user);
                
                ScrapeCommentsResponse response = ScrapeCommentsResponse.builder()
                        .status("success")
                        .message("Comments scraped and saved successfully")
                        .article(articleDto)
                        .comments(commentDtos)
                        .totalComments(totalComments)
                        .scrapingJob(scrapingJobDto)
                        .timestamp(LocalDateTime.now().toString())
                        .build();
                
                log.info("Successfully scraped and saved {} comments for article ID: {}, job ID: {}", 
                    savedComments.size(), article.getId(), scrapingJob.getId());
                
                return ResponseEntity.ok(response);
                
            } catch (Exception scrapingException) {
                // Fail the scraping job on error
                scrapingJob = scrapingJobService.failScrapingJob(scrapingJob.getId(), scrapingException.getMessage());
                throw scrapingException;
            }
            
        } catch (Exception e) {
            log.error("Error scraping comments: {}", e.getMessage(), e);
            
            ScrapeCommentsResponse errorResponse = ScrapeCommentsResponse.builder()
                    .status("error")
                    .message("Failed to scrape comments: " + e.getMessage())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/comment-classification")
    @Operation(
        summary = "Comment Classification", 
        description = "Analyzes sentiment of a comment using AI model by comment ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Sentiment analysis completed successfully",
                content = @Content(schema = @Schema(implementation = CommentClassificationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<CommentClassificationResponse> classifyComment(
            @Valid @RequestBody CommentClassificationRequest request) {
        
        try {
            log.info("Classifying comment with ID: {}", request.getCommentId());
            
            // Find the comment by ID
            Optional<Comment> commentOpt = commentService.findById(request.getCommentId());
            if (commentOpt.isEmpty()) {
                CommentClassificationResponse errorResponse = CommentClassificationResponse.builder()
                        .status("error")
                        .message("Comment not found with ID: " + request.getCommentId())
                        .timestamp(LocalDateTime.now().toString())
                        .build();
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            Comment comment = commentOpt.get();
            
            // Prepare FastAPI request
            CommentInput commentInput = new CommentInput(comment.getContent());
            
            // Call FastAPI for sentiment analysis
            ResponseEntity<Object> fastApiResponse = proxyToFastApi("/comment-classification", commentInput, HttpMethod.POST);
            
            // Parse FastAPI response
            Object responseBody = fastApiResponse.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from FastAPI");
            }
            JsonNode responseJson = objectMapper.readTree(responseBody.toString());
            
            // Extract sentiment analysis data from FastAPI response
            String sentimentStr = responseJson.get("sentiment").asText();
            
            // Parse scores from results array
            JsonNode resultsArray = responseJson.get("results");
            double confidenceScore = 0.0;
            double positiveScore = 0.0;
            double negativeScore = 0.0;
            double neutralScore = 0.0;
            
            // Extract scores from results array
            if (resultsArray != null && resultsArray.isArray()) {
                for (JsonNode result : resultsArray) {
                    String label = result.get("label").asText().toLowerCase();
                    double score = result.get("score").asDouble();
                    
                    switch (label) {
                        case "positive":
                            positiveScore = score;
                            break;
                        case "negative":
                            negativeScore = score;
                            break;
                        case "neutral":
                            neutralScore = score;
                            break;
                    }
                    
                    // Use the score of the predicted sentiment as confidence score
                    if (label.equalsIgnoreCase(sentimentStr)) {
                        confidenceScore = score;
                    }
                }
            }
            
            // Convert string sentiment to enum
            Sentiment sentiment = Sentiment.valueOf(sentimentStr.toUpperCase());
            
            // Create SentimentAnalysisResult
            String modelName = request.getModelName() != null ? request.getModelName() : "default_model";
            SentimentAnalysisResult result = sentimentAnalysisResultService.createSentimentAnalysisResult(
                    comment,
                    modelName,
                    sentiment,
                    BigDecimal.valueOf(confidenceScore),
                    BigDecimal.valueOf(positiveScore),
                    BigDecimal.valueOf(negativeScore),
                    BigDecimal.valueOf(neutralScore)
            );
            
            // Update comment with sentiment data
            commentService.updateCommentSentiment(comment.getId(), sentiment, confidenceScore);
            
            // Build response DTOs
            CommentDto commentDto = buildCommentDto(comment);
            SentimentAnalysisResultDto resultDto = buildSentimentAnalysisResultDto(result);
            
            CommentClassificationResponse response = CommentClassificationResponse.builder()
                    .status("success")
                    .message("Comment sentiment analysis completed successfully")
                    .comment(commentDto)
                    .sentimentAnalysisResult(resultDto)
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            
            log.info("Successfully analyzed sentiment for comment ID: {} - Result: {}", 
                    comment.getId(), sentiment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error analyzing comment sentiment: {}", e.getMessage(), e);
            
            CommentClassificationResponse errorResponse = CommentClassificationResponse.builder()
                    .status("error")
                    .message("Failed to analyze comment sentiment: " + e.getMessage())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private ResponseEntity<String> proxyToFastApiForScraping(String endpoint, Object body, HttpMethod httpMethod) {
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
            
            return response;
            
        } catch (Exception e) {
            log.error("Error proxying request to FastAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to proxy request to FastAPI: " + e.getMessage(), e);
        }
    }

    private ArticleDto buildArticleDto(Article article, User user) {
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
        
        return ArticleDto.builder()
                .id(article.getId())
                .title(article.getTitle())
                .url(article.getUrl())
                .totalComments(article.getTotalComments())
                .scrapedAt(article.getScrapedAt())
                .user(userDto)
                .build();
    }

    private CommentDto buildCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(comment.getAuthor())
                .url(comment.getUrl())
                .publishDate(comment.getPublishDate())
                .scrapedAt(comment.getScrapedAt())
                .sentiment(comment.getSentiment())
                .confidenceScore(comment.getConfidenceScore())
                .isProcessed(comment.getIsProcessed())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private SentimentAnalysisResultDto buildSentimentAnalysisResultDto(SentimentAnalysisResult result) {
        return SentimentAnalysisResultDto.builder()
                .id(result.getId())
                .modelName(result.getModelName())
                .sentiment(result.getSentiment())
                .confidenceScore(result.getConfidenceScore())
                .positiveScore(result.getPositiveScore())
                .negativeScore(result.getNegativeScore())
                .neutralScore(result.getNeutralScore())
                .processedAt(result.getProcessedAt())
                .createdAt(result.getCreatedAt())
                .build();
    }

    private ScrapingJobDto buildScrapingJobDto(ScrapingJob scrapingJob, User user) {
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
        
        return ScrapingJobDto.builder()
                .id(scrapingJob.getId())
                .user(userDto)
                .status(scrapingJob.getStatus())
                .startTime(scrapingJob.getStartTime())
                .endTime(scrapingJob.getEndTime())
                .targetUrl(scrapingJob.getTargetUrl())
                .pagesScraped(scrapingJob.getPagesScraped())
                .commentsFound(scrapingJob.getCommentsFound())
                .errors(scrapingJob.getErrors())
                .configuration(scrapingJob.getConfiguration())
                .createdAt(scrapingJob.getCreatedAt())
                .updatedAt(scrapingJob.getUpdatedAt())
                .build();
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
