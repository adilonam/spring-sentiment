package ma.code212.gateway.repository;

import ma.code212.gateway.model.CacheEntry;
import ma.code212.gateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CacheEntryRepository extends JpaRepository<CacheEntry, UUID> {
    
    Optional<CacheEntry> findByCacheKey(String cacheKey);
    
    List<CacheEntry> findByUser(User user);
    
    @Query("SELECT c FROM CacheEntry c WHERE c.expiresAt <= :now")
    List<CacheEntry> findExpiredEntries(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM CacheEntry c WHERE :tag MEMBER OF c.tags")
    List<CacheEntry> findByTag(@Param("tag") String tag);
    
    @Query("SELECT c FROM CacheEntry c WHERE c.user.id = :userId")
    List<CacheEntry> findByUserId(@Param("userId") UUID userId);
    
    void deleteByCacheKey(String cacheKey);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
