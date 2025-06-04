package ma.code212.gateway.repository;

import ma.code212.gateway.model.ScrapingJob;
import ma.code212.gateway.model.User;
import ma.code212.gateway.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScrapingJobRepository extends JpaRepository<ScrapingJob, UUID> {
    
    List<ScrapingJob> findByUser(User user);
    
    List<ScrapingJob> findByStatus(JobStatus status);
    
    @Query("SELECT s FROM ScrapingJob s WHERE s.user.id = :userId")
    List<ScrapingJob> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT s FROM ScrapingJob s WHERE s.user.id = :userId AND s.status = :status")
    List<ScrapingJob> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") JobStatus status);
    
    long countByStatus(JobStatus status);
}
