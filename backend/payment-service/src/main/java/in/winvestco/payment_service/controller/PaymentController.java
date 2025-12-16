package in.winvestco.payment_service.controller;

import in.winvestco.payment_service.dto.*;
import in.winvestco.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for payment operations
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment operations API")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a new payment", description = "Creates a Razorpay order and returns details for frontend checkout")
    public ResponseEntity<RazorpayOrderResponse> initiatePayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InitiatePaymentRequest request) {

        Long userId = extractUserId(jwt);
        log.info("Payment initiation request from user: {}", userId);

        RazorpayOrderResponse response = paymentService.initiatePayment(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify payment after checkout", description = "Verifies payment signature after Razorpay checkout completes")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VerifyPaymentRequest request) {

        Long userId = extractUserId(jwt);
        log.info("Payment verification request from user: {}", userId);

        PaymentResponse response = paymentService.verifyPayment(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Retrieves a specific payment by its ID")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        Long userId = extractUserId(jwt);
        PaymentResponse response = paymentService.getPayment(userId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Get payment history", description = "Retrieves all payments for the current user")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = extractUserId(jwt);
        List<PaymentResponse> payments = paymentService.getPaymentHistory(userId);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/{id}/pending")
    @Operation(summary = "Mark payment as pending", description = "Called when user opens payment page")
    public ResponseEntity<PaymentResponse> markPending(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        Long userId = extractUserId(jwt);
        PaymentResponse response = paymentService.markPaymentPending(userId, id);
        return ResponseEntity.ok(response);
    }

    private Long extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Number) {
            return ((Number) userIdClaim).longValue();
        }
        if (userIdClaim instanceof String) {
            return Long.parseLong((String) userIdClaim);
        }
        throw new IllegalStateException("User ID not found in JWT");
    }
}
