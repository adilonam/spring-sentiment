package ma.code212.gateway.repository;

import ma.code212.gateway.model.SystemLog;
import ma.code212.gateway.model.User;
import ma.code212.gateway.enums.LogLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, UUID> {
    
    List<SystemLog> findByLevel(LogLevel level);
    
    List<SystemLog> findByUser(User user);
    
    List<SystemLog> findByService(String service);
    
    @Query("SELECT s FROM SystemLog s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    List<SystemLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM SystemLog s WHERE s.level = :level AND s.createdAt >= :since")
    List<SystemLog> findByLevelAndCreatedAtAfter(@Param("level") LogLevel level, @Param("since") LocalDateTime since);
    
    @Query("SELECT s FROM SystemLog s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<SystemLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
