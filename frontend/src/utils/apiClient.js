/**
 * Centralized API Client
 * Provides standardized error handling, retry logic with exponential backoff,
 * and automatic auth token injection.
 */

// Default retry configuration
const DEFAULT_RETRY_CONFIG = {
    maxRetries: 3,
    baseDelayMs: 1000,
    maxDelayMs: 10000,
    retryableStatuses: [408, 429, 500, 502, 503, 504]
};

/**
 * Custom API Error class with structured error information
 */
export class ApiError extends Error {
    constructor(message, status, code, data = null, isRetryable = false) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.code = code;
        this.data = data;
        this.isRetryable = isRetryable;
        this.timestamp = new Date().toISOString();
    }

    /**
     * Get user-friendly error message
     */
    getUserMessage() {
        switch (this.status) {
            case 0:
                return 'Unable to connect to server. Please check your internet connection.';
            case 401:
                return 'Your session has expired. Please log in again.';
            case 403:
                return 'You don\'t have permission to perform this action.';
            case 404:
                return 'The requested resource was not found.';
            case 408:
            case 504:
                return 'Request timed out. Please try again.';
            case 429:
                return 'Too many requests. Please wait a moment and try again.';
            case 500:
            case 502:
            case 503:
                return 'Server is temporarily unavailable. Please try again later.';
            default:
                return this.message || 'An unexpected error occurred. Please try again.';
        }
    }
}

/**
 * Sleep for a specified duration
 * @param {number} ms - Milliseconds to sleep
 */
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

/**
 * Calculate delay with exponential backoff and jitter
 * @param {number} attempt - Current attempt number (0-indexed)
 * @param {number} baseDelayMs - Base delay in milliseconds
 * @param {number} maxDelayMs - Maximum delay cap
 */
const calculateBackoffDelay = (attempt, baseDelayMs, maxDelayMs) => {
    const exponentialDelay = baseDelayMs * Math.pow(2, attempt);
    const jitter = Math.random() * 0.3 * exponentialDelay; // 0-30% jitter
    return Math.min(exponentialDelay + jitter, maxDelayMs);
};

/**
 * Get authentication headers with JWT token
 */
export const getAuthHeaders = () => {
    const token = localStorage.getItem('accessToken');
    return {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` })
    };
};

/**
 * Parse error response from API
 * @param {Response} response - Fetch Response object
 */
const parseErrorResponse = async (response) => {
    try {
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const data = await response.json();
            return {
                message: data.message || data.error || `Request failed with status ${response.status}`,
                code: data.code || data.errorCode || null,
                data: data
            };
        }
        const text = await response.text();
        return {
            message: text || `Request failed with status ${response.status}`,
            code: null,
            data: null
        };
    } catch {
        return {
            message: `Request failed with status ${response.status}`,
            code: null,
            data: null
        };
    }
};

/**
 * Make an API request with retry logic and error handling
 * @param {string} url - Request URL
 * @param {RequestInit} options - Fetch options
 * @param {Object} retryConfig - Retry configuration
 */
export const apiRequest = async (url, options = {}, retryConfig = {}) => {
    const config = { ...DEFAULT_RETRY_CONFIG, ...retryConfig };
    const { maxRetries, baseDelayMs, maxDelayMs, retryableStatuses } = config;

    // Merge headers with auth headers if not explicitly disabled
    const headers = options.skipAuth 
        ? { 'Content-Type': 'application/json', ...options.headers }
        : { ...getAuthHeaders(), ...options.headers };

    const fetchOptions = {
        ...options,
        headers
    };

    let lastError;

    for (let attempt = 0; attempt <= maxRetries; attempt++) {
        try {
            const response = await fetch(url, fetchOptions);

            if (response.ok) {
                // Handle 204 No Content
                if (response.status === 204) {
                    return null;
                }
                
                // Try to parse JSON, fallback to null for empty responses
                const text = await response.text();
                return text ? JSON.parse(text) : null;
            }

            // Parse error response
            const errorInfo = await parseErrorResponse(response);
            const isRetryable = retryableStatuses.includes(response.status);
            
            lastError = new ApiError(
                errorInfo.message,
                response.status,
                errorInfo.code,
                errorInfo.data,
                isRetryable
            );

            // Don't retry on non-retryable errors
            if (!isRetryable || attempt === maxRetries) {
                throw lastError;
            }

            // Calculate backoff delay and wait
            const delay = calculateBackoffDelay(attempt, baseDelayMs, maxDelayMs);
            console.warn(`API request failed (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${Math.round(delay)}ms...`);
            await sleep(delay);

        } catch (error) {
            // Handle network errors
            if (error instanceof TypeError && error.message.includes('fetch')) {
                lastError = new ApiError(
                    'Network error: Unable to connect to server',
                    0,
                    'NETWORK_ERROR',
                    null,
                    true
                );

                if (attempt < maxRetries) {
                    const delay = calculateBackoffDelay(attempt, baseDelayMs, maxDelayMs);
                    console.warn(`Network error (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${Math.round(delay)}ms...`);
                    await sleep(delay);
                    continue;
                }
            }

            // Re-throw ApiErrors and other errors
            if (error instanceof ApiError) {
                throw error;
            }

            throw lastError || error;
        }
    }

    throw lastError;
};

/**
 * Convenience methods for common HTTP verbs
 */
export const api = {
    get: (url, options = {}) => 
        apiRequest(url, { ...options, method: 'GET' }),

    post: (url, body, options = {}) => 
        apiRequest(url, { 
            ...options, 
            method: 'POST', 
            body: JSON.stringify(body) 
        }),

    put: (url, body, options = {}) => 
        apiRequest(url, { 
            ...options, 
            method: 'PUT', 
            body: JSON.stringify(body) 
        }),

    patch: (url, body, options = {}) => 
        apiRequest(url, { 
            ...options, 
            method: 'PATCH', 
            body: JSON.stringify(body) 
        }),

    delete: (url, options = {}) => 
        apiRequest(url, { ...options, method: 'DELETE' })
};

/**
 * Create a retry wrapper for existing async functions
 * @param {Function} fn - Async function to wrap
 * @param {Object} retryConfig - Retry configuration
 */
export const withRetry = (fn, retryConfig = {}) => {
    const config = { ...DEFAULT_RETRY_CONFIG, ...retryConfig };
    const { maxRetries, baseDelayMs, maxDelayMs } = config;

    return async (...args) => {
        let lastError;

        for (let attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return await fn(...args);
            } catch (error) {
                lastError = error;

                // Check if error is retryable
                const isRetryable = error instanceof ApiError 
                    ? error.isRetryable 
                    : true; // Default to retryable for unknown errors

                if (!isRetryable || attempt === maxRetries) {
                    throw error;
                }

                const delay = calculateBackoffDelay(attempt, baseDelayMs, maxDelayMs);
                console.warn(`Operation failed (attempt ${attempt + 1}/${maxRetries + 1}), retrying in ${Math.round(delay)}ms...`);
                await sleep(delay);
            }
        }

        throw lastError;
    };
};

export default api;
