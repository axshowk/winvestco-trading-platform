/**
 * Payment API Service
 * Handles Razorpay payment integration for deposits
 */

const API_BASE_URL = '/api/payments';

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

// ==================== Payment APIs ====================

/**
 * Initiate a payment - creates Razorpay order
 * @param {number} amount - Amount to deposit in rupees
 * @param {string} description - Optional description
 * @returns {Promise<Object>} Razorpay order details for checkout
 */
export const initiatePayment = async (amount, description = '') => {
    const response = await fetch(`${API_BASE_URL}/initiate`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
            amount,
            description: description || `Wallet deposit of ₹${amount.toLocaleString('en-IN')}`
        })
    });
    return handleResponse(response);
};

/**
 * Verify payment after Razorpay checkout completes
 * @param {string} razorpayOrderId - Razorpay order ID
 * @param {string} razorpayPaymentId - Razorpay payment ID
 * @param {string} razorpaySignature - Razorpay signature
 * @returns {Promise<Object>} Verified payment details
 */
export const verifyPayment = async (razorpayOrderId, razorpayPaymentId, razorpaySignature) => {
    const response = await fetch(`${API_BASE_URL}/verify`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
            razorpayOrderId,
            razorpayPaymentId,
            razorpaySignature
        })
    });
    return handleResponse(response);
};

/**
 * Get payment by ID
 * @param {number} paymentId - Payment ID
 * @returns {Promise<Object>} Payment details
 */
export const getPayment = async (paymentId) => {
    const response = await fetch(`${API_BASE_URL}/${paymentId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

/**
 * Get payment history
 * @returns {Promise<Array>} List of payments
 */
export const getPaymentHistory = async () => {
    const response = await fetch(`${API_BASE_URL}/history`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

// ==================== Razorpay Checkout Helper ====================

/**
 * Open Razorpay checkout modal
 * @param {Object} orderData - Order data from initiatePayment
 * @param {Object} options - Additional options (name, email, phone)
 * @returns {Promise<Object>} Payment result with razorpay IDs
 */
export const openRazorpayCheckout = (orderData, options = {}) => {
    return new Promise((resolve, reject) => {
        // Check if Razorpay is loaded
        if (!window.Razorpay) {
            reject(new Error('Razorpay SDK not loaded. Please refresh the page.'));
            return;
        }

        const razorpayOptions = {
            key: orderData.razorpayKeyId,
            amount: orderData.amountInPaise,
            currency: orderData.currency,
            name: 'Winvestco',
            description: orderData.description || 'Wallet Deposit',
            order_id: orderData.orderId,
            prefill: {
                name: options.name || '',
                email: options.email || '',
                contact: options.phone || ''
            },
            theme: {
                color: '#6366f1' // Indigo to match app theme
            },
            handler: function (response) {
                // Payment successful
                resolve({
                    razorpayOrderId: response.razorpay_order_id,
                    razorpayPaymentId: response.razorpay_payment_id,
                    razorpaySignature: response.razorpay_signature
                });
            },
            modal: {
                ondismiss: function () {
                    reject(new Error('Payment cancelled by user'));
                },
                escape: true,
                backdropclose: false
            }
        };

        try {
            const rzp = new window.Razorpay(razorpayOptions);

            rzp.on('payment.failed', function (response) {
                reject(new Error(response.error.description || 'Payment failed'));
            });

            rzp.open();
        } catch (error) {
            reject(new Error('Failed to open payment modal: ' + error.message));
        }
    });
};

// ==================== Payment Status Enums ====================

export const PaymentStatus = {
    CREATED: 'CREATED',
    INITIATED: 'INITIATED',
    PENDING: 'PENDING',
    SUCCESS: 'SUCCESS',
    FAILED: 'FAILED',
    EXPIRED: 'EXPIRED'
};

/**
 * Get display info for payment status
 */
export const getPaymentStatusDisplay = (status) => {
    const displays = {
        [PaymentStatus.CREATED]: { label: 'Created', color: '#94a3b8', bgColor: 'rgba(148, 163, 184, 0.15)' },
        [PaymentStatus.INITIATED]: { label: 'Initiated', color: '#f59e0b', bgColor: 'rgba(245, 158, 11, 0.15)' },
        [PaymentStatus.PENDING]: { label: 'Pending', color: '#f59e0b', bgColor: 'rgba(245, 158, 11, 0.15)' },
        [PaymentStatus.SUCCESS]: { label: 'Success', color: '#10b981', bgColor: 'rgba(16, 185, 129, 0.15)' },
        [PaymentStatus.FAILED]: { label: 'Failed', color: '#ef4444', bgColor: 'rgba(239, 68, 68, 0.15)' },
        [PaymentStatus.EXPIRED]: { label: 'Expired', color: '#94a3b8', bgColor: 'rgba(148, 163, 184, 0.15)' }
    };
    return displays[status] || { label: status, color: '#94a3b8', bgColor: 'rgba(148, 163, 184, 0.15)' };
};
