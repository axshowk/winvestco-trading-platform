import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  FileText, Download, Trash2, RefreshCw, Calendar, 
  FileSpreadsheet, FileDown, Clock, CheckCircle, XCircle, Loader2
} from 'lucide-react';
import {
  requestReport,
  getUserReports,
  downloadReport,
  deleteReport,
  REPORT_TYPES,
  REPORT_FORMATS,
  REPORT_STATUS
} from '../services/reportApi';
import './Reports.css';

const Reports = () => {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [selectedType, setSelectedType] = useState('P_AND_L');
  const [selectedFormat, setSelectedFormat] = useState('PDF');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [error, setError] = useState(null);

  const fetchReports = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getUserReports();
      setReports(data.content || []);
    } catch (err) {
      setError('Failed to fetch reports');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchReports();
    // Poll for status updates every 5 seconds if any report is pending/processing
    const interval = setInterval(() => {
      if (reports.some(r => r.status === 'PENDING' || r.status === 'PROCESSING')) {
        fetchReports();
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [fetchReports, reports]);

  const handleGenerateReport = async (e) => {
    e.preventDefault();
    setGenerating(true);
    setError(null);

    try {
      const from = fromDate ? new Date(fromDate) : null;
      const to = toDate ? new Date(toDate) : null;
      await requestReport(selectedType, selectedFormat, from, to);
      fetchReports();
    } catch (err) {
      setError('Failed to generate report');
      console.error(err);
    } finally {
      setGenerating(false);
    }
  };

  const handleDownload = async (report) => {
    try {
      const extension = report.format.toLowerCase();
      const fileName = `${report.reportType.toLowerCase()}-${report.reportId.slice(0, 8)}.${extension}`;
      await downloadReport(report.reportId, fileName);
    } catch (err) {
      setError('Failed to download report');
      console.error(err);
    }
  };

  const handleDelete = async (reportId) => {
    if (!window.confirm('Are you sure you want to delete this report?')) return;
    
    try {
      await deleteReport(reportId);
      fetchReports();
    } catch (err) {
      setError('Failed to delete report');
      console.error(err);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'PENDING': return <Clock className="status-icon pending" />;
      case 'PROCESSING': return <Loader2 className="status-icon processing spin" />;
      case 'COMPLETED': return <CheckCircle className="status-icon completed" />;
      case 'FAILED': return <XCircle className="status-icon failed" />;
      default: return null;
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="reports-container">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="reports-header"
      >
        <div className="header-content">
          <FileText className="header-icon" />
          <div>
            <h1>Reports</h1>
            <p>Generate and download P&L, tax, and transaction reports</p>
          </div>
        </div>
      </motion.div>

      {error && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="error-banner"
        >
          {error}
          <button onClick={() => setError(null)}>×</button>
        </motion.div>
      )}

      {/* Report Generation Form */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="generate-card"
      >
        <h2>Generate New Report</h2>
        <form onSubmit={handleGenerateReport} className="generate-form">
          <div className="form-row">
            <div className="form-group">
              <label>Report Type</label>
              <select
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
              >
                {REPORT_TYPES.map(type => (
                  <option key={type.value} value={type.value}>
                    {type.icon} {type.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Format</label>
              <select
                value={selectedFormat}
                onChange={(e) => setSelectedFormat(e.target.value)}
              >
                {REPORT_FORMATS.map(format => (
                  <option key={format.value} value={format.value}>
                    {format.icon} {format.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label><Calendar size={16} /> From Date</label>
              <input
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label><Calendar size={16} /> To Date</label>
              <input
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
              />
            </div>
          </div>

          <button 
            type="submit" 
            className="generate-btn"
            disabled={generating}
          >
            {generating ? (
              <>
                <Loader2 className="spin" size={18} />
                Generating...
              </>
            ) : (
              <>
                <FileDown size={18} />
                Generate Report
              </>
            )}
          </button>
        </form>
      </motion.div>

      {/* Reports List */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="reports-list-card"
      >
        <div className="list-header">
          <h2>Your Reports</h2>
          <button onClick={fetchReports} className="refresh-btn" disabled={loading}>
            <RefreshCw className={loading ? 'spin' : ''} size={18} />
          </button>
        </div>

        {loading && reports.length === 0 ? (
          <div className="loading-state">
            <Loader2 className="spin" size={32} />
            <p>Loading reports...</p>
          </div>
        ) : reports.length === 0 ? (
          <div className="empty-state">
            <FileSpreadsheet size={48} />
            <p>No reports generated yet</p>
            <span>Generate your first report above</span>
          </div>
        ) : (
          <div className="reports-table">
            <div className="table-header">
              <span>Type</span>
              <span>Format</span>
              <span>Status</span>
              <span>Requested</span>
              <span>Actions</span>
            </div>
            <AnimatePresence>
              {reports.map((report) => (
                <motion.div
                  key={report.reportId}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  className="table-row"
                >
                  <span className="report-type">
                    {REPORT_TYPES.find(t => t.value === report.reportType)?.icon}
                    {REPORT_TYPES.find(t => t.value === report.reportType)?.label || report.reportType}
                  </span>
                  <span className="report-format">{report.format}</span>
                  <span className={`report-status ${report.status.toLowerCase()}`}>
                    {getStatusIcon(report.status)}
                    {REPORT_STATUS[report.status]?.label || report.status}
                  </span>
                  <span className="report-date">{formatDate(report.requestedAt)}</span>
                  <span className="report-actions">
                    {report.status === 'COMPLETED' && (
                      <button
                        onClick={() => handleDownload(report)}
                        className="action-btn download"
                        title="Download"
                      >
                        <Download size={16} />
                      </button>
                    )}
                    <button
                      onClick={() => handleDelete(report.reportId)}
                      className="action-btn delete"
                      title="Delete"
                    >
                      <Trash2 size={16} />
                    </button>
                  </span>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        )}
      </motion.div>
    </div>
  );
};

export default Reports;
