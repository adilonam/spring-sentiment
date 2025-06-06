package ma.code212.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing comment data with sentiment analysis result")
public class CommentClassificationResponse {
    
    @Schema(description = "Status of the request")
    private String status;
    
    @Schema(description = "Response message")
    private String message;
    
    @Schema(description = "The comment that was analyzed")
    private CommentDto comment;
    
    @Schema(description = "The sentiment analysis result")
    private SentimentAnalysisResultDto sentimentAnalysisResult;
    
    @Schema(description = "Timestamp of the response")
    private String timestamp;
}
