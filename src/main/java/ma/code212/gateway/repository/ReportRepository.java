package ma.code212.gateway.repository;

import ma.code212.gateway.model.Report;
import ma.code212.gateway.model.User;
import ma.code212.gateway.enums.ReportStatus;
import ma.code212.gateway.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    
    List<Report> findByUser(User user);
    
    List<Report> findByType(ReportType type);
    
    List<Report> findByStatus(ReportStatus status);
    
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId")
    List<Report> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId AND r.type = :type")
    List<Report> findByUserIdAndType(@Param("userId") UUID userId, @Param("type") ReportType type);
    
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId AND r.status = :status")
    List<Report> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ReportStatus status);
}
