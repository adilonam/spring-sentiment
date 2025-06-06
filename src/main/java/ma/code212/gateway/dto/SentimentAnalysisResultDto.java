package ma.code212.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Sentiment analysis result data")
public class SentimentAnalysisResultDto {
    
    @Schema(description = "Unique identifier of the sentiment analysis result")
    private UUID id;
    
    @Schema(description = "Name of the model used for analysis")
    private String modelName;
    
    @Schema(description = "Detected sentiment")
    private Sentiment sentiment;
    
    @Schema(description = "Confidence score of the analysis")
    private BigDecimal confidenceScore;
    
    @Schema(description = "Positive sentiment score")
    private BigDecimal positiveScore;
    
    @Schema(description = "Negative sentiment score")
    private BigDecimal negativeScore;
    
    @Schema(description = "Neutral sentiment score")
    private BigDecimal neutralScore;
    
    @Schema(description = "Timestamp when the analysis was processed")
    private LocalDateTime processedAt;
    
    @Schema(description = "Timestamp when the result was created")
    private LocalDateTime createdAt;
}
