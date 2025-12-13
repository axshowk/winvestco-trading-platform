import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNotifications } from '../context/NotificationContext';
import {
    Bell,
    X,
    CheckCircle,
    AlertCircle,
    Info,
    TrendingUp,
    Wallet,
    ShoppingCart,
    Clock,
    XCircle,
    LogIn,
    Key,
    Shield,
    Check,
    ChevronRight
} from 'lucide-react';
import './NotificationBell.css';

// Get icon based on notification type
const getNotificationIcon = (type) => {
    const iconProps = { size: 16 };

    switch (type) {
        case 'ORDER_CREATED':
            return <ShoppingCart {...iconProps} className="notif-icon order" />;
        case 'ORDER_FILLED':
        case 'ORDER_PARTIALLY_FILLED':
            return <CheckCircle {...iconProps} className="notif-icon success" />;
        case 'ORDER_CANCELLED':
            return <XCircle {...iconProps} className="notif-icon warning" />;
        case 'ORDER_REJECTED':
            return <AlertCircle {...iconProps} className="notif-icon error" />;
        case 'ORDER_EXPIRED':
            return <Clock {...iconProps} className="notif-icon warning" />;
        case 'TRADE_EXECUTED':
            return <TrendingUp {...iconProps} className="notif-icon success" />;
        case 'FUNDS_DEPOSITED':
        case 'FUNDS_WITHDRAWN':
        case 'FUNDS_LOCKED':
        case 'FUNDS_RELEASED':
            return <Wallet {...iconProps} className="notif-icon info" />;
        case 'USER_LOGIN':
            return <LogIn {...iconProps} className="notif-icon security" />;
        case 'USER_PASSWORD_CHANGED':
            return <Key {...iconProps} className="notif-icon security" />;
        case 'USER_STATUS_CHANGED':
            return <Shield {...iconProps} className="notif-icon security" />;
        default:
            return <Info {...iconProps} className="notif-icon info" />;
    }
};

// Format time ago
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

const NotificationBell = () => {
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef(null);
    const {
        notifications,
        unreadCount,
        markAsRead,
        markAllAsRead,
        showDemoNotification
    } = useNotifications();

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleNotificationClick = (notification) => {
        if (notification.status === 'UNREAD') {
            markAsRead(notification.id);
        }
        // Could navigate to relevant page based on notification type
    };

    return (
        <div className="notification-bell-wrapper" ref={dropdownRef}>
            {/* Bell Button */}
            <button
                className={`notification-bell-btn ${unreadCount > 0 ? 'has-unread' : ''}`}
                onClick={() => setIsOpen(!isOpen)}
                aria-label={`Notifications ${unreadCount > 0 ? `(${unreadCount} unread)` : ''}`}
            >
                <Bell size={20} />
                {unreadCount > 0 && (
                    <motion.span
                        className="notification-badge"
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", stiffness: 500, damping: 25 }}
                    >
                        {unreadCount > 99 ? '99+' : unreadCount}
                    </motion.span>
                )}
            </button>

            {/* Dropdown */}
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        className="notification-dropdown"
                        initial={{ opacity: 0, y: -10, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: -10, scale: 0.95 }}
                        transition={{ duration: 0.2 }}
                    >
                        {/* Header */}
                        <div className="notification-header">
                            <h3>Notifications</h3>
                            <div className="notification-header-actions">
                                {unreadCount > 0 && (
                                    <button
                                        className="mark-all-read-btn"
                                        onClick={markAllAsRead}
                                    >
                                        <Check size={14} />
                                        Mark all read
                                    </button>
                                )}
                            </div>
                        </div>

                        {/* Notification List */}
                        <div className="notification-list">
                            {notifications.length === 0 ? (
                                <div className="notification-empty">
                                    <Bell size={40} strokeWidth={1.5} />
                                    <p>No notifications yet</p>
                                    <span>We'll notify you when something important happens</span>
                                    {/* Demo button for testing */}
                                    <button
                                        className="demo-notification-btn"
                                        onClick={() => showDemoNotification('ORDER_FILLED')}
                                    >
                                        Show Demo Notification
                                    </button>
                                </div>
                            ) : (
                                notifications.slice(0, 10).map((notification) => (
                                    <motion.div
                                        key={notification.id}
                                        className={`notification-item ${notification.status === 'UNREAD' ? 'unread' : ''}`}
                                        onClick={() => handleNotificationClick(notification)}
                                        whileHover={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
                                    >
                                        <div className="notification-item-icon">
                                            {getNotificationIcon(notification.type)}
                                        </div>
                                        <div className="notification-item-content">
                                            <div className="notification-item-title">
                                                {notification.title}
                                            </div>
                                            <div className="notification-item-message">
                                                {notification.message}
                                            </div>
                                            <div className="notification-item-time">
                                                {formatTimeAgo(notification.createdAt)}
                                            </div>
                                        </div>
                                        {notification.status === 'UNREAD' && (
                                            <div className="notification-unread-dot" />
                                        )}
                                    </motion.div>
                                ))
                            )}
                        </div>

                        {/* Footer */}
                        {notifications.length > 0 && (
                            <div className="notification-footer">
                                <button className="view-all-btn">
                                    View all notifications
                                    <ChevronRight size={16} />
                                </button>
                            </div>
                        )}
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default NotificationBell;
