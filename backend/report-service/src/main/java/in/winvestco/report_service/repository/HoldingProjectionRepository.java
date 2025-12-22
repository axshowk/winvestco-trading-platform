package in.winvestco.report_service.repository;

import in.winvestco.report_service.model.projection.HoldingProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingProjectionRepository extends JpaRepository<HoldingProjection, Long> {

    List<HoldingProjection> findByUserId(Long userId);

    Optional<HoldingProjection> findByUserIdAndSymbol(Long userId, String symbol);

    boolean existsByUserIdAndSymbol(Long userId, String symbol);
}
