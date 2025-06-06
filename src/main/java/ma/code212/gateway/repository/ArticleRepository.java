package ma.code212.gateway.repository;

import ma.code212.gateway.model.Article;
import ma.code212.gateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
    
    Optional<Article> findByUrl(String url);
    
    
    List<Article> findByUser(User user);
    
    
    @Query("SELECT a FROM Article a WHERE a.user.id = :userId")
    List<Article> findByUserId(@Param("userId") UUID userId);
    
    boolean existsByUrl(String url);
}
