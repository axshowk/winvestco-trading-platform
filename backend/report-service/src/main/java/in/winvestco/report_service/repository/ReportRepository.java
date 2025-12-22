package in.winvestco.report_service.repository;

import in.winvestco.common.enums.ReportStatus;
import in.winvestco.report_service.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportId(String reportId);

    Page<Report> findByUserIdOrderByRequestedAtDesc(Long userId, Pageable pageable);

    List<Report> findByUserIdAndStatus(Long userId, ReportStatus status);

    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.requestedAt < :cutoff")
    List<Report> findPendingReportsOlderThan(
            @Param("status") ReportStatus status, 
            @Param("cutoff") Instant cutoff);

    @Query("SELECT r FROM Report r WHERE r.status = 'COMPLETED' AND r.completedAt < :expiryDate")
    List<Report> findExpiredReports(@Param("expiryDate") Instant expiryDate);

    long countByUserIdAndStatus(Long userId, ReportStatus status);
}
