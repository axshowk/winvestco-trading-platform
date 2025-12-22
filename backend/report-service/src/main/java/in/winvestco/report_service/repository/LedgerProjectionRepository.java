package in.winvestco.report_service.repository;

import in.winvestco.report_service.model.projection.LedgerProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LedgerProjectionRepository extends JpaRepository<LedgerProjection, Long> {

    Page<LedgerProjection> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT l FROM LedgerProjection l WHERE l.userId = :userId " +
           "AND l.createdAt >= :fromDate AND l.createdAt <= :toDate " +
           "ORDER BY l.createdAt DESC")
    List<LedgerProjection> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);

    @Query("SELECT l FROM LedgerProjection l WHERE l.userId = :userId " +
           "AND l.entryType = :entryType " +
           "AND l.createdAt >= :fromDate AND l.createdAt <= :toDate " +
           "ORDER BY l.createdAt DESC")
    List<LedgerProjection> findByUserIdAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("entryType") String entryType,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);
}
