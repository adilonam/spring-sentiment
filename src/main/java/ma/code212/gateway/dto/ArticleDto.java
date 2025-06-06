package ma.code212.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDto {
    private UUID id;
    private String title;
    private String url;  
    private Integer totalComments;
    private LocalDateTime scrapedAt;
    private UserDto user;
}
