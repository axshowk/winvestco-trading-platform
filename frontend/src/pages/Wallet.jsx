import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Wallet as WalletIcon, Plus, Minus, RefreshCw,
    ArrowDownCircle, ArrowUpCircle, Clock, CheckCircle,
    XCircle, Lock, Unlock, ChevronLeft, ChevronRight,
    TrendingUp, TrendingDown, AlertTriangle, Loader2
} from 'lucide-react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import {
    getWallet, getTransactions,
    initiateWithdrawal, getTypeDisplay, getStatusDisplay,
    formatCurrency, formatDateTime, TransactionType, TransactionStatus
} from '../services/fundsApi';
import { initiatePayment, verifyPayment, openRazorpayCheckout } from '../services/paymentApi';
import './Wallet.css';

const Wallet = () => {
    const navigate = useNavigate();

    // Wallet state
    const [wallet, setWallet] = useState(null);
    const [loadingWallet, setLoadingWallet] = useState(true);
    const [walletError, setWalletError] = useState(null);

    // Transactions state
    const [transactions, setTransactions] = useState([]);
    const [loadingTransactions, setLoadingTransactions] = useState(true);
    const [transactionsError, setTransactionsError] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    // Deposit modal state
    const [showDepositModal, setShowDepositModal] = useState(false);
    const [depositAmount, setDepositAmount] = useState('');
    const [depositDescription, setDepositDescription] = useState('');
    const [depositing, setDepositing] = useState(false);
    const [depositError, setDepositError] = useState(null);
    const [depositSuccess, setDepositSuccess] = useState(null);

    // Withdraw modal state
    const [showWithdrawModal, setShowWithdrawModal] = useState(false);
    const [withdrawAmount, setWithdrawAmount] = useState('');
    const [withdrawDescription, setWithdrawDescription] = useState('');
    const [withdrawing, setWithdrawing] = useState(false);
    const [withdrawError, setWithdrawError] = useState(null);
    const [withdrawSuccess, setWithdrawSuccess] = useState(null);

    // Refresh state
    const [refreshing, setRefreshing] = useState(false);

    const isAuthenticated = () => !!localStorage.getItem('accessToken');

    const fetchWallet = useCallback(async () => {
        if (!isAuthenticated()) return;
        try {
            setLoadingWallet(true);
            const data = await getWallet();
            setWallet(data);
            setWalletError(null);
        } catch (err) {
            console.error('Error fetching wallet:', err);
            setWalletError(err.message);
        } finally {
            setLoadingWallet(false);
        }
    }, []);

    const fetchTransactions = useCallback(async (page = 0) => {
        if (!isAuthenticated()) return;
        try {
            setLoadingTransactions(true);
            const data = await getTransactions(page, 10);
            setTransactions(data.content || []);
            setTotalPages(data.totalPages || 1);
            setCurrentPage(page);
            setTransactionsError(null);
        } catch (err) {
            console.error('Error fetching transactions:', err);
            setTransactionsError(err.message);
        } finally {
            setLoadingTransactions(false);
        }
    }, []);

    useEffect(() => {
        if (!isAuthenticated()) {
            navigate('/login');
            return;
        }
        fetchWallet();
        fetchTransactions(0);
    }, [fetchWallet, fetchTransactions, navigate]);

    const handleRefresh = async () => {
        setRefreshing(true);
        await Promise.all([fetchWallet(), fetchTransactions(currentPage)]);
        setRefreshing(false);
    };

    // Deposit handlers
    const openDepositModal = () => {
        setShowDepositModal(true);
        setDepositAmount('');
        setDepositDescription('');
        setDepositError(null);
        setDepositSuccess(null);
    };

    const closeDepositModal = () => {
        setShowDepositModal(false);
        setDepositAmount('');
        setDepositDescription('');
        setDepositError(null);
        setDepositSuccess(null);
    };

    const handleDeposit = async () => {
        const amount = parseFloat(depositAmount);
        if (isNaN(amount) || amount <= 0) {
            setDepositError('Please enter a valid amount');
            return;
        }
        if (amount < 100) {
            setDepositError('Minimum deposit amount is ₹100');
            return;
        }

        try {
            setDepositing(true);
            setDepositError(null);
            
            // Step 1: Initiate payment - creates Razorpay order
            const orderData = await initiatePayment(amount, depositDescription);
            
            // Step 2: Open Razorpay checkout modal
            const paymentResult = await openRazorpayCheckout(orderData, {
                name: wallet?.userName || '',
                email: wallet?.userEmail || '',
                phone: wallet?.userPhone || ''
            });
            
            // Step 3: Verify payment after successful checkout
            await verifyPayment(
                paymentResult.razorpayOrderId,
                paymentResult.razorpayPaymentId,
                paymentResult.razorpaySignature
            );

            setDepositSuccess(`Successfully deposited ${formatCurrency(amount)}`);
            await Promise.all([fetchWallet(), fetchTransactions(0)]);

            setTimeout(() => {
                closeDepositModal();
            }, 1500);
        } catch (err) {
            console.error('Deposit error:', err);
            // Don't show error for user cancellation
            if (err.message === 'Payment cancelled by user') {
                setDepositError('Payment was cancelled');
            } else {
                setDepositError(err.message);
            }
        } finally {
            setDepositing(false);
        }
    };

    // Withdraw handlers
    const openWithdrawModal = () => {
        setShowWithdrawModal(true);
        setWithdrawAmount('');
        setWithdrawDescription('');
        setWithdrawError(null);
        setWithdrawSuccess(null);
    };

    const closeWithdrawModal = () => {
        setShowWithdrawModal(false);
        setWithdrawAmount('');
        setWithdrawDescription('');
        setWithdrawError(null);
        setWithdrawSuccess(null);
    };

    const handleWithdraw = async () => {
        const amount = parseFloat(withdrawAmount);
        if (isNaN(amount) || amount <= 0) {
            setWithdrawError('Please enter a valid amount');
            return;
        }
        if (amount < 100) {
            setWithdrawError('Minimum withdrawal amount is ₹100');
            return;
        }
        if (wallet && amount > wallet.availableBalance) {
            setWithdrawError(`Insufficient balance. Available: ${formatCurrency(wallet.availableBalance)}`);
            return;
        }

        try {
            setWithdrawing(true);
            setWithdrawError(null);
            await initiateWithdrawal(amount, withdrawDescription);

            setWithdrawSuccess(`Withdrawal of ${formatCurrency(amount)} initiated`);
            await Promise.all([fetchWallet(), fetchTransactions(0)]);

            setTimeout(() => {
                closeWithdrawModal();
            }, 1500);
        } catch (err) {
            console.error('Withdrawal error:', err);
            setWithdrawError(err.message);
        } finally {
            setWithdrawing(false);
        }
    };

    const setPresetAmount = (amount, setter) => {
        setter(amount.toString());
    };

    const getTransactionIcon = (type) => {
        const iconProps = { size: 18 };
        switch (type) {
            case TransactionType.DEPOSIT:
                return <ArrowDownCircle {...iconProps} className="tx-icon deposit" />;
            case TransactionType.WITHDRAWAL:
                return <ArrowUpCircle {...iconProps} className="tx-icon withdrawal" />;
            case TransactionType.TRADE_DEBIT:
                return <TrendingDown {...iconProps} className="tx-icon debit" />;
            case TransactionType.TRADE_CREDIT:
                return <TrendingUp {...iconProps} className="tx-icon credit" />;
            case TransactionType.LOCK:
                return <Lock {...iconProps} className="tx-icon lock" />;
            case TransactionType.UNLOCK:
                return <Unlock {...iconProps} className="tx-icon unlock" />;
            default:
                return <Clock {...iconProps} className="tx-icon" />;
        }
    };

    if (!isAuthenticated()) {
        return null;
    }

    return (
        <div className="page-wrapper">
            <Navbar onLogin={() => navigate('/login')} />
            <div className="wallet-container">
                {/* Header */}
                <div className="wallet-header">
                    <div className="header-title">
                        <WalletIcon size={28} />
                        <h1>Wallet</h1>
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

                {/* Balance Cards */}
                {loadingWallet ? (
                    <div className="loading-state">
                        <div className="loading-spinner"></div>
                        <p>Loading wallet...</p>
                    </div>
                ) : walletError ? (
                    <div className="error-state">
                        <XCircle size={32} />
                        <p>{walletError}</p>
                        <button onClick={fetchWallet} className="retry-btn">Try Again</button>
                    </div>
                ) : wallet && (
                    <>
                        <div className="balance-cards">
                            <div className="balance-card main glass">
                                <div className="balance-icon total">
                                    <WalletIcon size={28} />
                                </div>
                                <div className="balance-content">
                                    <span className="balance-label">Total Balance</span>
                                    <span className="balance-value main-value">
                                        {formatCurrency(wallet.totalBalance)}
                                    </span>
                                </div>
                            </div>
                            <div className="balance-card glass">
                                <div className="balance-icon available">
                                    <CheckCircle size={24} />
                                </div>
                                <div className="balance-content">
                                    <span className="balance-label">Available</span>
                                    <span className="balance-value">
                                        {formatCurrency(wallet.availableBalance)}
                                    </span>
                                </div>
                            </div>
                            <div className="balance-card glass">
                                <div className="balance-icon locked">
                                    <Lock size={24} />
                                </div>
                                <div className="balance-content">
                                    <span className="balance-label">Locked</span>
                                    <span className="balance-value">
                                        {formatCurrency(wallet.lockedBalance)}
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Quick Actions */}
                        <div className="quick-actions">
                            <button className="action-btn deposit" onClick={openDepositModal}>
                                <Plus size={20} />
                                <span>Add Funds</span>
                            </button>
                            <button className="action-btn withdraw" onClick={openWithdrawModal}>
                                <Minus size={20} />
                                <span>Withdraw</span>
                            </button>
                        </div>
                    </>
                )}

                {/* Transaction History */}
                <div className="transactions-section">
                    <div className="section-header">
                        <Clock size={20} />
                        <h2>Transaction History</h2>
                    </div>

                    {loadingTransactions ? (
                        <div className="loading-state">
                            <div className="loading-spinner"></div>
                            <p>Loading transactions...</p>
                        </div>
                    ) : transactionsError ? (
                        <div className="error-state">
                            <XCircle size={32} />
                            <p>{transactionsError}</p>
                            <button onClick={() => fetchTransactions(currentPage)} className="retry-btn">
                                Try Again
                            </button>
                        </div>
                    ) : transactions.length === 0 ? (
                        <div className="empty-state glass">
                            <Clock size={48} />
                            <h3>No Transactions Yet</h3>
                            <p>Your transaction history will appear here.</p>
                            <button onClick={openDepositModal} className="primary-btn">
                                Make Your First Deposit
                            </button>
                        </div>
                    ) : (
                        <>
                            <div className="transactions-table-container glass">
                                <table className="transactions-table">
                                    <thead>
                                        <tr>
                                            <th>Type</th>
                                            <th>Amount</th>
                                            <th>Status</th>
                                            <th>Description</th>
                                            <th>Date</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {transactions.map((tx) => {
                                            const typeInfo = getTypeDisplay(tx.transactionType);
                                            const statusInfo = getStatusDisplay(tx.status);
                                            const isCredit = [TransactionType.DEPOSIT, TransactionType.TRADE_CREDIT, TransactionType.UNLOCK].includes(tx.transactionType);

                                            return (
                                                <tr key={tx.id}>
                                                    <td>
                                                        <div className="tx-type">
                                                            {getTransactionIcon(tx.transactionType)}
                                                            <span>{typeInfo.label}</span>
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <span className={`tx-amount ${isCredit ? 'credit' : 'debit'}`}>
                                                            {isCredit ? '+' : '-'}{formatCurrency(tx.amount)}
                                                        </span>
                                                    </td>
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
                                                    <td className="tx-description">
                                                        {tx.description || '-'}
                                                    </td>
                                                    <td className="tx-date">
                                                        {formatDateTime(tx.createdAt)}
                                                    </td>
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
                                        onClick={() => fetchTransactions(currentPage - 1)}
                                        disabled={currentPage === 0}
                                    >
                                        <ChevronLeft size={18} />
                                    </button>
                                    <span className="page-info">
                                        Page {currentPage + 1} of {totalPages}
                                    </span>
                                    <button
                                        className="page-btn"
                                        onClick={() => fetchTransactions(currentPage + 1)}
                                        disabled={currentPage >= totalPages - 1}
                                    >
                                        <ChevronRight size={18} />
                                    </button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>

            {/* Deposit Modal */}
            {showDepositModal && (
                <div className="modal-overlay" onClick={closeDepositModal}>
                    <div className="modal glass" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3><Plus size={20} /> Add Funds</h3>
                            <button className="close-btn" onClick={closeDepositModal}>×</button>
                        </div>
                        <div className="modal-body">
                            {depositSuccess ? (
                                <div className="success-message">
                                    <CheckCircle size={48} />
                                    <p>{depositSuccess}</p>
                                </div>
                            ) : (
                                <>
                                    <div className="form-group">
                                        <label>Amount (₹)</label>
                                        <input
                                            type="number"
                                            value={depositAmount}
                                            onChange={(e) => setDepositAmount(e.target.value)}
                                            placeholder="Enter amount"
                                            min="100"
                                            step="100"
                                        />
                                    </div>
                                    <div className="preset-amounts">
                                        {[1000, 5000, 10000, 25000, 50000].map(amt => (
                                            <button
                                                key={amt}
                                                className="preset-btn"
                                                onClick={() => setPresetAmount(amt, setDepositAmount)}
                                            >
                                                ₹{(amt / 1000).toFixed(0)}K
                                            </button>
                                        ))}
                                    </div>
                                    <div className="form-group">
                                        <label>Description (Optional)</label>
                                        <input
                                            type="text"
                                            value={depositDescription}
                                            onChange={(e) => setDepositDescription(e.target.value)}
                                            placeholder="e.g., Monthly investment"
                                        />
                                    </div>
                                    {depositError && (
                                        <div className="error-message">
                                            <AlertTriangle size={16} />
                                            {depositError}
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                        {!depositSuccess && (
                            <div className="modal-footer">
                                <button className="btn-secondary" onClick={closeDepositModal}>
                                    Cancel
                                </button>
                                <button
                                    className="btn-primary"
                                    onClick={handleDeposit}
                                    disabled={depositing || !depositAmount}
                                >
                                    {depositing ? (
                                        <>
                                            <Loader2 size={16} className="spinning" />
                                            Processing...
                                        </>
                                    ) : (
                                        <>Add Funds</>
                                    )}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Withdraw Modal */}
            {showWithdrawModal && (
                <div className="modal-overlay" onClick={closeWithdrawModal}>
                    <div className="modal glass" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3><Minus size={20} /> Withdraw Funds</h3>
                            <button className="close-btn" onClick={closeWithdrawModal}>×</button>
                        </div>
                        <div className="modal-body">
                            {withdrawSuccess ? (
                                <div className="success-message">
                                    <CheckCircle size={48} />
                                    <p>{withdrawSuccess}</p>
                                </div>
                            ) : (
                                <>
                                    <div className="available-balance-info">
                                        <span>Available Balance:</span>
                                        <strong>{wallet ? formatCurrency(wallet.availableBalance) : '₹0.00'}</strong>
                                    </div>
                                    <div className="form-group">
                                        <label>Amount (₹)</label>
                                        <input
                                            type="number"
                                            value={withdrawAmount}
                                            onChange={(e) => setWithdrawAmount(e.target.value)}
                                            placeholder="Enter amount"
                                            min="100"
                                            step="100"
                                            max={wallet?.availableBalance || 0}
                                        />
                                    </div>
                                    <div className="preset-amounts">
                                        {[1000, 5000, 10000, 25000].map(amt => (
                                            <button
                                                key={amt}
                                                className="preset-btn"
                                                onClick={() => setPresetAmount(amt, setWithdrawAmount)}
                                                disabled={wallet && amt > wallet.availableBalance}
                                            >
                                                ₹{(amt / 1000).toFixed(0)}K
                                            </button>
                                        ))}
                                        <button
                                            className="preset-btn max"
                                            onClick={() => wallet && setPresetAmount(Math.floor(wallet.availableBalance), setWithdrawAmount)}
                                            disabled={!wallet || wallet.availableBalance < 100}
                                        >
                                            MAX
                                        </button>
                                    </div>
                                    <div className="form-group">
                                        <label>Description (Optional)</label>
                                        <input
                                            type="text"
                                            value={withdrawDescription}
                                            onChange={(e) => setWithdrawDescription(e.target.value)}
                                            placeholder="e.g., Bank transfer"
                                        />
                                    </div>
                                    {withdrawError && (
                                        <div className="error-message">
                                            <AlertTriangle size={16} />
                                            {withdrawError}
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                        {!withdrawSuccess && (
                            <div className="modal-footer">
                                <button className="btn-secondary" onClick={closeWithdrawModal}>
                                    Cancel
                                </button>
                                <button
                                    className="btn-primary withdraw"
                                    onClick={handleWithdraw}
                                    disabled={withdrawing || !withdrawAmount}
                                >
                                    {withdrawing ? (
                                        <>
                                            <Loader2 size={16} className="spinning" />
                                            Processing...
                                        </>
                                    ) : (
                                        <>Withdraw</>
                                    )}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
};

export default Wallet;
