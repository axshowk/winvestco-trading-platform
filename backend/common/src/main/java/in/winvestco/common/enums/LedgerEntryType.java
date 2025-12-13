package in.winvestco.common.enums;

/**
 * Types of ledger entries - immutable audit trail
 */
public enum LedgerEntryType {
    DEPOSIT,
    WITHDRAWAL,
    TRADE_BUY,
    TRADE_SELL,
    LOCK,
    UNLOCK,
    FEE,
    DIVIDEND,
    ADJUSTMENT
}
