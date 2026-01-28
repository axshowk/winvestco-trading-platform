import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    TrendingUp, TrendingDown, RefreshCw, X, Plus, ChevronDown,
    Briefcase, AlertCircle, LogIn, Trash2, Wifi, WifiOff,
    CheckCircle, Star
} from 'lucide-react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import usePortfolioWebSocket from '../hooks/usePortfolioWebSocket';
import './Portfolio.css';

const Portfolio = () => {
    const navigate = useNavigate();
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [portfolio, setPortfolio] = useState(null);
    const [portfolios, setPortfolios] = useState([]);
    const [selectedPortfolioId, setSelectedPortfolioId] = useState(null);
    const [holdings, setHoldings] = useState([]);
    const [marketPrices, setMarketPrices] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [refreshing, setRefreshing] = useState(false);
    const [userId, setUserId] = useState(null);
    const [tradeNotification, setTradeNotification] = useState(null);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showSelector, setShowSelector] = useState(false);
    const [newPortfolio, setNewPortfolio] = useState({
        name: '',
        description: '',
        portfolioType: 'MAIN',
        isDefault: false
    });

    const getAuthHeaders = () => {
        const token = localStorage.getItem('accessToken');
        return {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        };
    };

    // Extract userId from JWT token
    const extractUserIdFromToken = useCallback(() => {
        const token = localStorage.getItem('accessToken');
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.userId || payload.sub;
        } catch (e) {
            console.error('Failed to parse token:', e);
            return null;
        }
    }, []);

    // Handle WebSocket messages
    const handleWebSocketMessage = useCallback((data) => {
        console.log('WebSocket update received:', data);

        switch (data.type) {
            case 'PRICE_UPDATE':
                if (data.priceUpdate && data.symbol) {
                    setMarketPrices(prev => ({
                        ...prev,
                        [data.symbol]: {
                            ltp: data.priceUpdate.lastPrice,
                            change: data.priceUpdate.change,
                            pChange: data.priceUpdate.changePercent
                        }
                    }));
                }
                break;

            case 'TRADE_EXECUTED':
                // Show trade notification
                setTradeNotification(data.message);
                setTimeout(() => setTradeNotification(null), 5000);
                // Refresh portfolio data
                fetchPortfolio();
                break;

            case 'PORTFOLIO_UPDATE':
            case 'HOLDING_UPDATE':
                // Refresh portfolio data on updates
                fetchPortfolio();
                break;

            case 'ERROR':
                setError(data.message);
                break;

            default:
                console.log('Unhandled WebSocket message type:', data.type);
        }
    }, []);

    // WebSocket connection
    const { isConnected, connectionStatus } = usePortfolioWebSocket(
        userId,
        handleWebSocketMessage,
        isAuthenticated
    );

    const fetchAllPortfolios = useCallback(async () => {
        try {
            const response = await fetch('/api/v1/portfolios/all', {
                headers: getAuthHeaders()
            });
            if (response.ok) {
                const data = await response.json();
                setPortfolios(data);
                // Set initial selection to default if not already set
                if (!selectedPortfolioId) {
                    const defaultPortfolio = data.find(p => p.isDefault);
                    if (defaultPortfolio) {
                        setSelectedPortfolioId(defaultPortfolio.id);
                    } else if (data.length > 0) {
                        setSelectedPortfolioId(data[0].id);
                    }
                }
            }
        } catch (err) {
            console.error('Error fetching all portfolios:', err);
        }
    }, [selectedPortfolioId]);

    const fetchPortfolio = useCallback(async (id = null) => {
        try {
            const url = id ? `/api/v1/portfolios/${id}` : '/api/v1/portfolios';
            const response = await fetch(url, {
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
                if (!selectedPortfolioId) {
                    setSelectedPortfolioId(data.id);
                }
            } else if (response.status === 404) {
                setPortfolio(null);
                setHoldings([]);
            } else {
                throw new Error('Failed to fetch portfolio');
            }
        } catch (err) {
            console.error('Error fetching portfolio:', err);
            setError('Failed to load portfolio');
        }
    }, [selectedPortfolioId]);

    const fetchMarketPrices = useCallback(async (holdingsList) => {
        const prices = {};
        for (const holding of holdingsList) {
            // Optimization: If price already enriched by backend, prioritize that
            if (holding.currentPrice) {
                prices[holding.symbol] = {
                    ltp: holding.currentPrice,
                    change: holding.dayChange,
                    pChange: holding.dayChangePercentage
                };
                continue;
            }
            try {
                const response = await fetch(`/api/v1/market/stocks/${holding.symbol}`);
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
            setUserId(null);
            setLoading(false);
            return;
        }

        setIsAuthenticated(true);
        setError(null);

        // Extract and set userId for WebSocket
        const extractedUserId = extractUserIdFromToken();
        setUserId(extractedUserId);

        await fetchAllPortfolios();
        await fetchPortfolio();
        setLoading(false);
    }, [fetchPortfolio, fetchAllPortfolios, extractUserIdFromToken]);

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
        await fetchAllPortfolios();
        await fetchPortfolio(selectedPortfolioId);
        if (holdings.length > 0) {
            await fetchMarketPrices(holdings);
        }
        setRefreshing(false);
    };

    const handlePortfolioSelect = async (id) => {
        setSelectedPortfolioId(id);
        setShowSelector(false);
        setLoading(true);
        await fetchPortfolio(id);
        setLoading(false);
    };

    const handleCreatePortfolio = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch('/api/v1/portfolios', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify(newPortfolio)
            });

            if (response.ok) {
                const data = await response.json();
                setShowCreateModal(false);
                setNewPortfolio({ name: '', description: '', portfolioType: 'MAIN', isDefault: false });
                await fetchAllPortfolios();
                await handlePortfolioSelect(data.id);
            } else {
                setError('Failed to create portfolio');
            }
        } catch (err) {
            console.error('Error creating portfolio:', err);
            setError('Error creating portfolio');
        }
    };

    const handleSetDefault = async (id) => {
        try {
            const response = await fetch(`/api/v1/portfolios/${id}/set-default`, {
                method: 'PUT',
                headers: getAuthHeaders()
            });

            if (response.ok) {
                await fetchAllPortfolios();
                // If it's the current one, the state already reflects isDefault, 
                // but we fetch all to update the list/badges
            } else {
                setError('Failed to set default portfolio');
            }
        } catch (err) {
            console.error('Error setting default:', err);
            setError('Error setting default portfolio');
        }
    };

    const handleDeleteHolding = async (holdingId) => {
        if (!window.confirm('Are you sure you want to remove this holding?')) {
            return;
        }

        try {
            const response = await fetch(`/api/v1/portfolios/holdings/${holdingId}`, {
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
                        <div className="portfolio-selector-wrapper">
                            <button 
                                className="portfolio-selector-btn glass"
                                onClick={() => setShowSelector(!showSelector)}
                            >
                                <Briefcase size={24} />
                                <div className="selected-info">
                                    <span className="portfolio-name">{portfolio?.name || 'Load Portfolio...'}</span>
                                    <div className="portfolio-badges">
                                        <span className={`badge type-${portfolio?.portfolioType?.toLowerCase()}`}>
                                            {portfolio?.portfolioType}
                                        </span>
                                        {portfolio?.isDefault && <span className="badge default"><Star size={10} fill="currentColor" /> Default</span>}
                                    </div>
                                </div>
                                <ChevronDown size={18} className={showSelector ? 'rotated' : ''} />
                            </button>

                            {showSelector && (
                                <div className="portfolio-dropdown glass">
                                    <div className="dropdown-header">
                                        <span>Select Portfolio</span>
                                    </div>
                                    <div className="portfolio-list">
                                        {portfolios.map(p => (
                                            <div 
                                                key={p.id} 
                                                className={`portfolio-item ${p.id === selectedPortfolioId ? 'active' : ''}`}
                                                onClick={() => handlePortfolioSelect(p.id)}
                                            >
                                                <div className="item-info">
                                                    <span className="item-name">{p.name} {p.isDefault && '⭐'}</span>
                                                    <span className="item-type">{p.portfolioType}</span>
                                                </div>
                                                {p.id !== selectedPortfolioId && (
                                                    <button 
                                                        className="set-default-btn"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            handleSetDefault(p.id);
                                                        }}
                                                        title="Set as Default"
                                                    >
                                                        <Star size={14} />
                                                    </button>
                                                )}
                                                {p.id === selectedPortfolioId && <CheckCircle size={14} className="active-icon" />}
                                            </div>
                                        ))}
                                    </div>
                                    <button 
                                        className="create-portfolio-btn"
                                        onClick={() => {
                                            setShowSelector(false);
                                            setShowCreateModal(true);
                                        }}
                                    >
                                        <Plus size={16} /> Create New Portfolio
                                    </button>
                                </div>
                            )}
                        </div>
                        <span className="holdings-count">({holdings.length} stocks)</span>
                        {/* WebSocket Connection Status */}
                        <div className={`connection-status ${isConnected ? 'connected' : 'disconnected'}`}
                            title={`Real-time updates: ${connectionStatus}`}>
                            {isConnected ? <Wifi size={14} /> : <WifiOff size={14} />}
                            <span>{isConnected ? 'Live' : 'Offline'}</span>
                        </div>
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

                {/* Trade Notification Toast */}
                {tradeNotification && (
                    <div className="trade-notification">
                        <TrendingUp size={18} />
                        <span>{tradeNotification}</span>
                        <button onClick={() => setTradeNotification(null)}><X size={14} /></button>
                    </div>
                )}

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
                        <button className="btn-primary" onClick={() => navigate('/markets')}>
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

            {/* Create Portfolio Modal */}
            {showCreateModal && (
                <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
                    <div className="modal-content glass" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Create New Portfolio</h2>
                            <button className="close-btn" onClick={() => setShowCreateModal(false)}>
                                <X size={24} />
                            </button>
                        </div>
                        <form onSubmit={handleCreatePortfolio}>
                            <div className="form-group">
                                <label>Portfolio Name</label>
                                <input 
                                    type="text" 
                                    required 
                                    placeholder="e.g. Long Term Stocks"
                                    value={newPortfolio.name}
                                    onChange={e => setNewPortfolio({...newPortfolio, name: e.target.value})}
                                />
                            </div>
                            <div className="form-group">
                                <label>Description</label>
                                <input 
                                    type="text" 
                                    placeholder="Optional description"
                                    value={newPortfolio.description}
                                    onChange={e => setNewPortfolio({...newPortfolio, description: e.target.value})}
                                />
                            </div>
                            <div className="form-row">
                                <div className="form-group">
                                    <label>Type</label>
                                    <select 
                                        value={newPortfolio.portfolioType}
                                        onChange={e => setNewPortfolio({...newPortfolio, portfolioType: e.target.value})}
                                    >
                                        <option value="MAIN">Main</option>
                                        <option value="PAPER_TRADING">Paper Trading</option>
                                        <option value="WATCHLIST">Watchlist</option>
                                        <option value="RETIREMENT">Retirement</option>
                                        <option value="CUSTOM">Custom</option>
                                    </select>
                                </div>
                                <div className="form-group checkbox">
                                    <label>
                                        <input 
                                            type="checkbox" 
                                            checked={newPortfolio.isDefault}
                                            onChange={e => setNewPortfolio({...newPortfolio, isDefault: e.target.checked})}
                                        />
                                        Set as Default
                                    </label>
                                </div>
                            </div>
                            <div className="form-actions">
                                <button type="button" className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn-primary">
                                    Create Portfolio
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
};

export default Portfolio;
