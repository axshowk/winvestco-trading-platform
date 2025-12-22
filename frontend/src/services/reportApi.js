import API from './api';

/**
 * Report API service for generating and downloading reports
 */

// Request a new report
export const requestReport = async (type, format, fromDate, toDate) => {
    const response = await API.post('/v1/reports', {
        type,
        format,
        fromDate: fromDate?.toISOString(),
        toDate: toDate?.toISOString()
    });
    return response.data;
};

// Get report by ID
export const getReport = async (reportId) => {
    const response = await API.get(`/v1/reports/${reportId}`);
    return response.data;
};

// Get all reports for the user
export const getUserReports = async (page = 0, size = 10) => {
    const response = await API.get('/v1/reports', {
        params: { page, size, sort: 'requestedAt,desc' }
    });
    return response.data;
};

// Download a report
export const downloadReport = async (reportId, fileName) => {
    const response = await API.get(`/v1/reports/${reportId}/download`, {
        responseType: 'blob'
    });

    // Create download link
    const url = window.URL.createObjectURL(new Blob([response.data]));
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
    await API.delete(`/v1/reports/${reportId}`);
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
