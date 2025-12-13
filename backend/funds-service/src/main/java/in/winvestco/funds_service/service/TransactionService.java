package in.winvestco.funds_service.service;

import in.winvestco.common.enums.TransactionStatus;
import in.winvestco.common.enums.TransactionType;
import in.winvestco.funds_service.dto.DepositRequest;
import in.winvestco.funds_service.dto.TransactionDTO;
import in.winvestco.funds_service.dto.WithdrawRequest;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.exception.TransactionNotFoundException;
import in.winvestco.funds_service.mapper.FundsMapper;
import in.winvestco.funds_service.model.Transaction;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing deposits and withdrawals.
 * Supports async confirmation workflow.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final FundsMapper fundsMapper;

    /**
     * Initiate a deposit (creates PENDING transaction)
     */
    @Transactional
    public TransactionDTO initiateDeposit(Long userId, DepositRequest request) {
        log.info("Initiating deposit of {} for user {}", request.getAmount(), userId);

        Wallet wallet = walletService.getWalletEntityByUserId(userId);

        String externalRef = request.getExternalReference() != null
                ? request.getExternalReference()
                : "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
                .walletId(wallet.getId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .status(TransactionStatus.PENDING)
                .externalReference(externalRef)
                .description(request.getDescription() != null ? request.getDescription() : "Deposit")
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Created deposit transaction {} for user {}", saved.getId(), userId);

        return fundsMapper.toTransactionDTO(saved);
    }

    /**
     * Confirm a deposit (called by payment gateway webhook or admin)
     */
    @Transactional
    public TransactionDTO confirmDeposit(String externalReference) {
        log.info("Confirming deposit with reference: {}", externalReference);

        Transaction transaction = transactionRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new TransactionNotFoundException("externalReference", externalReference));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Transaction {} is not PENDING (current: {})", externalReference, transaction.getStatus());
            return fundsMapper.toTransactionDTO(transaction);
        }

        // Credit the wallet
        Wallet wallet = walletService.creditFunds(
                getWalletUserId(transaction.getWalletId()),
                transaction.getAmount(),
                externalReference,
                "TRANSACTION",
                "Deposit confirmed"
        );

        // Update transaction status
        transaction.complete();
        Transaction saved = transactionRepository.save(transaction);

        log.info("Confirmed deposit {}. Wallet balance: {}", externalReference, wallet.getAvailableBalance());
        return fundsMapper.toTransactionDTO(saved);
    }

    /**
     * Initiate a withdrawal (creates PENDING transaction)
     */
    @Transactional
    public TransactionDTO initiateWithdrawal(Long userId, WithdrawRequest request) {
        log.info("Initiating withdrawal of {} for user {}", request.getAmount(), userId);

        Wallet wallet = walletService.getWalletEntityByUserId(userId);

        // Check sufficient balance
        if (!wallet.hasSufficientBalance(request.getAmount())) {
            throw new InsufficientFundsException(request.getAmount(), wallet.getAvailableBalance());
        }

        String externalRef = "WDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
                .walletId(wallet.getId())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .status(TransactionStatus.PENDING)
                .externalReference(externalRef)
                .description(request.getDescription() != null ? request.getDescription() : "Withdrawal")
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Created withdrawal transaction {} for user {}", saved.getId(), userId);

        return fundsMapper.toTransactionDTO(saved);
    }

    /**
     * Complete a withdrawal (debit funds from wallet)
     */
    @Transactional
    public TransactionDTO completeWithdrawal(String externalReference) {
        log.info("Completing withdrawal with reference: {}", externalReference);

        Transaction transaction = transactionRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new TransactionNotFoundException("externalReference", externalReference));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Transaction {} is not PENDING (current: {})", externalReference, transaction.getStatus());
            return fundsMapper.toTransactionDTO(transaction);
        }

        // Debit the wallet
        walletService.debitFunds(
                getWalletUserId(transaction.getWalletId()),
                transaction.getAmount(),
                externalReference,
                "TRANSACTION",
                "Withdrawal completed"
        );

        // Update transaction status
        transaction.complete();
        Transaction saved = transactionRepository.save(transaction);

        log.info("Completed withdrawal {}", externalReference);
        return fundsMapper.toTransactionDTO(saved);
    }

    /**
     * Fail a transaction
     */
    @Transactional
    public TransactionDTO failTransaction(String externalReference, String reason) {
        log.info("Failing transaction {}: {}", externalReference, reason);

        Transaction transaction = transactionRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new TransactionNotFoundException("externalReference", externalReference));

        transaction.fail(reason);
        Transaction saved = transactionRepository.save(transaction);

        return fundsMapper.toTransactionDTO(saved);
    }

    /**
     * Cancel a transaction
     */
    @Transactional
    public TransactionDTO cancelTransaction(String externalReference, String reason) {
        log.info("Cancelling transaction {}: {}", externalReference, reason);

        Transaction transaction = transactionRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new TransactionNotFoundException("externalReference", externalReference));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Cannot cancel transaction {} (status: {})", externalReference, transaction.getStatus());
            return fundsMapper.toTransactionDTO(transaction);
        }

        transaction.cancel(reason);
        Transaction saved = transactionRepository.save(transaction);

        return fundsMapper.toTransactionDTO(saved);
    }

    /**
     * Get transaction by external reference
     */
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionByReference(String externalReference) {
        Transaction transaction = transactionRepository.findByExternalReference(externalReference)
                .orElseThrow(() -> new TransactionNotFoundException("externalReference", externalReference));
        return fundsMapper.toTransactionDTO(transaction);
    }

    /**
     * Get transactions for a user (paginated)
     */
    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionsForUser(Long userId, Pageable pageable) {
        Wallet wallet = walletService.getWalletEntityByUserId(userId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(fundsMapper::toTransactionDTO);
    }

    /**
     * Helper to get userId from walletId
     * In a real system, this would be cached or part of a join
     */
    private Long getWalletUserId(Long walletId) {
        // This is a simplified implementation
        // In production, you might want to fetch this more efficiently
        return walletService.getWalletEntityByUserId(walletId) != null ? walletId : null;
    }
}
