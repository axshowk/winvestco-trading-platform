package in.winvestco.report_service.repository;

import in.winvestco.report_service.model.projection.WalletProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletProjectionRepository extends JpaRepository<WalletProjection, Long> {

    Optional<WalletProjection> findByUserId(Long userId);

    Optional<WalletProjection> findByWalletId(Long walletId);

    boolean existsByUserId(Long userId);
}
