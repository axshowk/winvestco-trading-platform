import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { TrendingUp, TrendingDown, ArrowLeft, RefreshCw, Activity, DollarSign, BarChart3, ShoppingCart, X, AlertCircle, CheckCircle } from 'lucide-react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import TradingViewChart from '../components/TradingViewChart';
import './StockDetails.css';

const StockDetails = () => {
    const { symbol } = useParams();
    const navigate = useNavigate();
    const [stockData, setStockData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [refreshing, setRefreshing] = useState(false);

    // Trade modal state
    const [showTradeModal, setShowTradeModal] = useState(false);
    const [tradeType, setTradeType] = useState('buy'); // 'buy' or 'sell'
    const [quantity, setQuantity] = useState('');
    const [trading, setTrading] = useState(false);
    const [tradeError, setTradeError] = useState(null);
    const [tradeSuccess, setTradeSuccess] = useState(null);

    const isAuthenticated = () => {
        return !!localStorage.getItem('accessToken');
    };

    const getAuthHeaders = () => {
        const token = localStorage.getItem('accessToken');
        return {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        };
    };

    const fetchStockData = async () => {
        try {
            setRefreshing(true);
            const response = await fetch(`/api/market/stocks/${symbol}`);
            if (response.ok) {
                const data = await response.json();
                if (data) {
                    setStockData({
                        symbol: data.symbol,
                        price: data.lastPrice,
                        change: data.pChange,
                        changeValue: data.change,
                        open: data.open,
                        high: data.dayHigh,
                        low: data.dayLow,
                        previousClose: data.previousClose,
                        totalTradedVolume: data.totalTradedVolume,
                        totalTradedValue: data.totalTradedValue,
                        marketCap: data.ffmc,
                        yearHigh: data.yearHigh,
                        yearLow: data.yearLow,
                        lastUpdateTime: new Date().toLocaleString()
                    });
                    setError(null);
                } else {
                    setError('Stock data not found');
                }
            } else {
                setError('Failed to fetch stock data');
            }
        } catch (err) {
            console.error('Error fetching stock data:', err);
            setError('Error loading stock data');
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    useEffect(() => {
        if (symbol) {
            fetchStockData();
        }
    }, [symbol]);

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

    const formatMarketCap = (valueInLakhs) => {
        if (valueInLakhs == null || isNaN(valueInLakhs)) return '-';

        // Convert Lakhs to Crores (1 Crore = 100 Lakhs)
        const valueInCrores = Math.abs(Number(valueInLakhs)) / 100;

        // Format in Indian number system (Cr.)
        return `₹${new Intl.NumberFormat('en-IN', {
            maximumFractionDigits: 2,
            minimumFractionDigits: 2
        }).format(valueInCrores)} Cr.`;
    };

    const formatVolume = (value) => {
        if (value == null) return '-';
        const absValue = Math.abs(Number(value));

        if (absValue >= 10000000) {
            return `${(absValue / 10000000).toFixed(2)} Cr`;
        } else if (absValue >= 100000) {
            return `${(absValue / 100000).toFixed(2)} L`;
        } else if (absValue >= 1000) {
            return `${(absValue / 1000).toFixed(2)} K`;
        }
        return formatNumber(value);
    };

    const handleBack = () => {
        // Check if there's a previous page in history, otherwise go to markets
        if (window.history.length > 2) {
            navigate(-1);
        } else {
            navigate('/markets');
        }
    };

    const handleRefresh = () => {
        fetchStockData();
    };

    const openTradeModal = (type) => {
        if (!isAuthenticated()) {
            navigate('/login');
            return;
        }
        setTradeType(type);
        setQuantity('');
        setTradeError(null);
        setTradeSuccess(null);
        setShowTradeModal(true);
    };

    const closeTradeModal = () => {
        setShowTradeModal(false);
        setQuantity('');
        setTradeError(null);
        setTradeSuccess(null);
    };

    const handleTrade = async (e) => {
        e.preventDefault();
        setTrading(true);
        setTradeError(null);
        setTradeSuccess(null);

        const qty = parseFloat(quantity);
        if (isNaN(qty) || qty <= 0) {
            setTradeError('Please enter a valid quantity');
            setTrading(false);
            return;
        }

        try {
            const endpoint = tradeType === 'buy' ? '/api/portfolios/buy' : '/api/portfolios/sell';
            const body = tradeType === 'buy'
                ? {
                    symbol: stockData.symbol,
                    quantity: qty,
                    price: stockData.price,
                    companyName: stockData.symbol
                }
                : {
                    symbol: stockData.symbol,
                    quantity: qty
                };

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify(body)
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setTradeSuccess(data.message);
                setTimeout(() => {
                    closeTradeModal();
                }, 2000);
            } else {
                setTradeError(data.message || `${tradeType === 'buy' ? 'Buy' : 'Sell'} order failed`);
            }
        } catch (err) {
            console.error('Trade error:', err);
            setTradeError('Failed to execute trade. Please try again.');
        } finally {
            setTrading(false);
        }
    };

    const totalValue = quantity && stockData?.price
        ? (parseFloat(quantity) * stockData.price).toFixed(2)
        : 0;

    if (loading) {
        return (
            <div className="page-wrapper">
                <Navbar onLogin={() => navigate('/login')} />
                <div className="stock-details-container">
                    <div className="loading-state">
                        <div className="loading-spinner"></div>
                        <p>Loading {symbol} data...</p>
                    </div>
                </div>
                <Footer />
            </div>
        );
    }

    if (error) {
        return (
            <div className="page-wrapper">
                <Navbar onLogin={() => navigate('/login')} />
                <div className="stock-details-container">
                    <div className="error-state">
                        <BarChart3 size={48} />
                        <h3>Unable to load stock data</h3>
                        <p>{error}</p>
                        <button className="retry-btn" onClick={handleRefresh}>
                            <RefreshCw size={18} />
                            <span>Try Again</span>
                        </button>
                        <button className="back-link" onClick={handleBack}>
                            ← Go Back
                        </button>
                    </div>
                </div>
                <Footer />
            </div>
        );
    }

    return (
        <div className="page-wrapper">
            <Navbar onLogin={() => navigate('/login')} />
            <div className="stock-details-container">
                {/* Header Section */}
                <div className="stock-details-header">
                    <button className="back-btn" onClick={handleBack}>
                        <ArrowLeft size={20} />
                        <span>Back</span>
                    </button>
                    <button
                        className={`refresh-btn ${refreshing ? 'spinning' : ''}`}
                        onClick={handleRefresh}
                        disabled={refreshing}
                    >
                        <RefreshCw size={18} />
                    </button>
                </div>

                {/* Stock Title Section */}
                <div className="stock-title-section glass">
                    <h1 className="stock-symbol">{stockData?.symbol}</h1>
                    <div className="stock-price-display">
                        <span className="current-price">{formatCurrency(stockData?.price)}</span>
                        <span className={`price-change ${stockData?.change >= 0 ? 'positive' : 'negative'}`}>
                            {stockData?.change >= 0 ? <TrendingUp size={20} /> : <TrendingDown size={20} />}
                            {stockData?.change >= 0 ? '+' : ''}{stockData?.change?.toFixed(2)}%
                            <span className="change-value">
                                ({stockData?.changeValue >= 0 ? '+' : ''}{stockData?.changeValue?.toFixed(2)})
                            </span>
                        </span>
                    </div>

                    {/* Buy/Sell Buttons */}
                    <div className="trade-buttons">
                        <button
                            className="trade-btn buy-btn"
                            onClick={() => openTradeModal('buy')}
                        >
                            <ShoppingCart size={18} />
                            <span>Buy</span>
                        </button>
                        <button
                            className="trade-btn sell-btn"
                            onClick={() => openTradeModal('sell')}
                        >
                            <TrendingDown size={18} />
                            <span>Sell</span>
                        </button>
                    </div>
                </div>

                {/* TradingView Chart Section */}
                <div className="chart-section glass">
                    <TradingViewChart symbol={stockData?.symbol} />
                </div>

                {/* Intraday Section */}
                <div className="data-section">
                    <div className="section-header">
                        <Activity size={20} />
                        <h2>Intraday</h2>
                    </div>
                    <div className="data-grid glass">
                        <div className="data-item">
                            <span className="data-label">Open</span>
                            <span className="data-value">{formatCurrency(stockData?.open)}</span>
                        </div>
                        <div className="data-item">
                            <span className="data-label">Day High</span>
                            <span className="data-value highlight-high">{formatCurrency(stockData?.high)}</span>
                        </div>
                        <div className="data-item">
                            <span className="data-label">Day Low</span>
                            <span className="data-value highlight-low">{formatCurrency(stockData?.low)}</span>
                        </div>
                        <div className="data-item">
                            <span className="data-label">Volume</span>
                            <span className="data-value">{formatVolume(stockData?.totalTradedVolume)}</span>
                        </div>
                        <div className="data-item">
                            <span className="data-label">Traded Value</span>
                            <span className="data-value">{formatMarketCap(stockData?.totalTradedValue / 10000000)}</span>
                        </div>
                        <div className="data-item">
                            <span className="data-label">Previous Close</span>
                            <span className="data-value">{formatCurrency(stockData?.previousClose)}</span>
                        </div>
                    </div>
                </div>

                {/* Fundamentals Section */}
                <div className="data-section">
                    <div className="section-header">
                        <DollarSign size={20} />
                        <h2>Fundamentals</h2>
                    </div>
                    <div className="data-grid glass">
                        {stockData?.marketCap && (
                            <div className="data-item wide">
                                <span className="data-label">Market Cap</span>
                                <span className="data-value">{formatMarketCap(stockData?.marketCap)}</span>
                            </div>
                        )}
                        {stockData?.yearHigh && (
                            <div className="data-item">
                                <span className="data-label">52 Week High</span>
                                <span className="data-value highlight-high">{formatCurrency(stockData?.yearHigh)}</span>
                            </div>
                        )}
                        {stockData?.yearLow && (
                            <div className="data-item">
                                <span className="data-label">52 Week Low</span>
                                <span className="data-value highlight-low">{formatCurrency(stockData?.yearLow)}</span>
                            </div>
                        )}
                        {!stockData?.marketCap && !stockData?.yearHigh && !stockData?.yearLow && (
                            <div className="data-item wide">
                                <span className="data-label">Fundamental data not available</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Other Data Section */}
                <div className="data-section">
                    <div className="section-header">
                        <BarChart3 size={20} />
                        <h2>Additional Info</h2>
                    </div>
                    <div className="data-grid glass">
                        <div className="data-item">
                            <span className="data-label">Exchange</span>
                            <span className="data-value">NSE</span>
                        </div>
                        <div className="data-item wide">
                            <span className="data-label">Last Updated</span>
                            <span className="data-value">{stockData?.lastUpdateTime}</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Trade Modal */}
            {showTradeModal && (
                <div className="trade-modal-overlay" onClick={closeTradeModal}>
                    <div className="trade-modal glass" onClick={e => e.stopPropagation()}>
                        <div className="trade-modal-header">
                            <h2>{tradeType === 'buy' ? 'Buy' : 'Sell'} {stockData?.symbol}</h2>
                            <button className="close-btn" onClick={closeTradeModal}>
                                <X size={24} />
                            </button>
                        </div>

                        <div className="trade-modal-body">
                            {/* Trade Type Toggle */}
                            <div className="trade-type-toggle">
                                <button
                                    className={`toggle-btn ${tradeType === 'buy' ? 'active buy' : ''}`}
                                    onClick={() => setTradeType('buy')}
                                >
                                    Buy
                                </button>
                                <button
                                    className={`toggle-btn ${tradeType === 'sell' ? 'active sell' : ''}`}
                                    onClick={() => setTradeType('sell')}
                                >
                                    Sell
                                </button>
                            </div>

                            {/* Current Price Display */}
                            <div className="price-display">
                                <span className="label">Current Price</span>
                                <span className="price">{formatCurrency(stockData?.price)}</span>
                            </div>

                            {/* Trade Form */}
                            <form onSubmit={handleTrade}>
                                <div className="form-group">
                                    <label>Quantity</label>
                                    <input
                                        type="number"
                                        min="1"
                                        step="1"
                                        placeholder="Enter quantity"
                                        value={quantity}
                                        onChange={(e) => setQuantity(e.target.value)}
                                        required
                                        autoFocus
                                    />
                                </div>

                                {/* Total Value */}
                                {quantity && parseFloat(quantity) > 0 && (
                                    <div className="total-display">
                                        <span className="label">
                                            {tradeType === 'buy' ? 'Total Investment' : 'Expected Value'}
                                        </span>
                                        <span className="total">{formatCurrency(totalValue)}</span>
                                    </div>
                                )}

                                {/* Error Message */}
                                {tradeError && (
                                    <div className="trade-message error">
                                        <AlertCircle size={18} />
                                        <span>{tradeError}</span>
                                    </div>
                                )}

                                {/* Success Message */}
                                {tradeSuccess && (
                                    <div className="trade-message success">
                                        <CheckCircle size={18} />
                                        <span>{tradeSuccess}</span>
                                    </div>
                                )}

                                {/* Submit Button */}
                                <button
                                    type="submit"
                                    className={`submit-btn ${tradeType}`}
                                    disabled={trading || !quantity}
                                >
                                    {trading
                                        ? 'Processing...'
                                        : `${tradeType === 'buy' ? 'Buy' : 'Sell'} ${stockData?.symbol}`
                                    }
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
};

export default StockDetails;
