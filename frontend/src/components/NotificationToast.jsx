import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNotifications } from '../context/NotificationContext';
import {
    X,
    CheckCircle,
    AlertCircle,
    Info,
    TrendingUp,
    TrendingDown,
    Wallet,
    ShoppingCart,
    Clock,
    XCircle,
    AlertTriangle,
    LogIn,
    Key,
    Shield
} from 'lucide-react';
import './NotificationToast.css';

// Get icon based on notification type
const getNotificationIcon = (type) => {
    const iconProps = { size: 20 };

    switch (type) {
        case 'ORDER_CREATED':
            return <ShoppingCart {...iconProps} className="toast-icon order" />;
        case 'ORDER_FILLED':
        case 'ORDER_PARTIALLY_FILLED':
            return <CheckCircle {...iconProps} className="toast-icon success" />;
        case 'ORDER_CANCELLED':
            return <XCircle {...iconProps} className="toast-icon warning" />;
        case 'ORDER_REJECTED':
            return <AlertCircle {...iconProps} className="toast-icon error" />;
        case 'ORDER_EXPIRED':
            return <Clock {...iconProps} className="toast-icon warning" />;
        case 'TRADE_EXECUTED':
            return <TrendingUp {...iconProps} className="toast-icon success" />;
        case 'FUNDS_DEPOSITED':
            return <Wallet {...iconProps} className="toast-icon success" />;
        case 'FUNDS_WITHDRAWN':
            return <Wallet {...iconProps} className="toast-icon info" />;
        case 'FUNDS_LOCKED':
        case 'FUNDS_RELEASED':
            return <Wallet {...iconProps} className="toast-icon info" />;
        case 'USER_LOGIN':
            return <LogIn {...iconProps} className="toast-icon security" />;
        case 'USER_PASSWORD_CHANGED':
            return <Key {...iconProps} className="toast-icon security" />;
        case 'USER_STATUS_CHANGED':
            return <Shield {...iconProps} className="toast-icon security" />;
        default:
            return <Info {...iconProps} className="toast-icon info" />;
    }
};

// Get toast style class based on type
const getToastClass = (type) => {
    if (type?.includes('FILLED') || type === 'TRADE_EXECUTED' || type === 'FUNDS_DEPOSITED') {
        return 'toast-success';
    }
    if (type === 'ORDER_REJECTED' || type === 'ORDER_CANCELLED') {
        return 'toast-error';
    }
    if (type === 'ORDER_EXPIRED') {
        return 'toast-warning';
    }
    if (type?.startsWith('USER_')) {
        return 'toast-security';
    }
    return 'toast-info';
};

const NotificationToast = () => {
    const { toasts, dismissToast } = useNotifications();

    return (
        <div className="toast-container">
            <AnimatePresence mode="popLayout">
                {toasts.map((toast) => (
                    <motion.div
                        key={toast.id}
                        className={`toast ${getToastClass(toast.type)}`}
                        initial={{ opacity: 0, x: 100, scale: 0.8 }}
                        animate={{ opacity: 1, x: 0, scale: 1 }}
                        exit={{ opacity: 0, x: 100, scale: 0.8 }}
                        transition={{
                            type: "spring",
                            stiffness: 400,
                            damping: 25
                        }}
                        layout
                    >
                        <div className="toast-content">
                            <div className="toast-icon-wrapper">
                                {getNotificationIcon(toast.type)}
                            </div>
                            <div className="toast-body">
                                <div className="toast-title">{toast.title}</div>
                                <div className="toast-message">{toast.message}</div>
                            </div>
                            <button
                                className="toast-close"
                                onClick={() => dismissToast(toast.id)}
                                aria-label="Close notification"
                            >
                                <X size={16} />
                            </button>
                        </div>
                        <motion.div
                            className="toast-progress"
                            initial={{ width: "100%" }}
                            animate={{ width: "0%" }}
                            transition={{ duration: 5, ease: "linear" }}
                        />
                    </motion.div>
                ))}
            </AnimatePresence>
        </div>
    );
};

export default NotificationToast;
