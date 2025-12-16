package in.winvestco.payment_service.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import in.winvestco.payment_service.exception.PaymentVerificationException;
import in.winvestco.payment_service.exception.RazorpayGatewayException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for Razorpay SDK integration
 */
@Service
@Slf4j
public class RazorpayService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        try {
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
            log.info("Razorpay client initialized successfully");
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay client", e);
            throw new RazorpayGatewayException("Failed to initialize Razorpay client", e);
        }
    }

    /**
     * Get Razorpay key ID for frontend
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Create a Razorpay order
     *
     * @param amount   Amount in rupees
     * @param currency Currency code (INR)
     * @param receipt  Receipt/reference number
     * @return Razorpay order ID
     */
    public String createOrder(BigDecimal amount, String currency, String receipt) {
        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in smallest currency unit (paise for INR)
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1); // Auto-capture

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            log.info("Created Razorpay order: {} for amount: {} {}", orderId, amount, currency);
            return orderId;

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw new RazorpayGatewayException("Failed to create payment order: " + e.getMessage(), e);
        }
    }

    /**
     * Verify payment signature from Razorpay
     *
     * @param orderId   Razorpay order ID
     * @param paymentId Razorpay payment ID
     * @param signature Razorpay signature
     * @return true if signature is valid
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(attributes, keySecret);

            if (isValid) {
                log.info("Payment signature verified for order: {}, payment: {}", orderId, paymentId);
            } else {
                log.warn("Invalid payment signature for order: {}, payment: {}", orderId, paymentId);
            }

            return isValid;

        } catch (RazorpayException e) {
            log.error("Error verifying payment signature: {}", e.getMessage(), e);
            throw new PaymentVerificationException("Failed to verify payment signature", e);
        }
    }

    /**
     * Verify webhook signature
     *
     * @param payload         Raw webhook payload body
     * @param webhookSignature Signature from X-Razorpay-Signature header
     * @param webhookSecret   Webhook secret from Razorpay dashboard
     * @return true if signature is valid
     */
    public boolean verifyWebhookSignature(String payload, String webhookSignature, String webhookSecret) {
        try {
            boolean isValid = Utils.verifyWebhookSignature(payload, webhookSignature, webhookSecret);

            if (isValid) {
                log.debug("Webhook signature verified successfully");
            } else {
                log.warn("Invalid webhook signature");
            }

            return isValid;

        } catch (RazorpayException e) {
            log.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convert amount to paise
     */
    public long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    /**
     * Convert paise to rupees
     */
    public BigDecimal toRupees(long paise) {
        return BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100));
    }
}
