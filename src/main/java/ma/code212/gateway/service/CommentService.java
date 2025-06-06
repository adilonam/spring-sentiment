package ma.code212.gateway.service;

import ma.code212.gateway.model.Article;
import ma.code212.gateway.model.Comment;
import ma.code212.gateway.repository.CommentRepository;
import ma.code212.gateway.enums.Sentiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;

    /**
     * Create a new comment
     */
    @Transactional
    public Comment createComment(String content, String author, String url, Article article) {
        log.info("Creating new comment for article ID: {}", article.getId());
        
        Comment comment = Comment.builder()
                .content(content)
                .author(author)
                .url(url)
                .article(article)
                .scrapedAt(LocalDateTime.now())
                .isProcessed(false)
                .build();
        
        Comment savedComment = commentRepository.save(comment);
        log.info("Created comment with ID: {}", savedComment.getId());
        
        return savedComment;
    }

    /**
     * Create multiple comments for an article
     */
    @Transactional
    public List<Comment> createComments(List<String> comments, Article article) {
        log.info("Creating {} comments for article ID: {}", comments.size(), article.getId());
        
        List<Comment> commentEntities = comments.stream()
                .map(commentText -> Comment.builder()
                        .content(commentText)
                        .article(article)
                        .scrapedAt(LocalDateTime.now())
                        .isProcessed(false)
                        .build())
                .toList();
        
        List<Comment> savedComments = commentRepository.saveAll(commentEntities);
        log.info("Successfully created {} comments", savedComments.size());
        
        return savedComments;
    }

    /**
     * Find comment by ID
     */
    public Optional<Comment> findById(UUID id) {
        return commentRepository.findById(id);
    }

    /**
     * Find comments by article
     */
    public List<Comment> findByArticle(Article article) {
        return commentRepository.findByArticle(article);
    }

    /**
     * Find comments by article ID
     */
    public List<Comment> findByArticleId(UUID articleId) {
        return commentRepository.findByArticleId(articleId);
    }

    /**
     * Find comments by sentiment
     */
    public List<Comment> findBySentiment(Sentiment sentiment) {
        return commentRepository.findBySentiment(sentiment);
    }

    /**
     * Find unprocessed comments
     */
    public List<Comment> findUnprocessedComments() {
        return commentRepository.findByIsProcessed(false);
    }

    /**
     * Update comment sentiment
     */
    @Transactional
    public void updateCommentSentiment(UUID commentId, Sentiment sentiment, Double confidenceScore) {
        log.info("Updating sentiment for comment ID: {} to {}", commentId, sentiment);
        
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            comment.setSentiment(sentiment);
            if (confidenceScore != null) {
                comment.setConfidenceScore(java.math.BigDecimal.valueOf(confidenceScore));
            }
            comment.setIsProcessed(true);
            commentRepository.save(comment);
        }
    }

    /**
     * Count comments by article
     */
    public long countByArticle(Article article) {
        return commentRepository.countByArticle(article);
    }

    /**
     * Count comments by sentiment
     */
    public long countBySentiment(Sentiment sentiment) {
        return commentRepository.countBySentiment(sentiment);
    }
}
