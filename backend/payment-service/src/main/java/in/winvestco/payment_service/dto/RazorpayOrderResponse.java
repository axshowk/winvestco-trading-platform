package in.winvestco.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response containing Razorpay order details for frontend integration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {
    /**
     * Internal payment ID
     */
    private Long paymentId;

    /**
     * Razorpay order ID (order_xxxxx)
     */
    private String orderId;

    /**
     * Amount in smallest currency unit (paise for INR)
     */
    private Long amountInPaise;

    /**
     * Amount in rupees for display
     */
    private BigDecimal amount;

    /**
     * Currency code
     */
    private String currency;

    /**
     * Receipt/reference number
     */
    private String receipt;

    /**
     * Razorpay key ID for frontend initialization
     */
    private String razorpayKeyId;

    /**
     * Description to show on payment page
     */
    private String description;

    /**
     * User's name for prefilling
     */
    private String prefillName;

    /**
     * User's email for prefilling
     */
    private String prefillEmail;

    /**
     * User's phone for prefilling
     */
    private String prefillContact;
}
