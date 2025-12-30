import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../context/NotificationContext';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import {
    Bell, Check, Trash2, ShoppingCart, CheckCircle,
    XCircle, AlertCircle, Clock, TrendingUp, Wallet,
    LogIn, Key, Shield, Info, LogOut
} from 'lucide-react';
import './Notifications.css';

// Reuse icon logic
const getNotificationIcon = (type) => {
    const iconProps = { size: 20 };
    switch (type) {
        case 'ORDER_CREATED': return <ShoppingCart {...iconProps} className="order" />;
        case 'ORDER_FILLED':
        case 'ORDER_PARTIALLY_FILLED': return <CheckCircle {...iconProps} className="success" />;
        case 'ORDER_CANCELLED': return <XCircle {...iconProps} className="warning" />;
        case 'ORDER_REJECTED': return <AlertCircle {...iconProps} className="error" />;
        case 'ORDER_EXPIRED': return <Clock {...iconProps} className="warning" />;
        case 'TRADE_EXECUTED': return <TrendingUp {...iconProps} className="success" />;
        case 'FUNDS_DEPOSITED':
        case 'FUNDS_WITHDRAWN':
        case 'FUNDS_LOCKED':
        case 'FUNDS_RELEASED': return <Wallet {...iconProps} className="info" />;
        case 'USER_LOGIN': return <LogIn {...iconProps} className="security" />;
        case 'USER_PASSWORD_CHANGED': return <Key {...iconProps} className="security" />;
        case 'USER_STATUS_CHANGED': return <Shield {...iconProps} className="security" />;
        default: return <Info {...iconProps} className="info" />;
    }
};

const formatTimeAgo = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
};

const Notifications = () => {
    const navigate = useNavigate();
    const {
        notifications,
        unreadCount,
        markAsRead,
        markAllAsRead,
    } = useNotifications();

    // Local auth state
    const [isAuthenticated, setIsAuthenticated] = React.useState(!!localStorage.getItem('accessToken'));

    // Check auth
    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        setIsAuthenticated(!!token);
    }, []);

    const handleNotificationClick = (id, status) => {
        if (status === 'UNREAD') {
            markAsRead(id);
        }
    };

    if (!isAuthenticated) {
        return (
            <div className="page-wrapper">
                <Navbar onLogin={() => navigate('/login')} />
                <div className="notifications-container">
                    <div className="auth-required-state">
                        <LogOut size={64} style={{ color: '#6366f1', opacity: 0.5 }} />
                        <h2>Login to View Notifications</h2>
                        <p>Keep track of your orders, trades, and account activity</p>
                        <button className="login-btn" onClick={() => navigate('/login')}>
                            Login to Continue
                        </button>
                    </div>
                </div>
                <Footer />
            </div>
        );
    }

    return (
        <div className="page-wrapper">
            <Navbar onLogin={() => navigate('/login')} />
            <div className="notifications-container">
                {/* Header */}
                <div className="notifications-header">
                    <div>
                        <h1>
                            <Bell size={28} />
                            Notifications
                            <span className="notification-count">({unreadCount} unread)</span>
                        </h1>
                    </div>
                    <div className="header-actions">
                        {unreadCount > 0 && (
                            <button className="action-btn" onClick={markAllAsRead}>
                                <Check size={18} />
                                Mark all read
                            </button>
                        )}
                    </div>
                </div>

                {/* List */}
                <div className="notifications-list">
                    {notifications.length === 0 ? (
                        <div className="empty-state">
                            <Bell size={48} />
                            <h3>No Notifications</h3>
                            <p>You're all caught up! We'll notify you when something important happens.</p>
                        </div>
                    ) : (
                        notifications.map(notification => (
                            <div
                                key={notification.id}
                                className={`notification-card ${notification.status === 'UNREAD' ? 'unread' : ''}`}
                                onClick={() => handleNotificationClick(notification.id, notification.status)}
                            >
                                <div className={`notification-icon ${notification.type.split('_')[0].toLowerCase()}`}>
                                    {getNotificationIcon(notification.type)}
                                </div>
                                <div className="notification-content">
                                    <div className="notification-top">
                                        <span className="notification-title">{notification.title}</span>
                                        <span className="notification-time">{formatTimeAgo(notification.createdAt)}</span>
                                    </div>
                                    <p className="notification-message">{notification.message}</p>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
            <Footer />
        </div>
    );
};

export default Notifications;
