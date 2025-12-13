package in.winvestco.order_service.repository;

import in.winvestco.common.enums.OrderStatus;
import in.winvestco.order_service.model.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByUserIdAndStatusIn(Long userId, List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status NOT IN :terminalStatuses ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByUserId(
            @Param("userId") Long userId,
            @Param("terminalStatuses") List<OrderStatus> terminalStatuses);

    @Query("SELECT o FROM Order o WHERE o.status IN :statuses AND o.expiresAt <= :now")
    List<Order> findExpiredOrders(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("now") Instant now);

    List<Order> findBySymbolAndStatusIn(String symbol, List<OrderStatus> statuses);

    long countByUserIdAndStatus(Long userId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.symbol = :symbol AND o.status NOT IN :terminalStatuses")
    List<Order> findActiveOrdersByUserIdAndSymbol(
            @Param("userId") Long userId,
            @Param("symbol") String symbol,
            @Param("terminalStatuses") List<OrderStatus> terminalStatuses);
}
