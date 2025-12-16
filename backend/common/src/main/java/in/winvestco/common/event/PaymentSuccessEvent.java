package in.winvestco.common.event;

import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a payment is successfully completed.
 * Funds-service listens to this to credit the wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent implements Serializable {
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
