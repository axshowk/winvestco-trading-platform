import React, { useState, useEffect, useMemo } from 'react';
import { TrendingUp, TrendingDown, Search, ArrowUpDown, BarChart3 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import './MarketData.css';

const SORT_OPTIONS = [
    { value: 'name-asc', label: 'Name (A-Z)' },
    { value: 'name-desc', label: 'Name (Z-A)' },
    { value: 'price-desc', label: 'Price (High-Low)' },
    { value: 'price-asc', label: 'Price (Low-High)' },
    { value: 'change-desc', label: 'Top Gainers' },
    { value: 'change-asc', label: 'Top Losers' },
];

const MarketData = () => {
    const navigate = useNavigate();
    const [stocks, setStocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [sortOption, setSortOption] = useState('name-asc');
    const [totalCount, setTotalCount] = useState(0);

    // Fetch all stocks on page load
    useEffect(() => {
        const fetchAllStocks = async () => {
            setLoading(true);
            try {
                const response = await fetch('/api/v1/market/stocks/all');
                if (response.ok) {
                    const data = await response.json();
                    if (data.data && Array.isArray(data.data)) {
                        const fetchTime = new Date().toLocaleString();
                        const parsedStocks = data.data.map(item => ({
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
                        setTotalCount(data.totalCount || parsedStocks.length);
                    }
                }
            } catch (error) {
                console.error('Error fetching stocks:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchAllStocks();
    }, []);

    // Filter and sort stocks
    const filteredAndSortedStocks = useMemo(() => {
        let result = stocks.filter(stock =>
            stock.symbol.toLowerCase().includes(searchTerm.toLowerCase())
        );

        // Sort based on selected option
        const [sortBy, sortOrder] = sortOption.split('-');
        result.sort((a, b) => {
            let comparison = 0;
            if (sortBy === 'name') {
                comparison = a.symbol.localeCompare(b.symbol);
            } else if (sortBy === 'price') {
                comparison = (a.price || 0) - (b.price || 0);
            } else if (sortBy === 'change') {
                comparison = (a.change || 0) - (b.change || 0);
            }
            return sortOrder === 'desc' ? -comparison : comparison;
        });

        return result;
    }, [stocks, searchTerm, sortOption]);

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

    // Format market cap in Indian words (Lakh Crore, Thousand Crore, Crore)
    // Input value is in Crores from NSE API
    const formatMarketCap = (valueInCrores) => {
        if (valueInCrores == null || isNaN(valueInCrores)) return '-';

        const absValue = Math.abs(Number(valueInCrores));

        if (absValue >= 100000) {
            // Lakh Crore (1,00,000+ Cr = 1 Lakh Crore)
            const lakhCrore = absValue / 100000;
            return `₹${lakhCrore.toFixed(2)} Lakh Crore`;
        } else if (absValue >= 1000) {
            // Thousand Crore (1,000+ Cr)
            const thousandCrore = absValue / 1000;
            return `₹${thousandCrore.toFixed(2)} Thousand Crore`;
        } else if (absValue >= 1) {
            // Crore
            return `₹${absValue.toFixed(2)} Crore`;
        } else {
            // Less than 1 Crore - show in Lakhs
            const lakhs = absValue * 100;
            return `₹${lakhs.toFixed(2)} Lakh`;
        }
    };

    return (
        <div className="page-wrapper">
            <Navbar onLogin={() => navigate('/login')} />
            <div className="market-data-container">
                <div className="market-data-header">
                    <div className="header-title-area">
                        <h1>Market Data</h1>
                        <p className="header-subtitle">
                            All NSE Stocks • {totalCount} stocks available
                        </p>
                    </div>

                    <div className="controls-row">
                        <div className="search-bar">
                            <Search size={20} className="search-icon" />
                            <input
                                type="text"
                                placeholder="Search stocks by symbol..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>

                        <div className="sort-control">
                            <ArrowUpDown size={18} className="sort-icon" />
                            <select
                                value={sortOption}
                                onChange={(e) => setSortOption(e.target.value)}
                            >
                                {SORT_OPTIONS.map(option => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="results-info">
                        Showing {filteredAndSortedStocks.length} of {totalCount} stocks
                    </div>
                </div>

                {loading ? (
                    <div className="loading-state">
                        <div className="loading-spinner"></div>
                        <p>Loading market data...</p>
                    </div>
                ) : (
                    <div className="stocks-grid">
                        {filteredAndSortedStocks.map((stock) => (
                            <div
                                key={stock.symbol}
                                className="stock-card glass"
                                onClick={() => navigate(`/stock/${stock.symbol}`)}
                            >
                                <div className="stock-card-header">
                                    <h3>{stock.symbol}</h3>
                                    <span className={`change-badge ${stock.change >= 0 ? 'positive' : 'negative'}`}>
                                        {stock.change >= 0 ? <TrendingUp size={16} /> : <TrendingDown size={16} />}
                                        {Math.abs(stock.change || 0).toFixed(2)}%
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

                {!loading && filteredAndSortedStocks.length === 0 && (
                    <div className="empty-state">
                        <BarChart3 size={48} />
                        <h3>No stocks found</h3>
                        <p>Try adjusting your search term</p>
                    </div>
                )}
            </div>
            <Footer />
        </div>
    );
};

export default MarketData;
