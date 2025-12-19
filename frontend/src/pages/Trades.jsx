import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Activity, Clock, CheckCircle, XCircle, RefreshCw,
    TrendingUp, TrendingDown, Filter, ChevronLeft, ChevronRight,
    AlertTriangle, Loader2, Search, BarChart3
} from 'lucide-react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import {
    getActiveTrades, getTrades, cancelTrade,
    getStatusDisplay, isCancellable, isTerminalStatus,
    formatCurrency, formatDateTime, formatQuantity,
    TradeStatus, OrderSide
} from '../services/tradeApi';
import './Trades.css';

const Trades = () => {
    const navigate = useNavigate();

    // Active trades state
    const [activeTrades, setActiveTrades] = useState([]);
    const [loadingActive, setLoadingActive] = useState(true);
    const [activeError, setActiveError] = useState(null);

    // Trade history state
    const [tradeHistory, setTradeHistory] = useState([]);
    const [loadingHistory, setLoadingHistory] = useState(true);
    const [historyError, setHistoryError] = useState(null);
    const [historyPage, setHistoryPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [statusFilter, setStatusFilter] = useState('ALL');

    // Cancel modal state
    const [cancelModalTrade, setCancelModalTrade] = useState(null);
    const [cancelReason, setCancelReason] = useState('');
    const [cancelling, setCancelling] = useState(false);
    const [cancelError, setCancelError] = useState(null);

    // Refresh state
    const [refreshing, setRefreshing] = useState(false);

    const isAuthenticated = () => {
        return !!localStorage.getItem('accessToken');
    };

    const fetchActiveTrades = useCallback(async () => {
        if (!isAuthenticated()) return;

        try {
            setLoadingActive(true);
            const trades = await getActiveTrades();
            setActiveTrades(trades);
            setActiveError(null);
        } catch (err) {
            console.error('Error fetching active trades:', err);
            setActiveError(err.message);
        } finally {
            setLoadingActive(false);
        }
    }, []);

    const fetchTradeHistory = useCallback(async (page = 0) => {
        if (!isAuthenticated()) return;

        try {
            setLoadingHistory(true);
            const response = await getTrades(page, 10);

            // Filter by status if needed
            let trades = response.content || [];
            if (statusFilter !== 'ALL') {
                trades = trades.filter(trade => trade.status === statusFilter);
            }

            setTradeHistory(trades);
            setTotalPages(response.totalPages || 1);
            setHistoryPage(page);
            setHistoryError(null);
        } catch (err) {
            console.error('Error fetching trade history:', err);
            setHistoryError(err.message);
        } finally {
            setLoadingHistory(false);
        }
    }, [statusFilter]);

    useEffect(() => {
        if (!isAuthenticated()) {
            navigate('/login');
            return;
        }
        fetchActiveTrades();
        fetchTradeHistory(0);
    }, [fetchActiveTrades, fetchTradeHistory, navigate]);

    useEffect(() => {
        fetchTradeHistory(0);
    }, [statusFilter, fetchTradeHistory]);

    const handleRefresh = async () => {
        setRefreshing(true);
        await Promise.all([fetchActiveTrades(), fetchTradeHistory(historyPage)]);
        setRefreshing(false);
    };

    const openCancelModal = (trade) => {
        setCancelModalTrade(trade);
        setCancelReason('');
        setCancelError(null);
    };

    const closeCancelModal = () => {
        setCancelModalTrade(null);
        setCancelReason('');
        setCancelError(null);
    };

    const handleCancelTrade = async () => {
        if (!cancelReason.trim()) {
            setCancelError('Please provide a cancellation reason');
            return;
        }

        try {
            setCancelling(true);
            setCancelError(null);
            await cancelTrade(cancelModalTrade.tradeId, cancelReason);
            closeCancelModal();
            // Refresh both lists
            await Promise.all([fetchActiveTrades(), fetchTradeHistory(historyPage)]);
        } catch (err) {
            console.error('Error cancelling trade:', err);
            setCancelError(err.message);
        } finally {
            setCancelling(false);
        }
    };

    const navigateToStock = (symbol) => {
        navigate(`/stock/${symbol}`);
    };

    const getSideClass = (side) => {
        return side === OrderSide.BUY ? 'buy' : 'sell';
    };

    // Summary stats
    const totalActiveTrades = activeTrades.length;
    const executingTrades = activeTrades.filter(t =>
        t.status === TradeStatus.EXECUTING || t.status === TradeStatus.PARTIALLY_FILLED
    ).length;
    const filledTrades = tradeHistory.filter(t => t.status === TradeStatus.FILLED).length;
    const closedTrades = tradeHistory.filter(t => t.status === TradeStatus.CLOSED).length;

    if (!isAuthenticated()) {
        return null;
    }

    return (
        <div className="page-wrapper">
            <Navbar onLogin={() => navigate('/login')} />
            <div className="trades-container">
                {/* Header */}
                <div className="trades-header">
                    <div className="header-title">
                        <Activity size={28} />
                        <h1>Trades</h1>
                    </div>
                    <button
                        className={`refresh-btn ${refreshing ? 'spinning' : ''}`}
                        onClick={handleRefresh}
                        disabled={refreshing}
                    >
                        <RefreshCw size={18} />
                        <span>Refresh</span>
                    </button>
                </div>

                {/* Summary Cards */}
                <div className="summary-cards">
                    <div className="summary-card glass">
                        <div className="summary-icon active">
                            <Loader2 size={24} />
                        </div>
                        <div className="summary-content">
                            <span className="summary-value">{totalActiveTrades}</span>
                            <span className="summary-label">Active Trades</span>
                        </div>
                    </div>
                    <div className="summary-card glass">
                        <div className="summary-icon filled">
                            <BarChart3 size={24} />
                        </div>
                        <div className="summary-content">
                            <span className="summary-value">{executingTrades}</span>
                            <span className="summary-label">Executing</span>
                        </div>
                    </div>
                    <div className="summary-card glass">
                        <div className="summary-icon closed">
                            <CheckCircle size={24} />
                        </div>
                        <div className="summary-content">
                            <span className="summary-value">{closedTrades}</span>
                            <span className="summary-label">Closed</span>
                        </div>
                    </div>
                    <div className="summary-card glass">
                        <div className="summary-icon cancelled">
                            <XCircle size={24} />
                        </div>
                        <div className="summary-content">
                            <span className="summary-value">{filledTrades}</span>
                            <span className="summary-label">Filled</span>
                        </div>
                    </div>
                </div>

                {/* Active Trades Section */}
                <div className="trades-section">
                    <div className="section-header">
                        <Clock size={20} />
                        <h2>Active Trades</h2>
                        <span className="trade-count">{totalActiveTrades}</span>
                    </div>

                    {loadingActive ? (
                        <div className="loading-state">
                            <div className="loading-spinner"></div>
                            <p>Loading active trades...</p>
                        </div>
                    ) : activeError ? (
                        <div className="error-state">
                            <XCircle size={32} />
                            <p>{activeError}</p>
                            <button onClick={fetchActiveTrades} className="retry-btn">Try Again</button>
                        </div>
                    ) : activeTrades.length === 0 ? (
                        <div className="empty-state glass">
                            <Activity size={48} />
                            <h3>No Active Trades</h3>
                            <p>Your active trades will appear here.</p>
                            <button onClick={() => navigate('/markets')} className="action-btn">
                                Explore Markets
                            </button>
                        </div>
                    ) : (
                        <div className="trades-table-container glass">
                            <table className="trades-table">
                                <thead>
                                    <tr>
                                        <th>Symbol</th>
                                        <th>Side</th>
                                        <th>Type</th>
                                        <th>Qty</th>
                                        <th>Filled</th>
                                        <th>Avg Price</th>
                                        <th>Value</th>
                                        <th>Status</th>
                                        <th>Time</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {activeTrades.map((trade) => {
                                        const statusInfo = getStatusDisplay(trade.status);
                                        const tradeValue = trade.averagePrice
                                            ? trade.executedQuantity * trade.averagePrice
                                            : trade.quantity * (trade.price || 0);
                                        return (
                                            <tr key={trade.tradeId}>
                                                <td>
                                                    <button
                                                        className="symbol-link"
                                                        onClick={() => navigateToStock(trade.symbol)}
                                                    >
                                                        {trade.symbol}
                                                    </button>
                                                </td>
                                                <td>
                                                    <span className={`side-badge ${getSideClass(trade.side)}`}>
                                                        {trade.side === OrderSide.BUY ? (
                                                            <TrendingUp size={12} />
                                                        ) : (
                                                            <TrendingDown size={12} />
                                                        )}
                                                        {trade.side}
                                                    </span>
                                                </td>
                                                <td className="trade-type">{trade.tradeType?.replace('_', ' ')}</td>
                                                <td>{formatQuantity(trade.quantity)}</td>
                                                <td>{formatQuantity(trade.executedQuantity || 0)}/{formatQuantity(trade.quantity)}</td>
                                                <td>{trade.averagePrice ? formatCurrency(trade.averagePrice) : '-'}</td>
                                                <td className="value-cell">{formatCurrency(tradeValue)}</td>
                                                <td>
                                                    <span
                                                        className="status-badge"
                                                        style={{
                                                            color: statusInfo.color,
                                                            backgroundColor: statusInfo.bgColor
                                                        }}
                                                    >
                                                        {statusInfo.label}
                                                    </span>
                                                </td>
                                                <td className="time-cell">{formatDateTime(trade.createdAt)}</td>
                                                <td>
                                                    {isCancellable(trade.status) && (
                                                        <button
                                                            className="cancel-btn"
                                                            onClick={() => openCancelModal(trade)}
                                                        >
                                                            Cancel
                                                        </button>
                                                    )}
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>

                {/* Trade History Section */}
                <div className="trades-section">
                    <div className="section-header">
                        <Activity size={20} />
                        <h2>Trade History</h2>
                        <div className="filter-group">
                            <Filter size={16} />
                            <select
                                value={statusFilter}
                                onChange={(e) => setStatusFilter(e.target.value)}
                                className="status-filter"
                            >
                                <option value="ALL">All Trades</option>
                                <option value={TradeStatus.FILLED}>Filled</option>
                                <option value={TradeStatus.CLOSED}>Closed</option>
                                <option value={TradeStatus.CANCELLED}>Cancelled</option>
                                <option value={TradeStatus.FAILED}>Failed</option>
                            </select>
                        </div>
                    </div>

                    {loadingHistory ? (
                        <div className="loading-state">
                            <div className="loading-spinner"></div>
                            <p>Loading trade history...</p>
                        </div>
                    ) : historyError ? (
                        <div className="error-state">
                            <XCircle size={32} />
                            <p>{historyError}</p>
                            <button onClick={() => fetchTradeHistory(historyPage)} className="retry-btn">Try Again</button>
                        </div>
                    ) : tradeHistory.length === 0 ? (
                        <div className="empty-state glass">
                            <Search size={48} />
                            <h3>No Trades Found</h3>
                            <p>
                                {statusFilter !== 'ALL'
                                    ? `No ${statusFilter.toLowerCase()} trades found.`
                                    : 'Your trade history will appear here.'}
                            </p>
                        </div>
                    ) : (
                        <>
                            <div className="trades-table-container glass">
                                <table className="trades-table">
                                    <thead>
                                        <tr>
                                            <th>Trade ID</th>
                                            <th>Symbol</th>
                                            <th>Side</th>
                                            <th>Type</th>
                                            <th>Qty</th>
                                            <th>Avg Price</th>
                                            <th>Value</th>
                                            <th>Status</th>
                                            <th>Executed</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {tradeHistory.map((trade) => {
                                            const statusInfo = getStatusDisplay(trade.status);
                                            const tradeValue = trade.averagePrice
                                                ? trade.executedQuantity * trade.averagePrice
                                                : 0;
                                            return (
                                                <tr key={trade.tradeId} className={isTerminalStatus(trade.status) ? 'terminal' : ''}>
                                                    <td className="trade-id">{trade.tradeId?.substring(0, 8)}...</td>
                                                    <td>
                                                        <button
                                                            className="symbol-link"
                                                            onClick={() => navigateToStock(trade.symbol)}
                                                        >
                                                            {trade.symbol}
                                                        </button>
                                                    </td>
                                                    <td>
                                                        <span className={`side-badge ${getSideClass(trade.side)}`}>
                                                            {trade.side === OrderSide.BUY ? (
                                                                <TrendingUp size={12} />
                                                            ) : (
                                                                <TrendingDown size={12} />
                                                            )}
                                                            {trade.side}
                                                        </span>
                                                    </td>
                                                    <td className="trade-type">{trade.tradeType?.replace('_', ' ')}</td>
                                                    <td>{formatQuantity(trade.executedQuantity || 0)}/{formatQuantity(trade.quantity)}</td>
                                                    <td>{trade.averagePrice ? formatCurrency(trade.averagePrice) : '-'}</td>
                                                    <td className="value-cell">{tradeValue > 0 ? formatCurrency(tradeValue) : '-'}</td>
                                                    <td>
                                                        <span
                                                            className="status-badge"
                                                            style={{
                                                                color: statusInfo.color,
                                                                backgroundColor: statusInfo.bgColor
                                                            }}
                                                        >
                                                            {statusInfo.label}
                                                        </span>
                                                    </td>
                                                    <td className="time-cell">{formatDateTime(trade.executedAt || trade.createdAt)}</td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>

                            {/* Pagination */}
                            {totalPages > 1 && (
                                <div className="pagination">
                                    <button
                                        className="page-btn"
                                        onClick={() => fetchTradeHistory(historyPage - 1)}
                                        disabled={historyPage === 0}
                                    >
                                        <ChevronLeft size={18} />
                                    </button>
                                    <span className="page-info">
                                        Page {historyPage + 1} of {totalPages}
                                    </span>
                                    <button
                                        className="page-btn"
                                        onClick={() => fetchTradeHistory(historyPage + 1)}
                                        disabled={historyPage >= totalPages - 1}
                                    >
                                        <ChevronRight size={18} />
                                    </button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>

            {/* Cancel Trade Modal */}
            {cancelModalTrade && (
                <div className="modal-overlay" onClick={closeCancelModal}>
                    <div className="cancel-modal glass" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Cancel Trade</h3>
                            <button className="close-btn" onClick={closeCancelModal}>×</button>
                        </div>
                        <div className="modal-body">
                            <p>Are you sure you want to cancel this trade?</p>
                            <div className="trade-summary">
                                <div className="summary-row">
                                    <span>Symbol:</span>
                                    <strong>{cancelModalTrade.symbol}</strong>
                                </div>
                                <div className="summary-row">
                                    <span>Side:</span>
                                    <strong className={getSideClass(cancelModalTrade.side)}>
                                        {cancelModalTrade.side}
                                    </strong>
                                </div>
                                <div className="summary-row">
                                    <span>Quantity:</span>
                                    <strong>{formatQuantity(cancelModalTrade.quantity)}</strong>
                                </div>
                                <div className="summary-row">
                                    <span>Filled:</span>
                                    <strong>{formatQuantity(cancelModalTrade.executedQuantity || 0)}</strong>
                                </div>
                            </div>
                            <div className="form-group">
                                <label>Cancellation Reason *</label>
                                <textarea
                                    value={cancelReason}
                                    onChange={(e) => setCancelReason(e.target.value)}
                                    placeholder="Please provide a reason for cancellation"
                                    rows={3}
                                />
                            </div>
                            {cancelError && (
                                <div className="error-message">
                                    <AlertTriangle size={16} />
                                    {cancelError}
                                </div>
                            )}
                        </div>
                        <div className="modal-footer">
                            <button className="btn-secondary" onClick={closeCancelModal}>
                                Keep Trade
                            </button>
                            <button
                                className="btn-danger"
                                onClick={handleCancelTrade}
                                disabled={cancelling}
                            >
                                {cancelling ? 'Cancelling...' : 'Cancel Trade'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
};

export default Trades;
