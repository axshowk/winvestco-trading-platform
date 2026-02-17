package in.winvestco.common.event;

import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a payment is successfully completed.
 * Funds-service listens to this to credit the wallet.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PaymentSuccessEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long userId;
    private Long walletId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentProvider provider;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String receipt;
    private String description;
    private Instant completedAt;
}
