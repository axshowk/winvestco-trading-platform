package in.winvestco.payment_service.dto;

import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.enums.PaymentProvider;
import in.winvestco.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long userId;
    private Long walletId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private PaymentProvider provider;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String receipt;
    private String description;
    private String failureReason;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
