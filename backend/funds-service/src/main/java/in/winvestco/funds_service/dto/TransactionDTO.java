package in.winvestco.funds_service.dto;

import in.winvestco.common.enums.TransactionStatus;
import in.winvestco.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for transaction information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long id;
    private Long walletId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private TransactionStatus status;
    private String externalReference;
    private String description;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
