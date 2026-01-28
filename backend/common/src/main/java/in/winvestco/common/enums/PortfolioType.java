package in.winvestco.common.enums;

import lombok.Getter;

/**
 * Enum representing the type of portfolio.
 */
@Getter
public enum PortfolioType {
    MAIN, // Primary investment portfolio
    PAPER_TRADING, // Practice/simulation with virtual money
    WATCHLIST, // Tracking only, no real holdings
    RETIREMENT, // Long-term retirement portfolio
    CUSTOM // User-defined
}
