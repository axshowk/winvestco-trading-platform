package in.winvestco.funds_service.dto;

import in.winvestco.common.enums.LockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for funds lock information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundsLockDTO {
    private Long id;
    private Long walletId;
    private String orderId;
    private BigDecimal amount;
    private LockStatus status;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;
}
