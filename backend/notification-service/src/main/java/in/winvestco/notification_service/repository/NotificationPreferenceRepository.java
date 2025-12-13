package in.winvestco.notification_service.repository;

import in.winvestco.notification_service.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for NotificationPreference entity.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * Find preference by user ID.
     */
    Optional<NotificationPreference> findByUserId(Long userId);

    /**
     * Check if preference exists for user.
     */
    boolean existsByUserId(Long userId);
}
