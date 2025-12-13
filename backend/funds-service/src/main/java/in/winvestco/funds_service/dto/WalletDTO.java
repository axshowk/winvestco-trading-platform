package in.winvestco.funds_service.dto;

import in.winvestco.common.enums.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for wallet balance information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private Long id;
    private Long userId;
    private BigDecimal availableBalance;
    private BigDecimal lockedBalance;
    private BigDecimal totalBalance;
    private String currency;
    private WalletStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
