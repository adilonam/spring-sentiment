package ma.code212.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeCommentsResponse {
    private String status;
    private String message;
    private ArticleDto article;
    private List<CommentDto> comments;
    private int totalComments;
    private ScrapingJobDto scrapingJob;
    private String timestamp;
}
