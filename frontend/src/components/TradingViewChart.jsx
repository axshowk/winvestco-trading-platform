import React, { useEffect, useRef, useState } from 'react';
import { Maximize2, Minimize2, X } from 'lucide-react';
import './TradingViewChart.css';

const TradingViewChart = ({ symbol }) => {
    const containerRef = useRef(null);
    const widgetRef = useRef(null);
    const [isFullscreen, setIsFullscreen] = useState(false);

    useEffect(() => {
        if (!symbol || !containerRef.current) return;

        // Map Indian stock symbols to TradingView format
        // Most NSE stocks are available with NSE: prefix
        const tradingViewSymbol = `NSE:${symbol}`;

        // Clear previous widget if it exists
        if (containerRef.current) {
            containerRef.current.innerHTML = '';
        }

        // Create new TradingView widget
        try {
            widgetRef.current = new window.TradingView.widget({
                autosize: true,
                symbol: tradingViewSymbol,
                interval: 'D', // Daily interval
                timezone: 'Asia/Kolkata',
                theme: 'dark',
                style: '1', // Candle style
                locale: 'en',
                toolbar_bg: '#0a0e27',
                enable_publishing: false,
                hide_side_toolbar: false,
                allow_symbol_change: false,
                container_id: containerRef.current.id,
                studies: [
                    'MASimple@tv-basicstudies' // Moving Average
                ],
                disabled_features: [
                    'use_localstorage_for_settings',
                    'header_symbol_search',
                    'symbol_search_hot_key'
                ],
                enabled_features: [
                    'hide_left_toolbar_by_default'
                ],
                loading_screen: {
                    backgroundColor: '#0a0e27',
                    foregroundColor: '#6366f1'
                },
                overrides: {
                    'mainSeriesProperties.candleStyle.upColor': '#10b981',
                    'mainSeriesProperties.candleStyle.downColor': '#ef4444',
                    'mainSeriesProperties.candleStyle.borderUpColor': '#10b981',
                    'mainSeriesProperties.candleStyle.borderDownColor': '#ef4444',
                    'mainSeriesProperties.candleStyle.wickUpColor': '#10b981',
                    'mainSeriesProperties.candleStyle.wickDownColor': '#ef4444',
                    'paneProperties.background': '#0a0e27',
                    'paneProperties.backgroundType': 'solid',
                }
            });
        } catch (error) {
            console.error('Error initializing TradingView widget:', error);
        }

        // Cleanup function - run async to not block modal close
        return () => {
            // Use setTimeout to defer cleanup and allow instant modal close
            const widget = widgetRef.current;
            setTimeout(() => {
                if (widget && widget.remove) {
                    try {
                        widget.remove();
                    } catch (e) {
                        // Ignore cleanup errors
                    }
                }
            }, 0);
            widgetRef.current = null;
        };
    }, [symbol]);

    // Handle ESC key to exit fullscreen
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

    // Ensure body overflow is reset when component unmounts
    useEffect(() => {
        return () => {
            document.body.style.overflow = '';
        };
    }, []);

    const toggleFullscreen = (e) => {
        e.stopPropagation();
        setIsFullscreen(!isFullscreen);
    };

    return (
        <div className={`chart-wrapper ${isFullscreen ? 'fullscreen' : ''}`}>
            <button
                className="fullscreen-toggle-btn"
                onClick={toggleFullscreen}
                title={isFullscreen ? 'Exit Fullscreen (ESC)' : 'Fullscreen'}
            >
                {isFullscreen ? <X size={20} /> : <Maximize2 size={20} />}
            </button>
            {isFullscreen && (
                <div className="fullscreen-header">
                    <span className="fullscreen-symbol">{symbol}</span>
                    <span className="fullscreen-hint">Press ESC to exit</span>
                </div>
            )}
            <div
                id={`tradingview_${symbol}`}
                ref={containerRef}
                className="tradingview-chart-container"
                style={{ height: isFullscreen ? 'calc(100vh - 60px)' : '500px', width: '100%' }}
            />
        </div>
    );
};

export default TradingViewChart;
