import React from 'react';
import './Ticker.css';

const Ticker = () => {
    const [tickers, setTickers] = React.useState([]);

    React.useEffect(() => {
        const fetchAllStocksData = async () => {
            try {
                const response = await fetch('/api/v1/market/stocks/all');
                if (response.ok) {
                    const data = await response.json();
                    console.log('All Stocks Data:', data);

                    if (data.data && Array.isArray(data.data)) {
                        const newTickers = data.data
                            .filter(item => item.symbol && !item.symbol.startsWith('NIFTY')) // Exclude index summaries
                            .map(item => ({
                                symbol: item.symbol,
                                price: item.lastPrice ? item.lastPrice.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00',
                                change: (item.pChange > 0 ? '+' : '') + (item.pChange ? item.pChange.toFixed(2) : '0.00') + '%',
                                up: item.change > 0
                            }));

                        // If we got valid tickers, update state
                        if (newTickers.length > 0) {
                            setTickers(newTickers);
                            console.log(`Loaded ${newTickers.length} stocks for ticker`);
                        }
                    }
                } else {
                    console.error('Failed to fetch stocks data:', response.status);
                }
            } catch (error) {
                console.error('Error fetching all stocks data:', error);
            }
        };

        fetchAllStocksData();
        // Auto-refresh every 30 seconds for real-time updates
        const interval = setInterval(fetchAllStocksData, 30000);
        return () => clearInterval(interval);
    }, []);

    // Fallback/Loading state if no data yet
    if (tickers.length === 0) {
        return (
            <div className="ticker-wrap">
                <div className="ticker-move">
                    <div className="ticker-item">Loading Market Data...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="ticker-wrap">
            <div className="ticker-move">
                {[...tickers, ...tickers].map((item, index) => (
                    <div className="ticker-item" key={index}>
                        <span className="ticker-symbol">{item.symbol}</span>
                        <span className="ticker-price">₹{item.price}</span>
                        <span className={`ticker-change ${item.up ? 'up' : 'down'}`}>
                            {item.change}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default Ticker;
