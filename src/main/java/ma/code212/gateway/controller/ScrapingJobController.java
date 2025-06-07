package ma.code212.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.code212.gateway.dto.ScrapingJobDto;
import ma.code212.gateway.dto.UserDto;
import ma.code212.gateway.enums.JobStatus;
import ma.code212.gateway.model.ScrapingJob;
import ma.code212.gateway.model.User;
import ma.code212.gateway.service.ScrapingJobService;
import ma.code212.gateway.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/scraping-jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scraping Jobs", description = "Scraping job management and monitoring")
@SecurityRequirement(name = "Bearer Authentication")
public class ScrapingJobController {

    private final ScrapingJobService scrapingJobService;
    private final UserService userService;

    @GetMapping
    @Operation(
        summary = "Get User's Scraping Jobs", 
        description = "Retrieves all scraping jobs for the authenticated user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Scraping jobs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
        }
    )
    public ResponseEntity<Map<String, Object>> getUserScrapingJobs(
            @RequestParam(required = false) JobStatus status,
            Authentication authentication) {
        
        try {
            // Extract user from JWT token
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();
            
            Optional<User> userOpt = userService.findByKeycloakId(keycloakId);
            if (userOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "User not found in database");
                errorResponse.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            User user = userOpt.get();
            List<ScrapingJob> scrapingJobs;
            
            if (status != null) {
                scrapingJobs = scrapingJobService.findByUserIdAndStatus(user.getId(), status);
            } else {
                scrapingJobs = scrapingJobService.findByUserId(user.getId());
            }
            
            List<ScrapingJobDto> scrapingJobDtos = scrapingJobs.stream()
                    .map(job -> buildScrapingJobDto(job, user))
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Scraping jobs retrieved successfully");
            response.put("scrapingJobs", scrapingJobDtos);
            response.put("totalJobs", scrapingJobDtos.size());
            response.put("timestamp", LocalDateTime.now().toString());
            
            log.info("Retrieved {} scraping jobs for user: {}", scrapingJobDtos.size(), user.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving scraping jobs: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve scraping jobs: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{jobId}")
    @Operation(
        summary = "Get Scraping Job by ID", 
        description = "Retrieves a specific scraping job by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Scraping job retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Scraping job not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Job belongs to another user")
        }
    )
    public ResponseEntity<Map<String, Object>> getScrapingJobById(
            @PathVariable UUID jobId,
            Authentication authentication) {
        
        try {
            // Extract user from JWT token
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();
            
            Optional<User> userOpt = userService.findByKeycloakId(keycloakId);
            if (userOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "User not found in database");
                errorResponse.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            User user = userOpt.get();
            
            Optional<ScrapingJob> scrapingJobOpt = scrapingJobService.findById(jobId);
            if (scrapingJobOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Scraping job not found with ID: " + jobId);
                errorResponse.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            ScrapingJob scrapingJob = scrapingJobOpt.get();
            
            // Check if the job belongs to the authenticated user
            if (!scrapingJob.getUser().getId().equals(user.getId())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Access denied - Job belongs to another user");
                errorResponse.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            ScrapingJobDto scrapingJobDto = buildScrapingJobDto(scrapingJob, user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Scraping job retrieved successfully");
            response.put("scrapingJob", scrapingJobDto);
            response.put("timestamp", LocalDateTime.now().toString());
            
            log.info("Retrieved scraping job: {} for user: {}", jobId, user.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving scraping job {}: {}", jobId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve scraping job: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{jobId}/cancel")
    @Operation(
        summary = "Cancel Scraping Job", 
        description = "Cancels a running or pending scraping job",
        responses = {
            @ApiResponse(responseCode = "200", description = "Scraping job cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Scraping job not found"),
            @ApiResponse(responseCode = "400", description = "Job cannot be cancelled in current state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Job belongs to another user")
        }
    )
    public ResponseEntity<Map<String, Object>> cancelScrapingJob(
            @PathVariable UUID jobId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        try {
            // Extract user from JWT token
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();
            
            Optional<User> userOpt = userService.findByKeycloakId(keycloakId);
            if (userOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "User not found in database");
                errorResponse.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            User user = userOpt.get();
            
            Optional<ScrapingJob> scrapingJobOpt = scrapingJobService.findById(jobId);
            if (scrapingJobOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Scraping job not found with ID: " + jobId);
                errorResponse.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            ScrapingJob scrapingJob = scrapingJobOpt.get();
            
            // Check if the job belongs to the authenticated user
            if (!scrapingJob.getUser().getId().equals(user.getId())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Access denied - Job belongs to another user");
                errorResponse.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            // Check if job can be cancelled
            if (scrapingJob.getStatus() == JobStatus.COMPLETED || 
                scrapingJob.getStatus() == JobStatus.FAILED || 
                scrapingJob.getStatus() == JobStatus.CANCELLED) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Cannot cancel job in " + scrapingJob.getStatus() + " state");
                errorResponse.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.status(400).body(errorResponse);
            }
            
            ScrapingJob cancelledJob = scrapingJobService.cancelScrapingJob(
                jobId, 
                reason != null ? reason : "Cancelled by user"
            );
            
            ScrapingJobDto scrapingJobDto = buildScrapingJobDto(cancelledJob, user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Scraping job cancelled successfully");
            response.put("scrapingJob", scrapingJobDto);
            response.put("timestamp", LocalDateTime.now().toString());
            
            log.info("Cancelled scraping job: {} for user: {}", jobId, user.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error cancelling scraping job {}: {}", jobId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to cancel scraping job: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
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
}
