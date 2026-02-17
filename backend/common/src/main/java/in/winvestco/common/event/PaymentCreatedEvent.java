package in.winvestco.common.event;

import in.winvestco.common.enums.PaymentProvider;
import in.winvestco.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a payment is created/initiated
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PaymentCreatedEvent extends BaseEvent {
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
