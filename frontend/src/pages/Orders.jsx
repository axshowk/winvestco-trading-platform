import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    ClipboardList, Clock, CheckCircle, XCircle, RefreshCw,
    TrendingUp, TrendingDown, Filter, ChevronLeft, ChevronRight,
    AlertTriangle, Loader2, Search
} from 'lucide-react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import {
    getActiveOrders, getOrders, cancelOrder,
    getStatusDisplay, isCancellable, isTerminalStatus,
    formatCurrency, formatDateTime, formatQuantity,
    OrderStatus, OrderSide
} from '../services/orderApi';
import './Orders.css';

const Orders = () => {
    const navigate = useNavigate();

    // Active orders state
    const [activeOrders, setActiveOrders] = useState([]);
    const [loadingActive, setLoadingActive] = useState(true);
    const [activeError, setActiveError] = useState(null);

    // Order history state
    const [orderHistory, setOrderHistory] = useState([]);
    const [loadingHistory, setLoadingHistory] = useState(true);
    const [historyError, setHistoryError] = useState(null);
    const [historyPage, setHistoryPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [statusFilter, setStatusFilter] = useState('ALL');

    // Cancel modal state
    const [cancelModalOrder, setCancelModalOrder] = useState(null);
    const [cancelReason, setCancelReason] = useState('');
    const [cancelling, setCancelling] = useState(false);
    const [cancelError, setCancelError] = useState(null);

    // Refresh state
    const [refreshing, setRefreshing] = useState(false);

    const isAuthenticated = () => {
        return !!localStorage.getItem('accessToken');
    };

    const fetchActiveOrders = useCallback(async () => {
        if (!isAuthenticated()) return;

        try {
            setLoadingActive(true);
            const orders = await getActiveOrders();
            setActiveOrders(orders);
            setActiveError(null);
        } catch (err) {
            console.error('Error fetching active orders:', err);
            setActiveError(err.message);
        } finally {
            setLoadingActive(false);
        }
    }, []);

    const fetchOrderHistory = useCallback(async (page = 0) => {
        if (!isAuthenticated()) return;

        try {
            setLoadingHistory(true);
            const response = await getOrders(page, 10);

            // Filter by status if needed
            let orders = response.content || [];
            if (statusFilter !== 'ALL') {
                orders = orders.filter(order => order.status === statusFilter);
            }

            setOrderHistory(orders);
            setTotalPages(response.totalPages || 1);
            setHistoryPage(page);
            setHistoryError(null);
        } catch (err) {
            console.error('Error fetching order history:', err);
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
        fetchActiveOrders();
        fetchOrderHistory(0);
    }, [fetchActiveOrders, fetchOrderHistory, navigate]);

    useEffect(() => {
        fetchOrderHistory(0);
    }, [statusFilter, fetchOrderHistory]);

    const handleRefresh = async () => {
        setRefreshing(true);
        await Promise.all([fetchActiveOrders(), fetchOrderHistory(historyPage)]);
        setRefreshing(false);
    };

    const openCancelModal = (order) => {
        setCancelModalOrder(order);
        setCancelReason('');
        setCancelError(null);
    };

    const closeCancelModal = () => {
        setCancelModalOrder(null);
        setCancelReason('');
        setCancelError(null);
    };

    const handleCancelOrder = async () => {
        if (!cancelReason.trim()) {
            setCancelError('Please provide a cancellation reason');
            return;
        }

        try {
            setCancelling(true);
            setCancelError(null);
            await cancelOrder(cancelModalOrder.orderId, cancelReason);
            closeCancelModal();
            // Refresh both lists
            await Promise.all([fetchActiveOrders(), fetchOrderHistory(historyPage)]);
        } catch (err) {
            console.error('Error cancelling order:', err);
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
    const totalActiveOrders = activeOrders.length;
    const pendingOrders = activeOrders.filter(o => o.status === OrderStatus.PENDING).length;
    const partiallyFilledOrders = activeOrders.filter(o => o.status === OrderStatus.PARTIALLY_FILLED).length;

    if (!isAuthenticated()) {
        return null;
    }

    return (
        <div className="page-wrapper">
            <Navbar onLogin={() => navigate('/login')} />
            <div className="orders-container">
                {/* Header */}
                <div className="orders-header">
                    <div className="header-title">
                        <ClipboardList size={28} />
                        <h1>Orders</h1>
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
                        <div className="summary-icon pending">
                            <Clock size={24} />
                        </div>
                        <div className="summary-content">
                            <span className="summary-value">{totalActiveOrders}</span>
                            <span className="summary-label">Active Orders</span>
                        </div>
                    </div>
                    <div className="summary-card glass">
                        <div className="summary-icon waiting">
                            <Loader2 size={24} />
                        </div>
                        <div className="summary-content">
                            <span className="summary-value">{pendingOrders}</span>
                            <span className="summary-label">Pending</span>
                        </div>
                    </div>
                    <div className="summary-card glass">
                        <div className="summary-icon partial">
                            <AlertTriangle size={24} />
                        </div>
                        <div className="summary-content">
                            <span className="summary-value">{partiallyFilledOrders}</span>
                            <span className="summary-label">Partially Filled</span>
                        </div>
                    </div>
                </div>

                {/* Open Orders Section */}
                <div className="orders-section">
                    <div className="section-header">
                        <Clock size={20} />
                        <h2>Open Orders</h2>
                        <span className="order-count">{totalActiveOrders}</span>
                    </div>

                    {loadingActive ? (
                        <div className="loading-state">
                            <div className="loading-spinner"></div>
                            <p>Loading active orders...</p>
                        </div>
                    ) : activeError ? (
                        <div className="error-state">
                            <XCircle size={32} />
                            <p>{activeError}</p>
                            <button onClick={fetchActiveOrders} className="retry-btn">Try Again</button>
                        </div>
                    ) : activeOrders.length === 0 ? (
                        <div className="empty-state glass">
                            <ClipboardList size={48} />
                            <h3>No Open Orders</h3>
                            <p>Your active orders will appear here.</p>
                            <button onClick={() => navigate('/markets')} className="action-btn">
                                Explore Markets
                            </button>
                        </div>
                    ) : (
                        <div className="orders-table-container glass">
                            <table className="orders-table">
                                <thead>
                                    <tr>
                                        <th>Symbol</th>
                                        <th>Type</th>
                                        <th>Side</th>
                                        <th>Qty</th>
                                        <th>Price</th>
                                        <th>Filled</th>
                                        <th>Status</th>
                                        <th>Time</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {activeOrders.map((order) => {
                                        const statusInfo = getStatusDisplay(order.status);
                                        return (
                                            <tr key={order.orderId}>
                                                <td>
                                                    <button
                                                        className="symbol-link"
                                                        onClick={() => navigateToStock(order.symbol)}
                                                    >
                                                        {order.symbol}
                                                    </button>
                                                </td>
                                                <td className="order-type">{order.orderType?.replace('_', ' ')}</td>
                                                <td>
                                                    <span className={`side-badge ${getSideClass(order.side)}`}>
                                                        {order.side === OrderSide.BUY ? (
                                                            <TrendingUp size={12} />
                                                        ) : (
                                                            <TrendingDown size={12} />
                                                        )}
                                                        {order.side}
                                                    </span>
                                                </td>
                                                <td>{formatQuantity(order.quantity)}</td>
                                                <td>{order.price ? formatCurrency(order.price) : 'MKT'}</td>
                                                <td>{formatQuantity(order.filledQuantity || 0)}/{formatQuantity(order.quantity)}</td>
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
                                                <td className="time-cell">{formatDateTime(order.createdAt)}</td>
                                                <td>
                                                    {isCancellable(order.status) && (
                                                        <button
                                                            className="cancel-btn"
                                                            onClick={() => openCancelModal(order)}
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

                {/* Order History Section */}
                <div className="orders-section">
                    <div className="section-header">
                        <ClipboardList size={20} />
                        <h2>Order History</h2>
                        <div className="filter-group">
                            <Filter size={16} />
                            <select
                                value={statusFilter}
                                onChange={(e) => setStatusFilter(e.target.value)}
                                className="status-filter"
                            >
                                <option value="ALL">All Orders</option>
                                <option value={OrderStatus.FILLED}>Filled</option>
                                <option value={OrderStatus.CANCELLED}>Cancelled</option>
                                <option value={OrderStatus.EXPIRED}>Expired</option>
                                <option value={OrderStatus.REJECTED}>Rejected</option>
                            </select>
                        </div>
                    </div>

                    {loadingHistory ? (
                        <div className="loading-state">
                            <div className="loading-spinner"></div>
                            <p>Loading order history...</p>
                        </div>
                    ) : historyError ? (
                        <div className="error-state">
                            <XCircle size={32} />
                            <p>{historyError}</p>
                            <button onClick={() => fetchOrderHistory(historyPage)} className="retry-btn">Try Again</button>
                        </div>
                    ) : orderHistory.length === 0 ? (
                        <div className="empty-state glass">
                            <Search size={48} />
                            <h3>No Orders Found</h3>
                            <p>
                                {statusFilter !== 'ALL'
                                    ? `No ${statusFilter.toLowerCase()} orders found.`
                                    : 'Your order history will appear here.'}
                            </p>
                        </div>
                    ) : (
                        <>
                            <div className="orders-table-container glass">
                                <table className="orders-table">
                                    <thead>
                                        <tr>
                                            <th>Order ID</th>
                                            <th>Symbol</th>
                                            <th>Side</th>
                                            <th>Type</th>
                                            <th>Qty</th>
                                            <th>Avg Price</th>
                                            <th>Status</th>
                                            <th>Time</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {orderHistory.map((order) => {
                                            const statusInfo = getStatusDisplay(order.status);
                                            return (
                                                <tr key={order.orderId} className={isTerminalStatus(order.status) ? 'terminal' : ''}>
                                                    <td className="order-id">{order.orderId?.substring(0, 8)}...</td>
                                                    <td>
                                                        <button
                                                            className="symbol-link"
                                                            onClick={() => navigateToStock(order.symbol)}
                                                        >
                                                            {order.symbol}
                                                        </button>
                                                    </td>
                                                    <td>
                                                        <span className={`side-badge ${getSideClass(order.side)}`}>
                                                            {order.side === OrderSide.BUY ? (
                                                                <TrendingUp size={12} />
                                                            ) : (
                                                                <TrendingDown size={12} />
                                                            )}
                                                            {order.side}
                                                        </span>
                                                    </td>
                                                    <td className="order-type">{order.orderType?.replace('_', ' ')}</td>
                                                    <td>{formatQuantity(order.filledQuantity || 0)}/{formatQuantity(order.quantity)}</td>
                                                    <td>{order.averagePrice ? formatCurrency(order.averagePrice) : '-'}</td>
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
                                                    <td className="time-cell">{formatDateTime(order.createdAt)}</td>
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
                                        onClick={() => fetchOrderHistory(historyPage - 1)}
                                        disabled={historyPage === 0}
                                    >
                                        <ChevronLeft size={18} />
                                    </button>
                                    <span className="page-info">
                                        Page {historyPage + 1} of {totalPages}
                                    </span>
                                    <button
                                        className="page-btn"
                                        onClick={() => fetchOrderHistory(historyPage + 1)}
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

            {/* Cancel Order Modal */}
            {cancelModalOrder && (
                <div className="modal-overlay" onClick={closeCancelModal}>
                    <div className="cancel-modal glass" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Cancel Order</h3>
                            <button className="close-btn" onClick={closeCancelModal}>×</button>
                        </div>
                        <div className="modal-body">
                            <p>Are you sure you want to cancel this order?</p>
                            <div className="order-summary">
                                <div className="summary-row">
                                    <span>Symbol:</span>
                                    <strong>{cancelModalOrder.symbol}</strong>
                                </div>
                                <div className="summary-row">
                                    <span>Side:</span>
                                    <strong className={getSideClass(cancelModalOrder.side)}>
                                        {cancelModalOrder.side}
                                    </strong>
                                </div>
                                <div className="summary-row">
                                    <span>Quantity:</span>
                                    <strong>{formatQuantity(cancelModalOrder.quantity)}</strong>
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
                                Keep Order
                            </button>
                            <button
                                className="btn-danger"
                                onClick={handleCancelOrder}
                                disabled={cancelling}
                            >
                                {cancelling ? 'Cancelling...' : 'Cancel Order'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
};

export default Orders;
