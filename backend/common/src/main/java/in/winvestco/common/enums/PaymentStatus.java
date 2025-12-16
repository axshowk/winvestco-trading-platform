package in.winvestco.common.enums;

/**
 * Payment lifecycle status for payment gateway integration.
 * 
 * Lifecycle: CREATED → INITIATED → PENDING → SUCCESS/FAILED/EXPIRED
 */
public enum PaymentStatus {
    /**
     * Payment record created, awaiting gateway order
     */
    CREATED,

    /**
     * Razorpay order created successfully
     */
    INITIATED,

    /**
     * User redirected to payment page, awaiting completion
     */
    PENDING,

    /**
     * Payment verified successfully via webhook
     */
    SUCCESS,

    /**
     * Payment failed via webhook
     */
    FAILED,

    /**
     * Payment TTL exceeded without completion
     */
    EXPIRED
}
