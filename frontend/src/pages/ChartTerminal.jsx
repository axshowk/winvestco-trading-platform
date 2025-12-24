import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Maximize2, RefreshCw, BarChart2 } from 'lucide-react';
import LightweightChart from '../components/LightweightChart';
import './ChartTerminal.css';

const ChartTerminal = () => {
    const { symbol } = useParams();
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [interval, setInterval] = useState('1d');

    const fetchChartData = async () => {
        setLoading(true);
        try {
            const response = await fetch(`/api/v1/market/candles?symbol=${symbol}&interval=${interval}`);
            if (response.ok) {
                const candleData = await response.json();
                // Ensure data is sorted by time for Lightweight Charts
                const sortedData = [...candleData].sort((a, b) => a.time - b.time);
                setData(sortedData);
                setError(null);
            } else {
                setError('Failed to fetch chart data');
            }
        } catch (err) {
            console.error('Error fetching chart data:', err);
            setError('Error connecting to market service');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (symbol) {
            fetchChartData();
        }
    }, [symbol, interval]);

    const intervals = [
        { value: '5m', label: '5M' },
        { value: '15m', label: '15M' },
        { value: '1h', label: '1H' },
        { value: '1d', label: '1D' },
    ];

    return (
        <div className="terminal-page">
            <header className="terminal-header">
                <div className="header-left">
                    <Link to={`/stock/${symbol}`} className="back-link">
                        <ArrowLeft size={20} />
                    </Link>
                    <div className="symbol-info">
                        <span className="symbol-tag">{symbol}</span>
                        <span className="terminal-label">Advanced Terminal</span>
                    </div>
                </div>

                <div className="header-center">
                    <div className="interval-selector">
                        {intervals.map((int) => (
                            <button
                                key={int.value}
                                className={`interval-btn ${interval === int.value ? 'active' : ''}`}
                                onClick={() => setInterval(int.value)}
                            >
                                {int.label}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="header-right">
                    <button className="refresh-btn" onClick={fetchChartData} title="Refresh Data">
                        <RefreshCw size={18} className={loading ? 'spin' : ''} />
                    </button>
                    <div className="status-indicator">
                        <span className="pulse"></span>
                        Live
                    </div>
                </div>
            </header>

            <main className="terminal-main">
                {loading && data.length === 0 ? (
                    <div className="terminal-loading">
                        <div className="spinner"></div>
                        <p>Loading market data for {symbol}...</p>
                    </div>
                ) : error ? (
                    <div className="terminal-error">
                        <BarChart2 size={48} />
                        <h3>Chart Data Unavailable</h3>
                        <p>{error}</p>
                        <button onClick={fetchChartData} className="retry-btn">Retry</button>
                    </div>
                ) : (
                    <div className="chart-wrapper-full">
                        <LightweightChart data={data} height="calc(100vh - 60px)" />
                    </div>
                )}
            </main>
        </div>
    );
};

export default ChartTerminal;
