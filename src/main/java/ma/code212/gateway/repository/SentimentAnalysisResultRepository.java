package ma.code212.gateway.repository;

import ma.code212.gateway.model.Comment;
import ma.code212.gateway.model.SentimentAnalysisResult;
import ma.code212.gateway.enums.Sentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SentimentAnalysisResultRepository extends JpaRepository<SentimentAnalysisResult, UUID> {
    
    List<SentimentAnalysisResult> findByComment(Comment comment);
    
    List<SentimentAnalysisResult> findByModelName(String modelName);
    
    List<SentimentAnalysisResult> findBySentiment(Sentiment sentiment);
    
    @Query("SELECT s FROM SentimentAnalysisResult s WHERE s.comment.id = :commentId")
    List<SentimentAnalysisResult> findByCommentId(@Param("commentId") UUID commentId);
    
    @Query("SELECT s FROM SentimentAnalysisResult s WHERE s.comment.id = :commentId AND s.modelName = :modelName")
    List<SentimentAnalysisResult> findByCommentIdAndModelName(@Param("commentId") UUID commentId, @Param("modelName") String modelName);
}
