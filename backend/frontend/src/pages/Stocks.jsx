import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, X, Search, ArrowLeft, BarChart3 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import TradingViewChart from '../components/TradingViewChart';
import './Stocks.css';

// Available indices - All major NSE indices
const INDICES = [
    // Broad Market Indices
    { name: 'NIFTY 50', description: 'Top 50 companies by market cap' },
    { name: 'NIFTY NEXT 50', description: 'Next 50 large cap companies' },
    { name: 'NIFTY 100', description: 'Top 100 companies by market cap' },
    { name: 'NIFTY 200', description: 'Top 200 companies by market cap' },
    { name: 'NIFTY 500', description: 'Top 500 companies by market cap' },
    { name: 'NIFTY MIDCAP 50', description: 'Top 50 midcap companies' },
    { name: 'NIFTY MIDCAP 100', description: 'Top 100 midcap companies' },
    { name: 'NIFTY SMLCAP 100', description: 'Top 100 smallcap companies' },
    // Sectoral Indices
    { name: 'NIFTY BANK', description: 'Banking sector stocks' },
    { name: 'NIFTY IT', description: 'Information Technology stocks' },
    { name: 'NIFTY AUTO', description: 'Automobile sector stocks' },
    { name: 'NIFTY FINANCIAL SERVICES', description: 'Financial services sector' },
    { name: 'NIFTY FMCG', description: 'Fast moving consumer goods' },
    { name: 'NIFTY PHARMA', description: 'Pharmaceutical stocks' },
    { name: 'NIFTY METAL', description: 'Metal & Mining stocks' },
    { name: 'NIFTY MEDIA', description: 'Media & entertainment stocks' },
    { name: 'NIFTY ENERGY', description: 'Energy sector stocks' },
    { name: 'NIFTY PSU BANK', description: 'Public sector banks' },
    { name: 'NIFTY PRIVATE BANK', description: 'Private sector banks' },
    { name: 'NIFTY INFRA', description: 'Infrastructure stocks' },
    { name: 'NIFTY REALTY', description: 'Real estate stocks' },
    { name: 'NIFTY CONSUMPTION', description: 'Consumer durables stocks' }
];

