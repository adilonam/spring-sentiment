package ma.code212.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.code212.gateway.enums.JobStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapingJobDto {
    private UUID id;
    private UserDto user;
    private JobStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String targetUrl;
    private Integer pagesScraped;
    private Integer commentsFound;
    private String errors;
    private Map<String, Object> configuration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
