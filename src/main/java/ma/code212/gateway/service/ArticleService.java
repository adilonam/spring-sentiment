package ma.code212.gateway.service;

import ma.code212.gateway.model.Article;
import ma.code212.gateway.model.User;
import ma.code212.gateway.repository.ArticleRepository;
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
public class ArticleService {

    private final ArticleRepository articleRepository;

    /**
     * Find or create an article by URL and user
     */
    @Transactional
    public Article findOrCreateArticle(String url, String title, User user) {
        log.info("Finding or creating article for URL: {}", url);
        
        Optional<Article> existingArticle = articleRepository.findByUrl(url);
        
        if (existingArticle.isPresent()) {
            log.info("Article already exists for URL: {}", url);
            return existingArticle.get();
        }
        
        // Create new article
        Article newArticle = Article.builder()
                .url(url)
                .title(title)
                .user(user)
                .totalComments(0)
                .scrapedAt(LocalDateTime.now())
                .build();
        
        Article savedArticle = articleRepository.save(newArticle);
        log.info("Created new article with ID: {}", savedArticle.getId());
        
        return savedArticle;
    }

    /**
     * Update total comments count for an article
     */
    @Transactional
    public void updateTotalComments(UUID articleId, int totalComments) {
        log.info("Updating total comments for article ID: {} to {}", articleId, totalComments);
        
        Optional<Article> articleOpt = articleRepository.findById(articleId);
        if (articleOpt.isPresent()) {
            Article article = articleOpt.get();
            article.setTotalComments(totalComments);
            article.setScrapedAt(LocalDateTime.now());
            articleRepository.save(article);
        }
    }

    /**
     * Find article by URL
     */
    public Optional<Article> findByUrl(String url) {
        return articleRepository.findByUrl(url);
    }

    /**
     * Find articles by user
     */
    public List<Article> findByUser(User user) {
        return articleRepository.findByUser(user);
    }

    /**
     * Find article by ID
     */
    public Optional<Article> findById(UUID id) {
        return articleRepository.findById(id);
    }

    /**
     * Check if article exists by URL
     */
    public boolean existsByUrl(String url) {
        return articleRepository.existsByUrl(url);
    }
}
