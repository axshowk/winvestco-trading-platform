import React, { useEffect, useRef } from 'react';
import { createChart, ColorType } from 'lightweight-charts';
import { useTheme } from '../context/ThemeContext';

/**
 * LightweightChart Component
 * A high-performance financial chart using TradingView's lightweight-charts library.
 * 
 * @param {Array} data - Array of candle objects { time, open, high, low, close, volume }
 * @param {boolean} autoSize - Whether the chart should resize automatically
 * @param {string} height - Height of the chart (default: 400px)
 */
const LightweightChart = ({ data = [], autoSize = true, height = '400px' }) => {
    const { isDarkMode } = useTheme();
    const chartContainerRef = useRef();
    const chartRef = useRef();
    const seriesRef = useRef();

    useEffect(() => {
        if (!chartContainerRef.current) return;

        const handleResize = () => {
            if (autoSize && chartRef.current) {
                chartRef.current.applyOptions({
                    width: chartContainerRef.current.clientWidth,
                });
            }
        };

        // Create Chart
        chartRef.current = createChart(chartContainerRef.current, {
            layout: {
                background: {
                    type: ColorType.Solid,
                    color: isDarkMode ? '#0a0e27' : '#ffffff'
                },
                textColor: isDarkMode ? '#94a3b8' : '#334155',
            },
            grid: {
                vertLines: { color: isDarkMode ? 'rgba(71, 85, 105, 0.1)' : 'rgba(0, 0, 0, 0.05)' },
                horzLines: { color: isDarkMode ? 'rgba(71, 85, 105, 0.1)' : 'rgba(0, 0, 0, 0.05)' },
            },
            width: chartContainerRef.current.clientWidth,
            height: parseInt(height),
            timeScale: {
                borderColor: isDarkMode ? 'rgba(71, 85, 105, 0.3)' : 'rgba(0, 0, 0, 0.1)',
                timeVisible: true,
                secondsVisible: false,
            },
            rightPriceScale: {
                borderColor: isDarkMode ? 'rgba(71, 85, 105, 0.3)' : 'rgba(0, 0, 0, 0.1)',
            },
            crosshair: {
                mode: 0, // Normal
                vertLine: {
                    color: '#6366f1',
                    width: 1,
                    style: 3, // Dotted
                    labelBackgroundColor: '#6366f1',
                },
                horzLine: {
                    color: '#6366f1',
                    width: 1,
                    style: 3, // Dotted
                    labelBackgroundColor: '#6366f1',
                },
            },
        });

        // Add Candlestick Series
        seriesRef.current = chartRef.current.addCandlestickSeries({
            upColor: '#10b981',
            downColor: '#ef4444',
            borderVisible: false,
            wickUpColor: '#10b981',
            wickDownColor: '#ef4444',
        });

        // Set Data
        if (data && data.length > 0) {
            seriesRef.current.setData(data);
            chartRef.current.timeScale().fitContent();
        }

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            if (chartRef.current) {
                chartRef.current.remove();
            }
        };
    }, []); // Run once on mount

    // Update chart colors when theme changes
    useEffect(() => {
        if (chartRef.current) {
            chartRef.current.applyOptions({
                layout: {
                    background: {
                        type: ColorType.Solid,
                        color: isDarkMode ? '#0a0e27' : '#ffffff'
                    },
                    textColor: isDarkMode ? '#94a3b8' : '#334155',
                },
                grid: {
                    vertLines: { color: isDarkMode ? 'rgba(71, 85, 105, 0.1)' : 'rgba(0, 0, 0, 0.05)' },
                    horzLines: { color: isDarkMode ? 'rgba(71, 85, 105, 0.1)' : 'rgba(0, 0, 0, 0.05)' },
                },
                timeScale: {
                    borderColor: isDarkMode ? 'rgba(71, 85, 105, 0.3)' : 'rgba(0, 0, 0, 0.1)',
                },
                rightPriceScale: {
                    borderColor: isDarkMode ? 'rgba(71, 85, 105, 0.3)' : 'rgba(0, 0, 0, 0.1)',
                },
            });
        }
    }, [isDarkMode]);

    // Update data when it changes
    useEffect(() => {
        if (seriesRef.current && data && data.length > 0) {
            seriesRef.current.setData(data);
        }
    }, [data]);

    return (
        <div
            ref={chartContainerRef}
            className="lw-chart-container"
            style={{ width: '100%', height: height, position: 'relative' }}
        />
    );
};

export default LightweightChart;
