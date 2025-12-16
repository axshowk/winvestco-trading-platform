import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    TrendingUp, TrendingDown, RefreshCw, X,
    Briefcase, AlertCircle, LogIn, Trash2
} from 'lucide-react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import './Portfolio.css';

const Portfolio = () => {
    const navigate = useNavigate();
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [portfolio, setPortfolio] = useState(null);
    const [holdings, setHoldings] = useState([]);
    const [marketPrices, setMarketPrices] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [refreshing, setRefreshing] = useState(false);

    const getAuthHeaders = () => {
        const token = localStorage.getItem('accessToken');
        return {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        };
    };

    const fetchPortfolio = useCallback(async () => {
        try {
            const response = await fetch('/api/portfolios', {
                headers: getAuthHeaders()
            });

            if (response.status === 401) {
                setIsAuthenticated(false);
                setError('Session expired. Please login again.');
                return;
            }

            if (response.ok) {
                const data = await response.json();
                setPortfolio(data);
                setHoldings(data.holdings || []);
            } else if (response.status === 404) {
                // No portfolio yet - that's OK
                setPortfolio(null);
                setHoldings([]);
            } else {
                throw new Error('Failed to fetch portfolio');
            }
        } catch (err) {
            console.error('Error fetching portfolio:', err);
            setError('Failed to load portfolio');
        }
    }, []);

    const fetchMarketPrices = useCallback(async (holdingsList) => {
        const prices = {};
        for (const holding of holdingsList) {
            try {
                const response = await fetch(`/api/market/stocks/${holding.symbol}`);
                if (response.ok) {
                    const data = await response.json();
                    prices[holding.symbol] = {
                        ltp: data.lastPrice,
                        change: data.change,
                        pChange: data.pChange
                    };
                }
            } catch (err) {
                console.error(`Error fetching price for ${holding.symbol}:`, err);
            }
        }
        setMarketPrices(prices);
    }, []);

    const loadData = useCallback(async () => {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            setIsAuthenticated(false);
            setLoading(false);
            return;
        }

        setIsAuthenticated(true);
        setError(null);

        await fetchPortfolio();
        setLoading(false);
    }, [fetchPortfolio]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    useEffect(() => {
        if (holdings.length > 0) {
            fetchMarketPrices(holdings);
        }
    }, [holdings, fetchMarketPrices]);

    const handleRefresh = async () => {
        setRefreshing(true);
        await fetchPortfolio();
        if (holdings.length > 0) {
            await fetchMarketPrices(holdings);
        }
        setRefreshing(false);
    };

    const handleDeleteHolding = async (holdingId) => {
        if (!window.confirm('Are you sure you want to remove this holding?')) {
            return;
        }

        try {
            const response = await fetch(`/api/portfolios/holdings/${holdingId}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });

            if (response.ok) {
                await loadData();
            } else {
                setError('Failed to remove holding');
            }
        } catch (err) {
            console.error('Error deleting holding:', err);
            setError('Failed to remove holding');
        }
    };

    const formatCurrency = (value) => {
        if (value == null) return '₹-';
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            maximumFractionDigits: 2
        }).format(value);
    };

    const formatNumber = (value) => {
        if (value == null) return '-';
        return new Intl.NumberFormat('en-IN', {
            maximumFractionDigits: 2
        }).format(value);
    };

    // Calculate totals
    const calculateTotals = () => {
        let totalInvested = 0;
        let currentValue = 0;
        let dayPL = 0;

        holdings.forEach(holding => {
            const invested = (holding.quantity || 0) * (holding.averagePrice || 0);
            totalInvested += invested;

            const price = marketPrices[holding.symbol];
            if (price) {
                const current = (holding.quantity || 0) * price.ltp;
                currentValue += current;
                dayPL += (holding.quantity || 0) * (price.change || 0);
            } else {
                currentValue += invested;
            }
        });

        const overallPL = currentValue - totalInvested;
        const overallPLPercent = totalInvested > 0 ? (overallPL / totalInvested) * 100 : 0;
        const dayPLPercent = totalInvested > 0 ? (dayPL / totalInvested) * 100 : 0;

        return { totalInvested, currentValue, overallPL, overallPLPercent, dayPL, dayPLPercent };
    };

    const totals = calculateTotals();

    // Not authenticated
    if (!isAuthenticated && !loading) {
        return (
            <div className="page-wrapper">
                <Navbar onLogin={() => navigate('/login')} />
                <div className="portfolio-container">
                    <div className="auth-required-state">
                        <LogIn size={64} />
                        <h2>Login to View Your Portfolio</h2>
                        <p>Track your investments, view P&L, and manage your holdings</p>
                        <button className="login-btn" onClick={() => navigate('/login')}>
                            Login to Continue
                        </button>
                    </div>
                </div>
                <Footer />
            </div>
        );
    }

    // Loading state
    if (loading) {
        return (
            <div className="page-wrapper">
                <Navbar onLogin={() => navigate('/login')} />
                <div className="portfolio-container">
                    <div className="loading-state">
                        <div className="loading-spinner"></div>
                        <p>Loading your portfolio...</p>
                    </div>
                </div>
                <Footer />
            </div>
        );
    }

    return (
        <div className="page-wrapper">
            <Navbar onLogin={() => navigate('/login')} />
            <div className="portfolio-container">
                {/* Header */}
                <div className="portfolio-header">
                    <div className="header-left">
                        <h1>
                            <Briefcase size={28} />
                            Holdings
                            <span className="holdings-count">({holdings.length})</span>
                        </h1>
                    </div>
                    <div className="header-right">
                        <button
                            className={`refresh-btn ${refreshing ? 'spinning' : ''}`}
                            onClick={handleRefresh}
                            disabled={refreshing}
                        >
                            <RefreshCw size={18} />
                        </button>
                    </div>
                </div>

                {/* Error display */}
                {error && (
                    <div className="error-banner">
                        <AlertCircle size={18} />
                        <span>{error}</span>
                        <button onClick={() => setError(null)}><X size={16} /></button>
                    </div>
                )}

                {/* Summary Cards */}
                <div className="summary-cards">
                    <div className="summary-card glass">
                        <span className="summary-label">Total Invested</span>
                        <span className="summary-value">{formatCurrency(totals.totalInvested)}</span>
                    </div>
                    <div className="summary-card glass">
                        <span className="summary-label">Current Value</span>
                        <span className="summary-value">{formatCurrency(totals.currentValue)}</span>
                        <span className={`summary-change ${totals.overallPL >= 0 ? 'positive' : 'negative'}`}>
                            {totals.overallPL >= 0 ? '+' : ''}{formatCurrency(totals.overallPL)}
                            ({totals.overallPLPercent >= 0 ? '+' : ''}{totals.overallPLPercent.toFixed(2)}%)
                        </span>
                    </div>
                    <div className="summary-card glass">
                        <span className="summary-label">Today's P&L</span>
                        <span className={`summary-value ${totals.dayPL >= 0 ? 'positive' : 'negative'}`}>
                            {totals.dayPL >= 0 ? '+' : ''}{formatCurrency(totals.dayPL)}
                        </span>
                        <span className={`summary-change ${totals.dayPL >= 0 ? 'positive' : 'negative'}`}>
                            {totals.dayPLPercent >= 0 ? '+' : ''}{totals.dayPLPercent.toFixed(2)}%
                        </span>
                    </div>
                </div>

                {/* Holdings Table */}
                {holdings.length === 0 ? (
                    <div className="empty-state glass">
                        <Briefcase size={48} />
                        <h3>No Holdings Yet</h3>
                        <p>Place orders from the Markets page to build your portfolio</p>
                        <button className="action-btn large" onClick={() => navigate('/markets')}>
                            Explore Markets
                        </button>
                    </div>
                ) : (
                    <div className="holdings-table-container glass">
                        <table className="holdings-table">
                            <thead>
                                <tr>
                                    <th>Stock</th>
                                    <th className="right">Qty</th>
                                    <th className="right">Avg. Cost</th>
                                    <th className="right">LTP</th>
                                    <th className="right">Current Value</th>
                                    <th className="right">P&L</th>
                                    <th className="right">Day Change</th>
                                    <th className="center">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {holdings.map(holding => {
                                    const price = marketPrices[holding.symbol];
                                    const ltp = price?.ltp || holding.averagePrice;
                                    const currentValue = (holding.quantity || 0) * ltp;
                                    const invested = (holding.quantity || 0) * (holding.averagePrice || 0);
                                    const pl = currentValue - invested;
                                    const plPercent = invested > 0 ? (pl / invested) * 100 : 0;
                                    const dayChange = price?.pChange || 0;

                                    return (
                                        <tr
                                            key={holding.id}
                                            onClick={() => navigate(`/stock/${holding.symbol}`)}
                                            className="clickable"
                                        >
                                            <td className="stock-cell">
                                                <span className="stock-symbol">{holding.symbol}</span>
                                                {holding.companyName && (
                                                    <span className="company-name">{holding.companyName}</span>
                                                )}
                                            </td>
                                            <td className="right">{formatNumber(holding.quantity)}</td>
                                            <td className="right">{formatCurrency(holding.averagePrice)}</td>
                                            <td className="right">{formatCurrency(ltp)}</td>
                                            <td className="right">{formatCurrency(currentValue)}</td>
                                            <td className={`right ${pl >= 0 ? 'positive' : 'negative'}`}>
                                                <div className="pl-cell">
                                                    <span>{pl >= 0 ? '+' : ''}{formatCurrency(pl)}</span>
                                                    <span className="pl-percent">
                                                        ({plPercent >= 0 ? '+' : ''}{plPercent.toFixed(2)}%)
                                                    </span>
                                                </div>
                                            </td>
                                            <td className={`right ${dayChange >= 0 ? 'positive' : 'negative'}`}>
                                                <div className="day-change-cell">
                                                    {dayChange >= 0 ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
                                                    <span>{dayChange >= 0 ? '+' : ''}{dayChange.toFixed(2)}%</span>
                                                </div>
                                            </td>
                                            <td className="center actions-cell" onClick={e => e.stopPropagation()}>
                                                <button
                                                    className="action-btn delete"
                                                    onClick={() => handleDeleteHolding(holding.id)}
                                                    title="Remove holding"
                                                >
                                                    <Trash2 size={16} />
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
            <Footer />
        </div>
    );
};

export default Portfolio;
