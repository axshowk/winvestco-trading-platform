import React, { useEffect, useRef, useState, useCallback } from 'react';
import { createChart, CrosshairMode, LineStyle } from 'lightweight-charts';
import { Minus, TrendingUp, Trash2, Maximize2, X } from 'lucide-react';
import './LightweightChart.css';

const INTERVALS = [
    { value: '5m', label: '5M' },
    { value: '15m', label: '15M' },
    { value: '1h', label: '1H' },
    { value: '1d', label: '1D' },
];

const LightweightChart = ({ symbol }) => {
    const chartContainerRef = useRef(null);
    const chartRef = useRef(null);
    const candleSeriesRef = useRef(null);
    const volumeSeriesRef = useRef(null);
    
    const [interval, setInterval] = useState('5m');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isFullscreen, setIsFullscreen] = useState(false);
    
    // Drawing state
    const [drawingTool, setDrawingTool] = useState(null); // 'horizontal' | 'trendline' | null
    const [drawings, setDrawings] = useState([]);
    const [isDrawing, setIsDrawing] = useState(false);
    const [drawStart, setDrawStart] = useState(null);
    const priceLineRefs = useRef([]);

    // Fetch candle data from API
    const fetchCandles = useCallback(async () => {
        if (!symbol) return;
        
        setLoading(true);
        setError(null);
        
        try {
            const response = await fetch(
                `/api/market/candles?symbol=${symbol}&interval=${interval}&limit=500`
            );
            
            if (!response.ok) {
                // If no candle data yet, show message
                if (response.status === 404) {
                    setError('No historical data available yet. Data will accumulate over time.');
                    return [];
                }
                throw new Error('Failed to fetch candle data');
            }
            
            const data = await response.json();
            return data;
        } catch (err) {
            console.error('Error fetching candles:', err);
            setError('Unable to load chart data');
            return [];
        } finally {
            setLoading(false);
        }
    }, [symbol, interval]);

    // Initialize chart
    useEffect(() => {
        if (!chartContainerRef.current) return;

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: { type: 'solid', color: '#0a0e27' },
                textColor: '#d1d5db',
            },
            grid: {
                vertLines: { color: 'rgba(42, 46, 57, 0.6)' },
                horzLines: { color: 'rgba(42, 46, 57, 0.6)' },
            },
            crosshair: {
                mode: CrosshairMode.Normal,
                vertLine: {
                    color: 'rgba(99, 102, 241, 0.5)',
                    labelBackgroundColor: '#6366f1',
                },
                horzLine: {
                    color: 'rgba(99, 102, 241, 0.5)',
                    labelBackgroundColor: '#6366f1',
                },
            },
            rightPriceScale: {
                borderColor: 'rgba(42, 46, 57, 0.8)',
            },
            timeScale: {
                borderColor: 'rgba(42, 46, 57, 0.8)',
                timeVisible: true,
                secondsVisible: false,
            },
            handleScroll: { vertTouchDrag: false },
        });

        // Candlestick series
        const candleSeries = chart.addCandlestickSeries({
            upColor: '#10b981',
            downColor: '#ef4444',
            borderUpColor: '#10b981',
            borderDownColor: '#ef4444',
            wickUpColor: '#10b981',
            wickDownColor: '#ef4444',
        });

        // Volume series
        const volumeSeries = chart.addHistogramSeries({
            color: '#6366f1',
            priceFormat: { type: 'volume' },
            priceScaleId: '',
            scaleMargins: { top: 0.85, bottom: 0 },
        });

        chartRef.current = chart;
        candleSeriesRef.current = candleSeries;
        volumeSeriesRef.current = volumeSeries;

        // Handle resize
        const handleResize = () => {
            if (chartContainerRef.current && chart) {
                chart.applyOptions({
                    width: chartContainerRef.current.clientWidth,
                    height: isFullscreen ? window.innerHeight - 120 : 500,
                });
            }
        };

        window.addEventListener('resize', handleResize);
        handleResize();

        return () => {
            window.removeEventListener('resize', handleResize);
            chart.remove();
        };
    }, [isFullscreen]);

    // Load data when symbol/interval changes
    useEffect(() => {
        const loadData = async () => {
            const candles = await fetchCandles();
            
            if (candles && candles.length > 0 && candleSeriesRef.current) {
                const candleData = candles.map(c => ({
                    time: c.time,
                    open: parseFloat(c.open),
                    high: parseFloat(c.high),
                    low: parseFloat(c.low),
                    close: parseFloat(c.close),
                }));

                const volumeData = candles.map(c => ({
                    time: c.time,
                    value: c.volume,
                    color: parseFloat(c.close) >= parseFloat(c.open) 
                        ? 'rgba(16, 185, 129, 0.5)' 
                        : 'rgba(239, 68, 68, 0.5)',
                }));

                candleSeriesRef.current.setData(candleData);
                volumeSeriesRef.current.setData(volumeData);
                setError(null);
            }
        };

        loadData();
    }, [symbol, interval, fetchCandles]);

    // Chart click handler for drawing
    const handleChartClick = useCallback((e) => {
        if (!drawingTool || !chartRef.current || !candleSeriesRef.current) return;

        const rect = chartContainerRef.current.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        const time = chartRef.current.timeScale().coordinateToTime(x);
        const price = candleSeriesRef.current.coordinateToPrice(y);

        if (drawingTool === 'horizontal') {
            // Add horizontal line immediately
            const priceLine = candleSeriesRef.current.createPriceLine({
                price: price,
                color: '#f59e0b',
                lineWidth: 2,
                lineStyle: LineStyle.Solid,
                axisLabelVisible: true,
                title: `${price.toFixed(2)}`,
            });
            
            priceLineRefs.current.push(priceLine);
            setDrawings(prev => [...prev, { type: 'horizontal', price, priceLine }]);
            setDrawingTool(null);
        } else if (drawingTool === 'trendline') {
            if (!isDrawing) {
                // Start trendline
                setDrawStart({ time, price, x, y });
                setIsDrawing(true);
            } else {
                // End trendline - create visual line using markers
                const startPoint = drawStart;
                const endPoint = { time, price };
                
                // For now, we'll show trendlines as two markers with a note
                // Full trendline drawing would require canvas overlay
                setDrawings(prev => [...prev, { 
                    type: 'trendline', 
                    start: startPoint, 
                    end: endPoint 
                }]);
                
                setIsDrawing(false);
                setDrawStart(null);
                setDrawingTool(null);
            }
        }
    }, [drawingTool, isDrawing, drawStart]);

    // Clear all drawings
    const clearDrawings = () => {
        priceLineRefs.current.forEach(line => {
            try {
                candleSeriesRef.current?.removePriceLine(line);
            } catch (e) { /* ignore */ }
        });
        priceLineRefs.current = [];
        setDrawings([]);
    };

    // Toggle fullscreen
    const toggleFullscreen = () => {
        setIsFullscreen(!isFullscreen);
    };

    // Handle ESC to exit fullscreen
    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key === 'Escape' && isFullscreen) {
                setIsFullscreen(false);
            }
        };

        if (isFullscreen) {
            document.addEventListener('keydown', handleKeyDown);
            document.body.style.overflow = 'hidden';
        } else {
            document.body.style.overflow = '';
        }

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, [isFullscreen]);

    return (
        <div className={`lightweight-chart-wrapper ${isFullscreen ? 'fullscreen' : ''}`}>
            {/* Toolbar */}
            <div className="chart-toolbar">
                {/* Interval buttons */}
                <div className="interval-buttons">
                    {INTERVALS.map(int => (
                        <button
                            key={int.value}
                            className={`interval-btn ${interval === int.value ? 'active' : ''}`}
                            onClick={() => setInterval(int.value)}
                        >
                            {int.label}
                        </button>
                    ))}
                </div>

                {/* Drawing tools */}
                <div className="drawing-tools">
                    <button
                        className={`tool-btn ${drawingTool === 'horizontal' ? 'active' : ''}`}
                        onClick={() => setDrawingTool(drawingTool === 'horizontal' ? null : 'horizontal')}
                        title="Horizontal Line"
                    >
                        <Minus size={18} />
                    </button>
                    <button
                        className={`tool-btn ${drawingTool === 'trendline' ? 'active' : ''}`}
                        onClick={() => setDrawingTool(drawingTool === 'trendline' ? null : 'trendline')}
                        title="Trendline"
                    >
                        <TrendingUp size={18} />
                    </button>
                    {drawings.length > 0 && (
                        <button
                            className="tool-btn clear-btn"
                            onClick={clearDrawings}
                            title="Clear All Drawings"
                        >
                            <Trash2 size={18} />
                        </button>
                    )}
                </div>

                {/* Fullscreen button */}
                <button
                    className="fullscreen-btn"
                    onClick={toggleFullscreen}
                    title={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen'}
                >
                    {isFullscreen ? <X size={18} /> : <Maximize2 size={18} />}
                </button>
            </div>

            {/* Drawing hint */}
            {drawingTool && (
                <div className="drawing-hint">
                    {drawingTool === 'horizontal' && 'Click on chart to place horizontal line'}
                    {drawingTool === 'trendline' && !isDrawing && 'Click to set start point'}
                    {drawingTool === 'trendline' && isDrawing && 'Click to set end point'}
                </div>
            )}

            {/* Chart container */}
            <div 
                ref={chartContainerRef}
                className={`chart-container ${drawingTool ? 'drawing-mode' : ''}`}
                onClick={handleChartClick}
                style={{ height: isFullscreen ? 'calc(100vh - 120px)' : '500px' }}
            />

            {/* Loading overlay */}
            {loading && (
                <div className="chart-loading">
                    <div className="loading-spinner"></div>
                    <p>Loading chart data...</p>
                </div>
            )}

            {/* Error message */}
            {error && !loading && (
                <div className="chart-error">
                    <p>{error}</p>
                    <small>Chart data will be available once market data is collected.</small>
                </div>
            )}

            {/* Fullscreen header */}
            {isFullscreen && (
                <div className="fullscreen-info">
                    <span className="symbol-badge">{symbol}</span>
                    <span className="hint">Press ESC to exit</span>
                </div>
            )}
        </div>
    );
};

export default LightweightChart;
