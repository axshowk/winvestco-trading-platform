package in.winvestco.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.payment_service.dto.RazorpayWebhookPayload;
import in.winvestco.payment_service.service.PaymentService;
import in.winvestco.payment_service.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook controller for Razorpay callbacks.
 * 
 * This endpoint is public (no JWT auth) but secured via signature verification.
 */
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Received Razorpay webhook");

        // Verify signature if webhook secret is configured
        if (webhookSecret != null && !webhookSecret.isEmpty() && signature != null) {
            boolean isValid = razorpayService.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.badRequest().body("Invalid signature");
            }
        }

        try {
            RazorpayWebhookPayload webhookPayload = objectMapper.readValue(payload, RazorpayWebhookPayload.class);
            paymentService.handleWebhook(webhookPayload);
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            // Return 200 to prevent Razorpay from retrying
            return ResponseEntity.ok("Error processed");
        }
    }
}
