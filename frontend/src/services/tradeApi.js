/**
 * Trade Service API Module
 * Provides functions to interact with the trade-service backend
 */

const API_BASE_URL = '/api/trades';

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
 * Trade status enum
 */
export const TradeStatus = {
    CREATED: 'CREATED',
    VALIDATED: 'VALIDATED',
    PLACED: 'PLACED',
    EXECUTING: 'EXECUTING',
    PARTIALLY_FILLED: 'PARTIALLY_FILLED',
    FILLED: 'FILLED',
    CLOSED: 'CLOSED',
    CANCELLED: 'CANCELLED',
    FAILED: 'FAILED'
};

/**
 * Order side enum
 */
export const OrderSide = {
    BUY: 'BUY',
    SELL: 'SELL'
};

/**
 * Get status display properties
 */
export const getStatusDisplay = (status) => {
    const statusMap = {
        [TradeStatus.CREATED]: { label: 'Created', color: '#6b7280', bgColor: 'rgba(107, 114, 128, 0.15)' },
        [TradeStatus.VALIDATED]: { label: 'Validated', color: '#3b82f6', bgColor: 'rgba(59, 130, 246, 0.15)' },
        [TradeStatus.PLACED]: { label: 'Placed', color: '#8b5cf6', bgColor: 'rgba(139, 92, 246, 0.15)' },
        [TradeStatus.EXECUTING]: { label: 'Executing', color: '#f59e0b', bgColor: 'rgba(245, 158, 11, 0.15)' },
        [TradeStatus.PARTIALLY_FILLED]: { label: 'Partial Fill', color: '#06b6d4', bgColor: 'rgba(6, 182, 212, 0.15)' },
        [TradeStatus.FILLED]: { label: 'Filled', color: '#22c55e', bgColor: 'rgba(34, 197, 94, 0.15)' },
        [TradeStatus.CLOSED]: { label: 'Closed', color: '#10b981', bgColor: 'rgba(16, 185, 129, 0.15)' },
        [TradeStatus.CANCELLED]: { label: 'Cancelled', color: '#ef4444', bgColor: 'rgba(239, 68, 68, 0.15)' },
        [TradeStatus.FAILED]: { label: 'Failed', color: '#dc2626', bgColor: 'rgba(220, 38, 38, 0.15)' }
    };
    return statusMap[status] || { label: status, color: '#6b7280', bgColor: 'rgba(107, 114, 128, 0.15)' };
};

/**
 * Check if trade is in a terminal state
 */
export const isTerminalStatus = (status) => {
    return [
        TradeStatus.CLOSED,
        TradeStatus.CANCELLED,
        TradeStatus.FAILED
    ].includes(status);
};

/**
 * Check if trade can be cancelled
 */
export const isCancellable = (status) => {
    return [
        TradeStatus.CREATED,
        TradeStatus.VALIDATED,
        TradeStatus.PLACED,
        TradeStatus.EXECUTING,
        TradeStatus.PARTIALLY_FILLED
    ].includes(status);
};

/**
 * Get user's trades with pagination
 * @param {number} [page=0] - Page number (0-indexed)
 * @param {number} [size=20] - Page size
 * @returns {Promise<Object>} Paginated trades
 */
export const getTrades = async (page = 0, size = 20) => {
    const params = new URLSearchParams({ page, size });
    const response = await fetch(`${API_BASE_URL}?${params}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to get trades');
    }

    return response.json();
};

/**
 * Get user's active (non-terminal) trades
 * @returns {Promise<Array>} List of active trades
 */
export const getActiveTrades = async () => {
    const response = await fetch(`${API_BASE_URL}/active`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to get active trades');
    }

    return response.json();
};

/**
 * Get trade by ID
 * @param {string} tradeId - Trade ID
 * @returns {Promise<Object>} Trade details
 */
export const getTrade = async (tradeId) => {
    const response = await fetch(`${API_BASE_URL}/${tradeId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to get trade');
    }

    return response.json();
};

/**
 * Cancel a trade
 * @param {string} tradeId - Trade ID to cancel
 * @param {string} reason - Cancellation reason
 * @returns {Promise<Object>} Cancelled trade
 */
export const cancelTrade = async (tradeId, reason) => {
    const params = new URLSearchParams({ reason });
    const response = await fetch(`${API_BASE_URL}/${tradeId}/cancel?${params}`, {
        method: 'POST',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || 'Failed to cancel trade');
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

/**
 * Calculate total trade value
 */
export const calculateTradeValue = (quantity, price) => {
    if (quantity == null || price == null) return null;
    return quantity * price;
};
