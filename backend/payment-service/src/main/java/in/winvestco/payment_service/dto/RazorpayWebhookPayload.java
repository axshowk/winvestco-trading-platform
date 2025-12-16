package in.winvestco.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Razorpay webhook payload structure
 * 
 * @see <a href="https://razorpay.com/docs/webhooks/payloads/payments/">Razorpay Webhook Docs</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayWebhookPayload {

    private String event;

    @JsonProperty("account_id")
    private String accountId;

    private WebhookPayloadData payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookPayloadData {
        private PaymentData payment;
        private OrderData order;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentData {
        private PaymentEntity entity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentEntity {
        private String id;

        @JsonProperty("order_id")
        private String orderId;

        private Long amount;
        private String currency;
        private String status;
        private String method;
        private String description;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_description")
        private String errorDescription;

        @JsonProperty("error_reason")
        private String errorReason;

        private String email;
        private String contact;
        private String vpa;
        private String wallet;
        private String bank;

        @JsonProperty("created_at")
        private Long createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderData {
        private OrderEntity entity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderEntity {
        private String id;
        private Long amount;
        private String currency;
        private String status;
        private String receipt;
    }
}
