package ma.code212.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for comment sentiment classification")
public class CommentClassificationRequest {
    
    @NotNull(message = "Comment ID is required")
    @Schema(description = "The ID of the comment to analyze", required = true)
    private UUID commentId;
    
    @Schema(description = "The name of the model to use for analysis", example = "default_model")
    private String modelName;
}
