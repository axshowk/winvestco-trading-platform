package in.winvestco.ledger_service.testdata;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.dto.CreateLedgerEntryRequest;
import in.winvestco.ledger_service.dto.LedgerEntryDTO;
import in.winvestco.ledger_service.model.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Factory for creating test data for ledger operations
 */
public class LedgerTestDataFactory {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    public static LedgerEntry createTestEntry() {
        return createTestEntry(1L, LedgerEntryType.DEPOSIT, 
                new BigDecimal("1000.00"), 
                BigDecimal.ZERO, 
                new BigDecimal("1000.00"));
    }

    public static LedgerEntry createTestEntry(Long walletId, LedgerEntryType entryType, 
                                            BigDecimal amount, BigDecimal balanceBefore, 
                                            BigDecimal balanceAfter) {
        return LedgerEntry.builder()
                .id(RANDOM.nextLong(1, 10000))
                .walletId(walletId)
                .entryType(entryType)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceId("REF-" + RANDOM.nextLong(1000, 9999))
                .referenceType("TEST")
                .description("Test entry for " + entryType)
                .createdAt(Instant.now().minusSeconds(RANDOM.nextLong(0, 86400)))
                .build();
    }

    public static CreateLedgerEntryRequest createTestRequest() {
        return createTestRequest(1L, LedgerEntryType.DEPOSIT, 
                new BigDecimal("1000.00"), 
                BigDecimal.ZERO, 
                new BigDecimal("1000.00"));
    }

    public static CreateLedgerEntryRequest createTestRequest(Long walletId, LedgerEntryType entryType,
                                                           BigDecimal amount, BigDecimal balanceBefore,
                                                           BigDecimal balanceAfter) {
        return CreateLedgerEntryRequest.builder()
                .walletId(walletId)
                .entryType(entryType)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceId("REF-" + RANDOM.nextLong(1000, 9999))
                .referenceType("TEST")
                .description("Test request for " + entryType)
                .build();
    }

    public static LedgerEntryDTO createTestDTO() {
        return LedgerEntryDTO.builder()
                .id(RANDOM.nextLong(1, 10000))
                .walletId(1L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("1000.00"))
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("1000.00"))
                .referenceId("REF-" + RANDOM.nextLong(1000, 9999))
                .referenceType("TEST")
                .description("Test DTO")
                .createdAt(Instant.now())
                .build();
    }

    public static List<LedgerEntry> createTestEntriesSeries(Long walletId, int count) {
        return createTestEntriesSeries(walletId, count, LedgerEntryType.DEPOSIT);
    }

    public static List<LedgerEntry> createTestEntriesSeries(Long walletId, int count, LedgerEntryType entryType) {
        BigDecimal entryAmount = new BigDecimal("100.00");
        
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    BigDecimal balanceBefore = entryAmount.multiply(BigDecimal.valueOf(i));
                    BigDecimal balanceAfter = balanceBefore.add(entryAmount);
                    
                    // Adjust for withdrawal/buy types
                    if (entryType == LedgerEntryType.WITHDRAWAL || entryType == LedgerEntryType.TRADE_BUY) {
                        balanceBefore = entryAmount.multiply(BigDecimal.valueOf(count - i));
                        balanceAfter = balanceBefore.subtract(entryAmount);
                    }
                    
                    return createTestEntry(walletId, entryType, entryAmount, balanceBefore, balanceAfter);
                })
                .toList();
    }

    public static List<LedgerEntry> createMixedTransactionHistory(Long walletId) {
        BigDecimal balance = BigDecimal.ZERO;
        List<LedgerEntry> entries = new java.util.ArrayList<>();
        
        // Initial deposit
        BigDecimal depositAmount = new BigDecimal("10000.00");
        entries.add(createTestEntry(walletId, LedgerEntryType.DEPOSIT, depositAmount, balance, balance.add(depositAmount)));
        balance = balance.add(depositAmount);
        
        // Some trades
        BigDecimal tradeAmount = new BigDecimal("1000.00");
        entries.add(createTestEntry(walletId, LedgerEntryType.TRADE_BUY, tradeAmount, balance, balance.subtract(tradeAmount)));
        balance = balance.subtract(tradeAmount);
        
        entries.add(createTestEntry(walletId, LedgerEntryType.TRADE_SELL, tradeAmount.add(new BigDecimal("100.00")), 
                balance, balance.add(tradeAmount).add(new BigDecimal("100.00"))));
        balance = balance.add(tradeAmount).add(new BigDecimal("100.00"));
        
        // A withdrawal
        BigDecimal withdrawalAmount = new BigDecimal("500.00");
        entries.add(createTestEntry(walletId, LedgerEntryType.WITHDRAWAL, withdrawalAmount, balance, balance.subtract(withdrawalAmount)));
        balance = balance.subtract(withdrawalAmount);
        
        // A fee
        BigDecimal feeAmount = new BigDecimal("10.00");
        entries.add(createTestEntry(walletId, LedgerEntryType.FEE, feeAmount, balance, balance.subtract(feeAmount)));
        
        return entries;
    }

    public static BigDecimal randomAmount(BigDecimal min, BigDecimal max) {
        double randomValue = RANDOM.nextDouble();
        return min.add(max.subtract(min).multiply(BigDecimal.valueOf(randomValue)));
    }

    public static Instant randomInstant(Instant start, Instant end) {
        long startSeconds = start.getEpochSecond();
        long endSeconds = end.getEpochSecond();
        long randomSeconds = RANDOM.nextLong(startSeconds, endSeconds);
        return Instant.ofEpochSecond(randomSeconds);
    }
}
