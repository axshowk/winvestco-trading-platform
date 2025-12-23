package in.winvestco.funds_service.service;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.common.enums.LockStatus;
import in.winvestco.funds_service.client.LedgerClient;
import in.winvestco.funds_service.dto.CreateLedgerEntryRequest;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.exception.DuplicateLockException;
import in.winvestco.funds_service.exception.FundsLockNotFoundException;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.mapper.FundsMapper;
import in.winvestco.funds_service.model.FundsLock;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.repository.FundsLockRepository;
import in.winvestco.funds_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service for managing funds locks.
 * Uses LedgerClient to record all lock/unlock/settle operations to
 * ledger-service (SOURCE OF TRUTH).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FundsLockService {

    private final FundsLockRepository fundsLockRepository;
    private final WalletRepository walletRepository;
    private final LedgerClient ledgerClient;
    private final FundsMapper fundsMapper;
    private final FundsEventPublisher fundsEventPublisher;
    private final MeterRegistry meterRegistry;

    /**
     * Lock funds for an order
     */
    @Transactional
    public FundsLockDTO lockFunds(Long userId, String orderId, BigDecimal amount, String reason) {
        log.info("Locking {} for order {} (user {})", amount, orderId, userId);

        // Check for duplicate lock
        if (fundsLockRepository.existsByOrderId(orderId)) {
            throw new DuplicateLockException(orderId);
        }

        // Get wallet with lock
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));

        // Check sufficient balance
        if (!wallet.hasSufficientBalance(amount)) {
            throw new InsufficientFundsException(amount, wallet.getAvailableBalance());
        }

        BigDecimal balanceBefore = wallet.getAvailableBalance();

        // Lock the funds
        wallet.lockFunds(amount);
        walletRepository.save(wallet);

        // Create lock record
        FundsLock lock = FundsLock.builder()
                .walletId(wallet.getId())
                .orderId(orderId)
                .amount(amount)
                .status(LockStatus.LOCKED)
                .reason(reason != null ? reason : "Order placed")
                .build();

        FundsLock saved = fundsLockRepository.save(lock);

        // Record to ledger-service (SOURCE OF TRUTH)
        recordToLedger(
                wallet.getId(),
                LedgerEntryType.LOCK,
                amount,
                balanceBefore,
                wallet.getAvailableBalance(),
                orderId,
                "ORDER",
                "Funds locked for order: " + orderId);

        log.info("Locked {} for order {}. Lock ID: {}", amount, orderId, saved.getId());
        return fundsMapper.toFundsLockDTO(saved);
    }

    /**
     * Release locked funds (on cancel/reject)
     */
    @Transactional
    public FundsLockDTO releaseFunds(String orderId, String reason) {
        log.info("Releasing funds for order {}", orderId);

        FundsLock lock = fundsLockRepository.findByOrderId(orderId)
                .orElseThrow(() -> new FundsLockNotFoundException(orderId));

        if (lock.getStatus() != LockStatus.LOCKED) {
            log.warn("Lock {} is not in LOCKED status (current: {})", orderId, lock.getStatus());
            return fundsMapper.toFundsLockDTO(lock);
        }

        // Get wallet with lock
        Wallet wallet = walletRepository.findByIdForUpdate(lock.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + lock.getWalletId()));

        BigDecimal balanceBefore = wallet.getAvailableBalance();

        // Unlock the funds
        wallet.unlockFunds(lock.getAmount());
        walletRepository.save(wallet);

        // Update lock status
        lock.release(reason != null ? reason : "Order cancelled/rejected");
        FundsLock saved = fundsLockRepository.save(lock);

        // Record business metric: lock duration
        meterRegistry.timer("funds.lock.duration", "status", "RELEASED")
                .record(Duration.between(lock.getCreatedAt(), Instant.now()));

        // Record to ledger-service (SOURCE OF TRUTH)
        recordToLedger(
                wallet.getId(),
                LedgerEntryType.UNLOCK,
                lock.getAmount(),
                balanceBefore,
                wallet.getAvailableBalance(),
                orderId,
                "ORDER",
                "Funds released: " + (reason != null ? reason : "Order cancelled"));

        // Publish FundsReleasedEvent for notifications
        fundsEventPublisher.publishFundsReleased(
                wallet.getUserId(),
                wallet,
                saved,
                reason != null ? reason : "Order cancelled");

        log.info("Released {} for order {}", lock.getAmount(), orderId);
        return fundsMapper.toFundsLockDTO(saved);
    }

    /**
     * Settle locked funds (on trade execution)
     */
    @Transactional
    public FundsLockDTO settleFunds(String orderId, String reason) {
        log.info("Settling funds for order {}", orderId);

        FundsLock lock = fundsLockRepository.findByOrderId(orderId)
                .orElseThrow(() -> new FundsLockNotFoundException(orderId));

        if (lock.getStatus() != LockStatus.LOCKED) {
            log.warn("Lock {} is not in LOCKED status (current: {})", orderId, lock.getStatus());
            return fundsMapper.toFundsLockDTO(lock);
        }

        // Get wallet with lock
        Wallet wallet = walletRepository.findByIdForUpdate(lock.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + lock.getWalletId()));

        BigDecimal lockedBefore = wallet.getLockedBalance();

        // Settle the funds (remove from locked, don't add back to available)
        wallet.settleFunds(lock.getAmount());
        walletRepository.save(wallet);

        // Update lock status
        lock.settle(reason != null ? reason : "Trade executed");
        FundsLock saved = fundsLockRepository.save(lock);

        // Record business metric: lock duration
        meterRegistry.timer("funds.lock.duration", "status", "SETTLED")
                .record(Duration.between(lock.getCreatedAt(), Instant.now()));

        // Record to ledger-service (SOURCE OF TRUTH)
        recordToLedger(
                wallet.getId(),
                LedgerEntryType.TRADE_BUY,
                lock.getAmount(),
                lockedBefore,
                wallet.getLockedBalance(),
                orderId,
                "TRADE",
                "Trade executed for order: " + orderId);

        log.info("Settled {} for order {}", lock.getAmount(), orderId);
        return fundsMapper.toFundsLockDTO(saved);
    }

    /**
     * Get lock by order ID
     */
    @Transactional(readOnly = true)
    public FundsLockDTO getLockByOrderId(String orderId) {
        FundsLock lock = fundsLockRepository.findByOrderId(orderId)
                .orElseThrow(() -> new FundsLockNotFoundException(orderId));
        return fundsMapper.toFundsLockDTO(lock);
    }

    /**
     * Get all active locks for a wallet
     */
    @Transactional(readOnly = true)
    public List<FundsLockDTO> getActiveLocksForWallet(Long walletId) {
        List<FundsLock> locks = fundsLockRepository.findByWalletIdAndStatus(walletId, LockStatus.LOCKED);
        return fundsMapper.toFundsLockDTOList(locks);
    }

    /**
     * Get all locks for a wallet
     */
    @Transactional(readOnly = true)
    public List<FundsLockDTO> getAllLocksForWallet(Long walletId) {
        List<FundsLock> locks = fundsLockRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
        return fundsMapper.toFundsLockDTOList(locks);
    }

    /**
     * Record entry to ledger-service (SOURCE OF TRUTH)
     */
    private void recordToLedger(
            Long walletId,
            LedgerEntryType entryType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String referenceId,
            String referenceType,
            String description) {

        try {
            CreateLedgerEntryRequest request = CreateLedgerEntryRequest.builder()
                    .walletId(walletId)
                    .entryType(entryType)
                    .amount(amount)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .description(description)
                    .build();

            ledgerClient.recordEntry(request);
            log.debug("Recorded ledger entry: wallet={}, type={}, amount={}", walletId, entryType, amount);
        } catch (Exception e) {
            log.error("Failed to record ledger entry: wallet={}, type={}, amount={}", walletId, entryType, amount, e);
            throw new RuntimeException("Failed to record ledger entry", e);
        }
    }
}
