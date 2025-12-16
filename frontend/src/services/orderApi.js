/**
 * Order Service API Module
 * Provides functions to interact with the order-service backend
 */

const API_BASE_URL = '/api/orders';

/**
 * Get auth headers with JWT token
 */
const getAuthHeaders = () => {
    const token = localStorage.getItem('accessToken');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
};

/**
 * Order types enum
 */
export const OrderType = {
    MARKET: 'MARKET',
    LIMIT: 'LIMIT',
    STOP_LOSS: 'STOP_LOSS',
    STOP_LIMIT: 'STOP_LIMIT'
};

/**
 * Order side enum
 */
export const OrderSide = {
    BUY: 'BUY',
    SELL: 'SELL'
};

/**
 * Order validity enum
 */
export const OrderValidity = {
    DAY: 'DAY',
    IOC: 'IOC',
    GTC: 'GTC'
};

/**
 * Order status enum
 */
export const OrderStatus = {
    NEW: 'NEW',
    VALIDATED: 'VALIDATED',
    FUNDS_LOCKED: 'FUNDS_LOCKED',
    PENDING: 'PENDING',
    PARTIALLY_FILLED: 'PARTIALLY_FILLED',
    FILLED: 'FILLED',
    CANCELLED: 'CANCELLED',
    REJECTED: 'REJECTED',
    EXPIRED: 'EXPIRED'
};

/**
 * Get status display properties
 */
export const getStatusDisplay = (status) => {
    const statusMap = {
        [OrderStatus.NEW]: { label: 'New', color: '#6b7280', bgColor: 'rgba(107, 114, 128, 0.15)' },
        [OrderStatus.VALIDATED]: { label: 'Validated', color: '#3b82f6', bgColor: 'rgba(59, 130, 246, 0.15)' },
        [OrderStatus.FUNDS_LOCKED]: { label: 'Funds Locked', color: '#8b5cf6', bgColor: 'rgba(139, 92, 246, 0.15)' },
        [OrderStatus.PENDING]: { label: 'Pending', color: '#f59e0b', bgColor: 'rgba(245, 158, 11, 0.15)' },
        [OrderStatus.PARTIALLY_FILLED]: { label: 'Partial Fill', color: '#06b6d4', bgColor: 'rgba(6, 182, 212, 0.15)' },
        [OrderStatus.FILLED]: { label: 'Filled', color: '#10b981', bgColor: 'rgba(16, 185, 129, 0.15)' },
        [OrderStatus.CANCELLED]: { label: 'Cancelled', color: '#ef4444', bgColor: 'rgba(239, 68, 68, 0.15)' },
        [OrderStatus.REJECTED]: { label: 'Rejected', color: '#dc2626', bgColor: 'rgba(220, 38, 38, 0.15)' },
        [OrderStatus.EXPIRED]: { label: 'Expired', color: '#9ca3af', bgColor: 'rgba(156, 163, 175, 0.15)' }
    };
    return statusMap[status] || { label: status, color: '#6b7280', bgColor: 'rgba(107, 114, 128, 0.15)' };
};

/**
 * Check if order is in a terminal state
 */
export const isTerminalStatus = (status) => {
    return [
        OrderStatus.FILLED,
        OrderStatus.CANCELLED,
        OrderStatus.REJECTED,
        OrderStatus.EXPIRED
    ].includes(status);
};

/**
 * Check if order can be cancelled
 */
export const isCancellable = (status) => {
    return [
        OrderStatus.NEW,
        OrderStatus.VALIDATED,
        OrderStatus.FUNDS_LOCKED,
        OrderStatus.PENDING,
        OrderStatus.PARTIALLY_FILLED
    ].includes(status);
};

/**
 * Create a new order
 * @param {Object} orderRequest - Order request object
 * @param {string} orderRequest.symbol - Stock symbol
 * @param {string} orderRequest.side - BUY or SELL
 * @param {string} orderRequest.orderType - MARKET, LIMIT, STOP_LOSS, STOP_LIMIT
 * @param {number} orderRequest.quantity - Order quantity
 * @param {number} [orderRequest.price] - Limit price (required for LIMIT, STOP_LIMIT)
 * @param {number} [orderRequest.stopPrice] - Stop price (required for STOP_LOSS, STOP_LIMIT)
 * @param {string} [orderRequest.validity] - DAY, IOC, or GTC (defaults to DAY)
 * @returns {Promise<Object>} Created order
 */
export const createOrder = async (orderRequest) => {
    const response = await fetch(API_BASE_URL, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(orderRequest)
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to create order');
    }

    return response.json();
};

/**
 * Get order by ID
 * @param {string} orderId - Order ID (UUID)
 * @returns {Promise<Object>} Order details
 */
export const getOrder = async (orderId) => {
    const response = await fetch(`${API_BASE_URL}/${orderId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to get order');
    }

    return response.json();
};

/**
 * Get user's orders with pagination
 * @param {number} [page=0] - Page number (0-indexed)
 * @param {number} [size=20] - Page size
 * @returns {Promise<Object>} Paginated orders
 */
export const getOrders = async (page = 0, size = 20) => {
    const params = new URLSearchParams({ page, size });
    const response = await fetch(`${API_BASE_URL}?${params}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to get orders');
    }

    return response.json();
};

/**
 * Get user's active (non-terminal) orders
 * @returns {Promise<Array>} List of active orders
 */
export const getActiveOrders = async () => {
    const response = await fetch(`${API_BASE_URL}/active`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to get active orders');
    }

    return response.json();
};

/**
 * Cancel an order
 * @param {string} orderId - Order ID to cancel
 * @param {string} reason - Cancellation reason
 * @returns {Promise<Object>} Cancelled order
 */
export const cancelOrder = async (orderId, reason) => {
    const response = await fetch(`${API_BASE_URL}/${orderId}/cancel`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ reason })
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to cancel order');
    }

    return response.json();
};

/**
 * Format currency in Indian format
 */
export const formatCurrency = (value) => {
    if (value == null) return '₹-';
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2
    }).format(value);
};

/**
 * Format date/time for display
 */
export const formatDateTime = (isoString) => {
    if (!isoString) return '-';
    const date = new Date(isoString);
    return date.toLocaleString('en-IN', {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit'
    });
};

/**
 * Format quantity with commas
 */
export const formatQuantity = (value) => {
    if (value == null) return '-';
    return new Intl.NumberFormat('en-IN').format(value);
};
