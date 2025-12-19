package in.winvestco.funds_service.service;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.common.enums.WalletStatus;
import in.winvestco.funds_service.client.LedgerClient;
import in.winvestco.funds_service.dto.CreateLedgerEntryRequest;
import in.winvestco.funds_service.dto.LedgerEntryDTO;
import in.winvestco.funds_service.dto.WalletDTO;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.exception.WalletNotFoundException;
import in.winvestco.funds_service.mapper.FundsMapper;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for managing user wallets and balances.
 * Uses LedgerClient to record all transactions to the ledger-service (SOURCE OF
 * TRUTH).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerClient ledgerClient;
    private final FundsMapper fundsMapper;
    private final FundsEventPublisher fundsEventPublisher;

    /**
     * Create a new wallet for a user.
     * Called when UserCreatedEvent is received.
     */
    @Transactional
    public Wallet createWalletForUser(Long userId) {
        log.info("Creating wallet for user: {}", userId);

        // Check if wallet already exists
        if (walletRepository.existsByUserId(userId)) {
            log.warn("Wallet already exists for user: {}", userId);
            return walletRepository.findByUserId(userId).orElse(null);
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .availableBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet saved = walletRepository.save(wallet);
        log.info("Created wallet {} for user {}", saved.getId(), userId);

        return saved;
    }

    /**
     * Get wallet by user ID
     */
    @Transactional(readOnly = true)
    public WalletDTO getWalletByUserId(Long userId) {
        log.debug("Fetching wallet for user: {}", userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("userId", userId));

        return fundsMapper.toWalletDTO(wallet);
    }

    /**
     * Get wallet entity by user ID (internal use)
     */
    @Transactional(readOnly = true)
    public Wallet getWalletEntityByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("userId", userId));
    }

    /**
     * Get wallet entity by user ID with lock for update
     */
    @Transactional
    public Wallet getWalletForUpdate(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletNotFoundException("userId", userId));
    }

    /**
     * Get wallet entity by wallet ID (internal use)
     */
    @Transactional(readOnly = true)
    public Wallet getWalletById(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("walletId", walletId));
    }

    /**
     * Credit funds to wallet (e.g., deposit confirmation)
     * Records entry to ledger-service (SOURCE OF TRUTH)
     */
    @Transactional
    public Wallet creditFunds(Long userId, BigDecimal amount, String referenceId, String referenceType,
            String description) {
        log.info("Crediting {} to user {} wallet", amount, userId);

        Wallet wallet = getWalletForUpdate(userId);
        BigDecimal balanceBefore = wallet.getAvailableBalance();

        wallet.credit(amount);
        Wallet saved = walletRepository.save(wallet);

        // Record to ledger-service (SOURCE OF TRUTH)
        recordToLedger(
                wallet.getId(),
                LedgerEntryType.DEPOSIT,
                amount,
                balanceBefore,
                wallet.getAvailableBalance(),
                referenceId,
                referenceType,
                description);

        // Publish FundsDepositedEvent for notifications
        fundsEventPublisher.publishFundsDeposited(userId, saved, amount, referenceId, referenceType);

        log.info("Credited {} to wallet {}. New balance: {}", amount, wallet.getId(), wallet.getAvailableBalance());
        return saved;
    }

    /**
     * Debit funds from wallet (e.g., withdrawal)
     * Records entry to ledger-service (SOURCE OF TRUTH)
     */
    @Transactional
    public Wallet debitFunds(Long userId, BigDecimal amount, String referenceId, String referenceType,
            String description) {
        log.info("Debiting {} from user {} wallet", amount, userId);

        Wallet wallet = getWalletForUpdate(userId);

        if (!wallet.hasSufficientBalance(amount)) {
            throw new InsufficientFundsException(amount, wallet.getAvailableBalance());
        }

        BigDecimal balanceBefore = wallet.getAvailableBalance();
        wallet.debit(amount);
        Wallet saved = walletRepository.save(wallet);

        // Record to ledger-service (SOURCE OF TRUTH)
        recordToLedger(
                wallet.getId(),
                LedgerEntryType.WITHDRAWAL,
                amount,
                balanceBefore,
                wallet.getAvailableBalance(),
                referenceId,
                referenceType,
                description);

        // Publish FundsWithdrawnEvent for notifications
        fundsEventPublisher.publishFundsWithdrawn(userId, saved, amount, referenceId, referenceType, null);

        log.info("Debited {} from wallet {}. New balance: {}", amount, wallet.getId(), wallet.getAvailableBalance());
        return saved;
    }

    /**
     * Check if user has a wallet
     */
    @Transactional(readOnly = true)
    public boolean hasWallet(Long userId) {
        return walletRepository.existsByUserId(userId);
    }

    /**
     * Get total balance (available + locked)
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalance(Long userId) {
        Wallet wallet = getWalletEntityByUserId(userId);
        return wallet.getTotalBalance();
    }

    /**
     * Get available balance only
     */
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(Long userId) {
        Wallet wallet = getWalletEntityByUserId(userId);
        return wallet.getAvailableBalance();
    }

    /**
     * Get ledger entries for a wallet from ledger-service
     */
    public Page<LedgerEntryDTO> getLedgerEntries(Long walletId, int page, int size) {
        return ledgerClient.getWalletEntries(walletId, page, size);
    }

    /**
     * Get all ledger entries for a wallet
     */
    public List<LedgerEntryDTO> getAllLedgerEntries(Long walletId) {
        return ledgerClient.getAllWalletEntries(walletId);
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
            // In production, you might want to use a fallback or retry mechanism
            throw new RuntimeException("Failed to record ledger entry", e);
        }
    }
}
