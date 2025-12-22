/**
 * Funds API Service
 * Handles wallet balance, deposits, withdrawals, and transaction history
 */

const API_BASE_URL = '/api/v1/funds';

/**
 * Get auth headers with JWT token
 */
const getAuthHeaders = () => {
    const token = localStorage.getItem('accessToken');
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
};

/**
 * Handle API response
 */
const handleResponse = async (response) => {
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Request failed with status ${response.status}`);
    }
    return response.json();
};

// ==================== Wallet APIs ====================

/**
 * Get wallet balance for authenticated user
 */
export const getWallet = async () => {
    const response = await fetch(`${API_BASE_URL}/wallet`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

/**
 * Get balance summary (available, locked, total)
 */
export const getBalanceSummary = async () => {
    const response = await fetch(`${API_BASE_URL}/wallet/balance`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

// ==================== Transaction APIs ====================

/**
 * Initiate a deposit
 * @param {number} amount - Amount to deposit
 * @param {string} description - Optional description
 */
export const initiateDeposit = async (amount, description = '') => {
    const response = await fetch(`${API_BASE_URL}/deposit`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
            amount,
            description: description || `Deposit of ₹${amount.toLocaleString('en-IN')}`
        })
    });
    return handleResponse(response);
};

/**
 * Confirm a pending deposit (usually called by webhook)
 * @param {string} reference - Transaction reference
 */
export const confirmDeposit = async (reference) => {
    const response = await fetch(`${API_BASE_URL}/deposit/confirm?reference=${encodeURIComponent(reference)}`, {
        method: 'POST',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

/**
 * Initiate a withdrawal
 * @param {number} amount - Amount to withdraw
 * @param {string} description - Optional description
 */
export const initiateWithdrawal = async (amount, description = '') => {
    const response = await fetch(`${API_BASE_URL}/withdraw`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
            amount,
            description: description || `Withdrawal of ₹${amount.toLocaleString('en-IN')}`
        })
    });
    return handleResponse(response);
};

/**
 * Get paginated transaction history
 * @param {number} page - Page number (0-indexed)
 * @param {number} size - Page size
 */
export const getTransactions = async (page = 0, size = 10) => {
    const response = await fetch(`${API_BASE_URL}/transactions?page=${page}&size=${size}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

/**
 * Get transaction by reference
 * @param {string} reference - Transaction reference
 */
export const getTransactionByReference = async (reference) => {
    const response = await fetch(`${API_BASE_URL}/transactions/${encodeURIComponent(reference)}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

// ==================== Enums & Helpers ====================

export const TransactionType = {
    DEPOSIT: 'DEPOSIT',
    WITHDRAWAL: 'WITHDRAWAL',
    TRADE_DEBIT: 'TRADE_DEBIT',
    TRADE_CREDIT: 'TRADE_CREDIT',
    LOCK: 'LOCK',
    UNLOCK: 'UNLOCK'
};

export const TransactionStatus = {
    PENDING: 'PENDING',
    COMPLETED: 'COMPLETED',
    FAILED: 'FAILED',
    CANCELLED: 'CANCELLED'
};

/**
 * Get display info for transaction type
 */
export const getTypeDisplay = (type) => {
    const displays = {
        [TransactionType.DEPOSIT]: { label: 'Deposit', icon: 'plus', color: '#10b981' },
        [TransactionType.WITHDRAWAL]: { label: 'Withdrawal', icon: 'minus', color: '#ef4444' },
        [TransactionType.TRADE_DEBIT]: { label: 'Trade Debit', icon: 'trending-down', color: '#f59e0b' },
        [TransactionType.TRADE_CREDIT]: { label: 'Trade Credit', icon: 'trending-up', color: '#10b981' },
        [TransactionType.LOCK]: { label: 'Funds Locked', icon: 'lock', color: '#8b5cf6' },
        [TransactionType.UNLOCK]: { label: 'Funds Released', icon: 'unlock', color: '#06b6d4' }
    };
    return displays[type] || { label: type, icon: 'circle', color: '#94a3b8' };
};

/**
 * Get display info for transaction status
 */
export const getStatusDisplay = (status) => {
    const displays = {
        [TransactionStatus.PENDING]: { label: 'Pending', color: '#f59e0b', bgColor: 'rgba(245, 158, 11, 0.15)' },
        [TransactionStatus.COMPLETED]: { label: 'Completed', color: '#10b981', bgColor: 'rgba(16, 185, 129, 0.15)' },
        [TransactionStatus.FAILED]: { label: 'Failed', color: '#ef4444', bgColor: 'rgba(239, 68, 68, 0.15)' },
        [TransactionStatus.CANCELLED]: { label: 'Cancelled', color: '#94a3b8', bgColor: 'rgba(148, 163, 184, 0.15)' }
    };
    return displays[status] || { label: status, color: '#94a3b8', bgColor: 'rgba(148, 163, 184, 0.15)' };
};

/**
 * Format currency in INR
 */
export const formatCurrency = (amount) => {
    if (amount === null || amount === undefined) return '₹0.00';
    return `₹${Number(amount).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })}`;
};

/**
 * Format date/time
 */
export const formatDateTime = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};
