package ma.code212.gateway.repository;

import ma.code212.gateway.model.Article;
import ma.code212.gateway.model.Comment;
import ma.code212.gateway.enums.Sentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    
    List<Comment> findByArticle(Article article);
    
    List<Comment> findBySentiment(Sentiment sentiment);
    
    List<Comment> findByIsProcessed(Boolean isProcessed);
    
    @Query("SELECT c FROM Comment c WHERE c.article.id = :articleId")
    List<Comment> findByArticleId(@Param("articleId") UUID articleId);
    
    @Query("SELECT c FROM Comment c WHERE c.sentiment = :sentiment AND c.article.id = :articleId")
    List<Comment> findByArticleIdAndSentiment(@Param("articleId") UUID articleId, @Param("sentiment") Sentiment sentiment);
    
    long countByArticle(Article article);
    
    long countBySentiment(Sentiment sentiment);
}