const Stocks = () => {
    const navigate = useNavigate();
    const [stocks, setStocks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(null);
    const [selectedStock, setSelectedStock] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [indexSummary, setIndexSummary] = useState(null);
    const [indicesPerformance, setIndicesPerformance] = useState({});
    const [loadingIndices, setLoadingIndices] = useState(true);

    // Fetch performance data for all indices on page load
    useEffect(() => {
        const fetchIndicesPerformance = async () => {
            setLoadingIndices(true);
            const performance = {};

            for (const index of INDICES) {
                try {
                    const response = await fetch(`/api/market/indices/${index.name}`);
                    if (response.ok) {
                        const data = await response.json();
                        if (data.data && Array.isArray(data.data)) {
                            const summaryItem = data.data.find(item => item.symbol === index.name);
                            if (summaryItem) {
                                performance[index.name] = {
                                    lastPrice: summaryItem.lastPrice,
                                    change: summaryItem.change,
                                    pChange: summaryItem.pChange
                                };
                            }
                        }
                    }
                } catch (error) {
                    console.error(`Error fetching ${index.name} data:`, error);
                }
            }

            setIndicesPerformance(performance);
            setLoadingIndices(false);
        };

        fetchIndicesPerformance();
    }, []);

    // Fetch stocks when an index is selected
    useEffect(() => {
        if (!selectedIndex) {
            setStocks([]);
            setIndexSummary(null);
            return;
        }

        const fetchStocks = async () => {
            setLoading(true);
            try {
                const response = await fetch(`/api/market/indices/${selectedIndex}`);
                if (response.ok) {
                    const data = await response.json();
                    if (data.data && Array.isArray(data.data)) {
                        const fetchTime = new Date().toLocaleString();

                        // First item is usually the index summary
                        const summaryItem = data.data.find(item => item.symbol === selectedIndex);
                        if (summaryItem) {
                            setIndexSummary({
                                lastPrice: summaryItem.lastPrice,
                                change: summaryItem.change,
                                pChange: summaryItem.pChange
                            });
                        }

                        const parsedStocks = data.data
                            .filter(item => item.symbol && item.symbol !== selectedIndex)
                            .map(item => ({
                                symbol: item.symbol,
                                price: item.lastPrice,
                                change: item.pChange,
                                changeValue: item.change,
                                open: item.open,
                                high: item.dayHigh,
                                low: item.dayLow,
                                previousClose: item.previousClose,
                                totalTradedVolume: item.totalTradedVolume,
                                totalTradedValue: item.totalTradedValue,
                                marketCap: item.ffmc,
                                lastUpdateTime: fetchTime
                            }));
                        setStocks(parsedStocks);
                    }
                }
            } catch (error) {
                console.error('Error fetching stocks:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchStocks();
    }, [selectedIndex]);

    const filteredStocks = stocks.filter(stock =>
        stock.symbol.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const formatCurrency = (value) => {
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            maximumFractionDigits: 2
        }).format(value);
    };

    const formatNumber = (value) => {
        return new Intl.NumberFormat('en-IN', {
            maximumFractionDigits: 2
        }).format(value);
    };

    // Format market cap in Indian words
    // NSE ffmc value is in Crores
    const formatMarketCap = (valueInCrores) => {
        if (valueInCrores == null || isNaN(valueInCrores)) return '-';

        const absValue = Math.abs(Number(valueInCrores));

        if (absValue >= 100000) {
            // Lakh Crore (1,00,000+ Cr)
            return `₹${(absValue / 100000).toFixed(2)} Lakh Crore`;
        } else if (absValue >= 1000) {
            // Thousand Crore
            return `₹${(absValue / 1000).toFixed(2)} Thousand Crore`;
        } else {
            return `₹${absValue.toFixed(2)} Crore`;
        }
    };

    const handleBackToIndices = () => {
        setSelectedIndex(null);
        setSearchTerm('');
    };

    return (
        <div className="page-wrapper">
            <Navbar onLogin={() => navigate('/login')} />
            <div className="stocks-container">
                {!selectedIndex ? (
                    // Show Indices
                    <>
                        <div className="stocks-header">
                            <h1>Market Indices</h1>
                            <p className="header-subtitle">Select an index to view its stocks</p>
                        </div>
                        {loadingIndices ? (
                            <div className="loading-state">Loading indices...</div>
                        ) : (
                            <div className="indices-grid">
                                {INDICES.map((index) => {
                                    const perf = indicesPerformance[index.name];
                                    return (
                                        <div
                                            key={index.name}
                                            className="index-card glass"
                                            onClick={() => setSelectedIndex(index.name)}
                                        >

                                            <h3>{index.name}</h3>

                                            {/* Today's Performance */}
                                            {perf && (
                                                <div className="index-performance">
                                                    <span className="index-current-value">
                                                        {formatNumber(perf.lastPrice)}
                                                    </span>
                                                    <span className={`index-perf-change ${perf.pChange >= 0 ? 'positive' : 'negative'}`}>
                                                        {perf.pChange >= 0 ? <TrendingUp size={16} /> : <TrendingDown size={16} />}
                                                        {perf.pChange >= 0 ? '+' : ''}{perf.pChange?.toFixed(2)}%
                                                    </span>
                                                </div>
                                            )}

                                            <p className="index-description">{index.description}</p>
                                            <div className="index-arrow">
                                                <BarChart3 size={20} />
                                                <span>View Stocks</span>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </>
                ) : (
                    // Show Stocks of Selected Index
                    <>
                        <div className="stocks-header">
                            <div className="header-with-back">
                                <button className="back-btn" onClick={handleBackToIndices}>
                                    <ArrowLeft size={20} />
                                    <span>Back</span>
                                </button>
                                <div className="header-title-section">
                                    <h1>{selectedIndex}</h1>
                                    {indexSummary && (
                                        <div className="index-summary">
                                            <span className="index-value">{formatNumber(indexSummary.lastPrice)}</span>
                                            <span className={`index-change ${indexSummary.pChange >= 0 ? 'positive' : 'negative'}`}>
                                                {indexSummary.pChange >= 0 ? '+' : ''}{indexSummary.pChange?.toFixed(2)}%
                                            </span>
                                        </div>
                                    )}
                                </div>
                            </div>
                            <div className="search-bar">
                                <Search size={20} className="search-icon" />
                                <input
                                    type="text"
                                    placeholder={`Search ${selectedIndex} stocks...`}
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>
                        </div>

                        {loading ? (
                            <div className="loading-state">Loading {selectedIndex} stocks...</div>
                        ) : (
                            <div className="stocks-grid">
                                {filteredStocks.map((stock) => (
                                    <div
                                        key={stock.symbol}
                                        className="stock-card glass"
                                        onClick={() => setSelectedStock(stock)}
                                    >
                                        <div className="stock-card-header">
                                            <h3>{stock.symbol}</h3>
                                            <span className={`change-badge ${stock.change >= 0 ? 'positive' : 'negative'}`}>
                                                {stock.change >= 0 ? <TrendingUp size={16} /> : <TrendingDown size={16} />}
                                                {Math.abs(stock.change).toFixed(2)}%
                                            </span>
                                        </div>
                                        <div className="stock-price">
                                            {formatCurrency(stock.price)}
                                        </div>
                                        <div className="stock-volume">
                                            Vol: {formatNumber(stock.totalTradedVolume)}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </>
                )}

                {/* Stock Details Modal */}
                {selectedStock && (
                    <div className="modal-overlay" onClick={() => setSelectedStock(null)}>
                        <div className="modal-content glass" onClick={e => e.stopPropagation()}>
                            <button className="close-btn" onClick={() => setSelectedStock(null)}>
                                <X size={24} />
                            </button>

                            <div className="modal-header">
                                <h2>{selectedStock.symbol}</h2>
                                <div className="modal-price-info">
                                    <span className="current-price">{formatCurrency(selectedStock.price)}</span>
                                    <span className={`price-change ${selectedStock.change >= 0 ? 'positive' : 'negative'}`}>
                                        {selectedStock.change >= 0 ? '+' : ''}{selectedStock.change.toFixed(2)}%
                                        ({selectedStock.changeValue.toFixed(2)})
                                    </span>
                                </div>
                            </div>

                            {/* TradingView Chart */}
                            <div className="chart-section">
                                <TradingViewChart symbol={selectedStock.symbol} />
                            </div>

                            <div className="modal-grid">
                                <div className="detail-item">
                                    <span className="label">Open</span>
                                    <span className="value">{formatCurrency(selectedStock.open)}</span>
                                </div>
                                <div className="detail-item">
                                    <span className="label">Previous Close</span>
                                    <span className="value">{formatCurrency(selectedStock.previousClose)}</span>
                                </div>
                                <div className="detail-item">
                                    <span className="label">Day High</span>
                                    <span className="value">{formatCurrency(selectedStock.high)}</span>
                                </div>
                                <div className="detail-item">
                                    <span className="label">Day Low</span>
                                    <span className="value">{formatCurrency(selectedStock.low)}</span>
                                </div>
                                <div className="detail-item">
                                    <span className="label">Volume</span>
                                    <span className="value">{formatNumber(selectedStock.totalTradedVolume)}</span>
                                </div>
                                <div className="detail-item">
                                    <span className="label">Value (Lakhs)</span>
                                    <span className="value">{formatNumber(selectedStock.totalTradedValue / 100000)}</span>
                                </div>
                            </div>

                            {/* Market Cap Display */}
                            {selectedStock.marketCap && (
                                <div className="market-cap-display">
                                    <span className="market-cap-label">Market Cap: </span>
                                    <span className="market-cap-value">
                                        ₹{formatNumber(selectedStock.marketCap)} Cr
                                    </span>
                                </div>
                            )}

                            <div className="last-updated">
                                Last Updated: {selectedStock.lastUpdateTime}
                            </div>
                        </div>
                    </div>
                )}
            </div>
            <Footer />
        </div>
    );
};

export default Stocks;
