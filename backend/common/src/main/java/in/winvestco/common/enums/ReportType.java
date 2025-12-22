package in.winvestco.common.enums;

/**
 * Types of reports that can be generated
 */
public enum ReportType {
    P_AND_L, // Profit & Loss statement (realized + unrealized)
    TAX_REPORT, // Capital gains tax summary (STCG/LTCG)
    TRANSACTION_HISTORY, // Ledger export
    HOLDINGS_SUMMARY, // Current portfolio snapshot
    TRADE_HISTORY // All trades export
}
