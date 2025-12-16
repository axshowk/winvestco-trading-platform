package in.winvestco.common.event;

import in.winvestco.common.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a payment fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long userId;
    private BigDecimal amount;
    private PaymentProvider provider;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String failureReason;
    private String errorCode;
    private Instant failedAt;
}
