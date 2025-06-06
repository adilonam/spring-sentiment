package ma.code212.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.code212.gateway.enums.Sentiment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private UUID id;
    private String content;
    private String author;
    private String url;
    private LocalDateTime publishDate;
    private LocalDateTime scrapedAt;
    private Sentiment sentiment;
    private BigDecimal confidenceScore;
    private Boolean isProcessed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
