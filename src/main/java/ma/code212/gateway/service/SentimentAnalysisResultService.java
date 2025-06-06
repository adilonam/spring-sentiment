package ma.code212.gateway.service;

import ma.code212.gateway.model.Comment;
import ma.code212.gateway.model.SentimentAnalysisResult;
import ma.code212.gateway.repository.SentimentAnalysisResultRepository;
import ma.code212.gateway.enums.Sentiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisResultService {

    private final SentimentAnalysisResultRepository sentimentAnalysisResultRepository;

    /**
     * Create a new sentiment analysis result
     */
    @Transactional
    public SentimentAnalysisResult createSentimentAnalysisResult(
            Comment comment,
            String modelName,
            Sentiment sentiment,
            BigDecimal confidenceScore,
            BigDecimal positiveScore,
            BigDecimal negativeScore,
            BigDecimal neutralScore) {
        
        log.info("Creating sentiment analysis result for comment ID: {} with model: {}", 
                comment.getId(), modelName);
        
        SentimentAnalysisResult result = SentimentAnalysisResult.builder()
                .comment(comment)
                .modelName(modelName)
                .sentiment(sentiment)
                .confidenceScore(confidenceScore)
                .positiveScore(positiveScore)
                .negativeScore(negativeScore)
                .neutralScore(neutralScore)
                .processedAt(LocalDateTime.now())
                .build();
        
        SentimentAnalysisResult savedResult = sentimentAnalysisResultRepository.save(result);
        log.info("Created sentiment analysis result with ID: {}", savedResult.getId());
        
        return savedResult;
    }

    /**
     * Find sentiment analysis results by comment
     */
    public List<SentimentAnalysisResult> findByComment(Comment comment) {
        return sentimentAnalysisResultRepository.findByComment(comment);
    }

    /**
     * Find sentiment analysis results by comment ID
     */
    public List<SentimentAnalysisResult> findByCommentId(UUID commentId) {
        return sentimentAnalysisResultRepository.findByCommentId(commentId);
    }

    /**
     * Find sentiment analysis results by model name
     */
    public List<SentimentAnalysisResult> findByModelName(String modelName) {
        return sentimentAnalysisResultRepository.findByModelName(modelName);
    }

    /**
     * Find sentiment analysis results by comment ID and model name
     */
    public List<SentimentAnalysisResult> findByCommentIdAndModelName(UUID commentId, String modelName) {
        return sentimentAnalysisResultRepository.findByCommentIdAndModelName(commentId, modelName);
    }

    /**
     * Find sentiment analysis result by ID
     */
    public Optional<SentimentAnalysisResult> findById(UUID id) {
        return sentimentAnalysisResultRepository.findById(id);
    }

    /**
     * Save sentiment analysis result
     */
    @Transactional
    public SentimentAnalysisResult save(SentimentAnalysisResult result) {
        return sentimentAnalysisResultRepository.save(result);
    }
}
