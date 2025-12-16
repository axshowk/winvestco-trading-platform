package in.winvestco.common.event;

import in.winvestco.common.enums.PaymentProvider;
import in.winvestco.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a payment is created/initiated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long userId;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentProvider provider;
    private String razorpayOrderId;
    private String receipt;
    private String description;
    private Instant createdAt;
}
