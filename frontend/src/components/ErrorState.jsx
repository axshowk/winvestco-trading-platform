import React from 'react';
import { AlertTriangle, WifiOff, RefreshCw, ArrowLeft, ServerCrash } from 'lucide-react';
import './ErrorState.css';

/**
 * Reusable Error State Component
 * Displays user-friendly error messages with recovery options.
 */
const ErrorState = ({
    error,
    title,
    message,
    onRetry,
    onBack,
    showRetry = true,
    showBack = false,
    retryLabel = 'Try Again',
    backLabel = 'Go Back',
    variant = 'default', // 'default', 'inline', 'compact'
    className = ''
}) => {
    // Determine error type and icon
    const getErrorDetails = () => {
        if (error?.status === 0 || error?.code === 'NETWORK_ERROR') {
            return {
                icon: WifiOff,
                defaultTitle: 'Connection Error',
                defaultMessage: 'Unable to connect to the server. Please check your internet connection.',
                iconColor: 'warning'
            };
        }

        if (error?.status >= 500) {
            return {
                icon: ServerCrash,
                defaultTitle: 'Server Error',
                defaultMessage: 'Our servers are experiencing issues. Please try again later.',
                iconColor: 'error'
            };
        }

        return {
            icon: AlertTriangle,
            defaultTitle: 'Something went wrong',
            defaultMessage: error?.getUserMessage?.() || error?.message || 'An unexpected error occurred.',
            iconColor: 'error'
        };
    };

    const { icon: Icon, defaultTitle, defaultMessage, iconColor } = getErrorDetails();

    const displayTitle = title || defaultTitle;
    const displayMessage = message || defaultMessage;

    if (variant === 'compact') {
        return (
            <div className={`error-state-compact ${className}`}>
                <Icon size={16} className={`error-icon ${iconColor}`} />
                <span className="error-text">{displayMessage}</span>
                {showRetry && onRetry && (
                    <button className="error-retry-link" onClick={onRetry}>
                        Retry
                    </button>
                )}
            </div>
        );
    }

    if (variant === 'inline') {
        return (
            <div className={`error-state-inline glass ${className}`}>
                <div className="error-inline-content">
                    <Icon size={20} className={`error-icon ${iconColor}`} />
                    <div className="error-inline-text">
                        <span className="error-inline-title">{displayTitle}</span>
                        <span className="error-inline-message">{displayMessage}</span>
                    </div>
                </div>
                {showRetry && onRetry && (
                    <button className="error-inline-retry" onClick={onRetry}>
                        <RefreshCw size={14} />
                        {retryLabel}
                    </button>
                )}
            </div>
        );
    }

    // Default full variant
    return (
        <div className={`error-state glass ${className}`}>
            <div className={`error-icon-container ${iconColor}`}>
                <Icon size={48} />
            </div>
            <h3 className="error-title">{displayTitle}</h3>
            <p className="error-message">{displayMessage}</p>

            <div className="error-actions">
                {showRetry && onRetry && (
                    <button className="error-btn primary" onClick={onRetry}>
                        <RefreshCw size={16} />
                        {retryLabel}
                    </button>
                )}
                {showBack && onBack && (
                    <button className="error-btn secondary" onClick={onBack}>
                        <ArrowLeft size={16} />
                        {backLabel}
                    </button>
                )}
            </div>

            {error?.isRetryable && (
                <p className="error-hint">
                    This issue may be temporary. Please try again in a moment.
                </p>
            )}
        </div>
    );
};

export default ErrorState;
