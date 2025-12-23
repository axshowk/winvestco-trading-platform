/**
 * Report API service for generating and downloading reports
 */

const API_BASE_URL = '/api/v1/reports';

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
    // For void responses (like delete), .json() might fail or be empty
    if (response.status === 204) return null;
    return response.json().catch(() => ({}));
};

// Request a new report
export const requestReport = async (type, format, fromDate, toDate) => {
    const response = await fetch(`${API_BASE_URL}`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
            type,
            format,
            fromDate: fromDate?.toISOString(),
            toDate: toDate?.toISOString()
        })
    });
    return handleResponse(response);
};

// Get report by ID
export const getReport = async (reportId) => {
    const response = await fetch(`${API_BASE_URL}/${reportId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

// Get all reports for the user
export const getUserReports = async (page = 0, size = 10) => {
    const params = new URLSearchParams({
        page,
        size,
        sort: 'requestedAt,desc'
    });
    const response = await fetch(`${API_BASE_URL}?${params}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

// Download a report
export const downloadReport = async (reportId, fileName) => {
    const response = await fetch(`${API_BASE_URL}/${reportId}/download`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        }
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Download failed with status ${response.status}`);
    }

    const blob = await response.blob();
    // Create download link
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName || `report-${reportId}`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
};

// Delete a report
export const deleteReport = async (reportId) => {
    const response = await fetch(`${API_BASE_URL}/${reportId}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
    });
    return handleResponse(response);
};

// Report types for UI
export const REPORT_TYPES = [
    { value: 'P_AND_L', label: 'Profit & Loss Statement', icon: '📊' },
    { value: 'TAX_REPORT', label: 'Tax Report (Capital Gains)', icon: '🧾' },
    { value: 'TRANSACTION_HISTORY', label: 'Transaction History', icon: '📋' },
    { value: 'HOLDINGS_SUMMARY', label: 'Holdings Summary', icon: '💼' },
    { value: 'TRADE_HISTORY', label: 'Trade History', icon: '📈' }
];

// Report formats for UI
export const REPORT_FORMATS = [
    { value: 'PDF', label: 'PDF', icon: '📄' },
    { value: 'EXCEL', label: 'Excel', icon: '📗' },
    { value: 'CSV', label: 'CSV', icon: '📑' }
];

// Report status mapping
export const REPORT_STATUS = {
    PENDING: { label: 'Pending', color: '#f59e0b', icon: '⏳' },
    PROCESSING: { label: 'Generating...', color: '#3b82f6', icon: '⚙️' },
    COMPLETED: { label: 'Ready', color: '#22c55e', icon: '✅' },
    FAILED: { label: 'Failed', color: '#ef4444', icon: '❌' },
    EXPIRED: { label: 'Expired', color: '#6b7280', icon: '⏰' }
};
