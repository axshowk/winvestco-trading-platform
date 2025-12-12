import React from 'react';
import './Ticker.css';

const Ticker = () => {
    const [tickers, setTickers] = React.useState([]);

    React.useEffect(() => {
        const fetchNiftyData = async () => {
            try {
                const response = await fetch('/api/market/indices/NIFTY 50');
                if (response.ok) {
                    const data = await response.json();
                    console.log('Nifty 50 Data:', data);

                    if (data.data && Array.isArray(data.data)) {
                        const newTickers = data.data
                            .filter(item => item.symbol && item.symbol !== 'NIFTY 50') // Exclude index summary if present in list
                            .map(item => ({
                                symbol: item.symbol,
                                price: item.lastPrice ? item.lastPrice.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00',
                                change: (item.pChange > 0 ? '+' : '') + (item.pChange ? item.pChange.toFixed(2) : '0.00') + '%',
                                up: item.change > 0
                            }));

                        // If we got valid tickers, update state
                        if (newTickers.length > 0) {
                            setTickers(newTickers);
                        }
                    }
                }
            } catch (error) {
                console.error('Error fetching Nifty 50 data:', error);
            }
        };

        fetchNiftyData();
        // Auto-refresh disabled
        // const interval = setInterval(fetchNiftyData, 10000);
        // return () => clearInterval(interval);
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
