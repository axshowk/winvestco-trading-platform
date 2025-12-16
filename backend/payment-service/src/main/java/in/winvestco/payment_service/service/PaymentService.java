package in.winvestco.payment_service.service;

import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.enums.PaymentProvider;
import in.winvestco.common.enums.PaymentStatus;
import in.winvestco.payment_service.dto.*;
import in.winvestco.payment_service.exception.PaymentNotFoundException;
import in.winvestco.payment_service.exception.PaymentVerificationException;
import in.winvestco.payment_service.mapper.PaymentMapper;
import in.winvestco.payment_service.messaging.PaymentEventPublisher;
import in.winvestco.payment_service.model.Payment;
import in.winvestco.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Core payment service handling payment lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final PaymentEventPublisher eventPublisher;
    private final PaymentMapper paymentMapper;

    @Value("${payment.expiry-minutes:15}")
    private int expiryMinutes;

    @Value("${payment.currency:INR}")
    private String defaultCurrency;

    /**
     * Initiate a new payment
     */
    @Transactional
    public RazorpayOrderResponse initiatePayment(Long userId, InitiatePaymentRequest request) {
        log.info("Initiating payment for user: {}, amount: {}", userId, request.getAmount());

        // Generate receipt
        String receipt = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create payment record
        Payment payment = Payment.builder()
            .userId(userId)
            .walletId(request.getWalletId())
            .amount(request.getAmount())
            .currency(defaultCurrency)
            .status(PaymentStatus.CREATED)
            .provider(PaymentProvider.RAZORPAY)
            .receipt(receipt)
            .description(request.getDescription() != null ? request.getDescription() : "Wallet deposit")
            .build();

        payment = paymentRepository.save(payment);
        log.debug("Created payment record: {}", payment.getId());

        // Create Razorpay order
        String razorpayOrderId = razorpayService.createOrder(
            request.getAmount(),
            defaultCurrency,
            receipt
        );

        // Update payment with order ID and expiry
        Instant expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);
        payment.initiate(razorpayOrderId, expiresAt);
        payment = paymentRepository.save(payment);

        // Publish event
        eventPublisher.publishPaymentCreated(payment);

        // Build response for frontend
        return RazorpayOrderResponse.builder()
            .paymentId(payment.getId())
            .orderId(razorpayOrderId)
            .amount(payment.getAmount())
            .amountInPaise(razorpayService.toPaise(payment.getAmount()))
            .currency(payment.getCurrency())
            .receipt(receipt)
            .razorpayKeyId(razorpayService.getKeyId())
            .description(payment.getDescription())
            .build();
    }

    /**
     * Verify payment after frontend checkout callback
     */
    @Transactional
    public PaymentResponse verifyPayment(Long userId, VerifyPaymentRequest request) {
        log.info("Verifying payment for order: {}", request.getRazorpayOrderId());

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
            .orElseThrow(() -> new PaymentNotFoundException("razorpayOrderId", request.getRazorpayOrderId()));

        // Verify ownership
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentVerificationException("Payment does not belong to this user");
        }

        // Check if already processed
        if (payment.isTerminal()) {
            log.info("Payment {} already in terminal state: {}", payment.getId(), payment.getStatus());
            return paymentMapper.toResponse(payment);
        }

        // Verify signature
        boolean isValid = razorpayService.verifySignature(
            request.getRazorpayOrderId(),
            request.getRazorpayPaymentId(),
            request.getRazorpaySignature()
        );

        if (!isValid) {
            payment.markFailed("Invalid payment signature", "SIGNATURE_VERIFICATION_FAILED");
            paymentRepository.save(payment);
            eventPublisher.publishPaymentFailed(payment);
            throw new PaymentVerificationException("Payment signature verification failed");
        }

        // Mark as success
        payment.markSuccess(
            request.getRazorpayPaymentId(),
            request.getRazorpaySignature(),
            PaymentMethod.OTHER // Will be updated from webhook with actual method
        );
        payment = paymentRepository.save(payment);

        // Publish success event - funds-service will credit wallet
        eventPublisher.publishPaymentSuccess(payment);

        log.info("Payment verified successfully: {}", payment.getId());
        return paymentMapper.toResponse(payment);
    }

    /**
     * Handle Razorpay webhook
     */
    @Transactional
    public void handleWebhook(RazorpayWebhookPayload payload) {
        log.info("Processing webhook event: {}", payload.getEvent());

        if (payload.getPayload() == null || payload.getPayload().getPayment() == null) {
            log.warn("Webhook payload missing payment data");
            return;
        }

        RazorpayWebhookPayload.PaymentEntity paymentEntity = payload.getPayload().getPayment().getEntity();
        String orderId = paymentEntity.getOrderId();

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
            .orElse(null);

        if (payment == null) {
            log.warn("Payment not found for Razorpay order: {}", orderId);
            return;
        }

        // Check if already processed
        if (payment.isTerminal()) {
            log.info("Payment {} already processed, skipping webhook", payment.getId());
            return;
        }

        String event = payload.getEvent();

        switch (event) {
            case "payment.authorized":
            case "payment.captured":
                handlePaymentSuccess(payment, paymentEntity);
                break;
            case "payment.failed":
                handlePaymentFailed(payment, paymentEntity);
                break;
            default:
                log.debug("Ignoring webhook event: {}", event);
        }
    }

    private void handlePaymentSuccess(Payment payment, RazorpayWebhookPayload.PaymentEntity entity) {
        PaymentMethod method = parsePaymentMethod(entity.getMethod());

        payment.markSuccess(entity.getId(), null, method);
        paymentRepository.save(payment);

        eventPublisher.publishPaymentSuccess(payment);
        log.info("Payment {} marked as SUCCESS via webhook", payment.getId());
    }

    private void handlePaymentFailed(Payment payment, RazorpayWebhookPayload.PaymentEntity entity) {
        String reason = entity.getErrorDescription() != null 
            ? entity.getErrorDescription() 
            : entity.getErrorReason();

        payment.markFailed(reason, entity.getErrorCode());
        paymentRepository.save(payment);

        eventPublisher.publishPaymentFailed(payment);
        log.info("Payment {} marked as FAILED via webhook: {}", payment.getId(), reason);
    }

    private PaymentMethod parsePaymentMethod(String method) {
        if (method == null) return PaymentMethod.OTHER;

        return switch (method.toLowerCase()) {
            case "upi" -> PaymentMethod.UPI;
            case "netbanking" -> PaymentMethod.NETBANKING;
            case "card" -> PaymentMethod.CARD;
            case "wallet" -> PaymentMethod.WALLET;
            default -> PaymentMethod.OTHER;
        };
    }

    /**
     * Get payment by ID
     */
    public PaymentResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentNotFoundException(paymentId);
        }

        return paymentMapper.toResponse(payment);
    }

    /**
     * Get payment history for user
     */
    public List<PaymentResponse> getPaymentHistory(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(paymentMapper::toResponse)
            .toList();
    }

    /**
     * Mark pending payment as pending (called when user opens payment page)
     */
    @Transactional
    public PaymentResponse markPaymentPending(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentNotFoundException(paymentId);
        }

        if (payment.getStatus() == PaymentStatus.INITIATED) {
            payment.markPending();
            payment = paymentRepository.save(payment);
        }

        return paymentMapper.toResponse(payment);
    }
}
