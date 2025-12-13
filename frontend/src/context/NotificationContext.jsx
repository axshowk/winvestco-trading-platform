import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';

const NotificationContext = createContext();

// API base URL
const API_BASE_URL = 'http://localhost:8090';

export const useNotifications = () => {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications must be used within a NotificationProvider');
    }
    return context;
};

export const NotificationProvider = ({ children }) => {
    const [notifications, setNotifications] = useState([]);
    const [toasts, setToasts] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [isConnected, setIsConnected] = useState(false);
    const wsRef = useRef(null);
    const toastIdRef = useRef(0);

    // Get user ID from localStorage (set after login)
    const getUserId = () => {
        const user = localStorage.getItem('user');
        if (user) {
            try {
                return JSON.parse(user).id;
            } catch {
                return null;
            }
        }
        return null;
    };

    // Connect to WebSocket
    const connectWebSocket = useCallback(() => {
        const userId = getUserId();
        if (!userId) return;

        const wsUrl = `ws://localhost:8091/ws/notifications?userId=${userId}`;

        try {
            wsRef.current = new WebSocket(wsUrl);

            wsRef.current.onopen = () => {
                console.log('🔔 Notification WebSocket connected');
                setIsConnected(true);
            };

            wsRef.current.onmessage = (event) => {
                try {
                    const notification = JSON.parse(event.data);
                    if (notification.type === 'CONNECTED') {
                        console.log('WebSocket connection confirmed');
                        return;
                    }

                    // Add to notifications list
                    setNotifications(prev => [notification, ...prev]);
                    setUnreadCount(prev => prev + 1);

                    // Show toast
                    showToast(notification);
                } catch (e) {
                    console.error('Failed to parse notification:', e);
                }
            };

            wsRef.current.onclose = () => {
                console.log('🔔 Notification WebSocket closed');
                setIsConnected(false);
                // Attempt reconnect after 5 seconds
                setTimeout(connectWebSocket, 5000);
            };

            wsRef.current.onerror = (error) => {
                console.error('WebSocket error:', error);
                setIsConnected(false);
            };
        } catch (error) {
            console.error('Failed to connect WebSocket:', error);
        }
    }, []);

    // Disconnect WebSocket
    const disconnectWebSocket = useCallback(() => {
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
        }
    }, []);

    // Show toast notification (minimum 3 seconds)
    const showToast = useCallback((notification) => {
        const id = ++toastIdRef.current;
        const toast = {
            id,
            ...notification,
            createdAt: new Date().toISOString()
        };

        setToasts(prev => [...prev, toast]);

        // Auto-dismiss after 5 seconds (minimum 3 as requested)
        setTimeout(() => {
            setToasts(prev => prev.filter(t => t.id !== id));
        }, 5000);
    }, []);

    // Manual dismiss toast
    const dismissToast = useCallback((id) => {
        setToasts(prev => prev.filter(t => t.id !== id));
    }, []);

    // Fetch notifications from API
    const fetchNotifications = useCallback(async () => {
        const userId = getUserId();
        if (!userId) return;

        try {
            const response = await fetch(`${API_BASE_URL}/api/v1/notifications`, {
                headers: {
                    'X-User-Id': userId.toString()
                }
            });
            if (response.ok) {
                const data = await response.json();
                setNotifications(data.content || []);
            }
        } catch (error) {
            console.error('Failed to fetch notifications:', error);
        }
    }, []);

    // Fetch unread count
    const fetchUnreadCount = useCallback(async () => {
        const userId = getUserId();
        if (!userId) return;

        try {
            const response = await fetch(`${API_BASE_URL}/api/v1/notifications/count`, {
                headers: {
                    'X-User-Id': userId.toString()
                }
            });
            if (response.ok) {
                const data = await response.json();
                setUnreadCount(data.unreadCount || 0);
            }
        } catch (error) {
            console.error('Failed to fetch unread count:', error);
        }
    }, []);

    // Mark notification as read
    const markAsRead = useCallback(async (notificationId) => {
        const userId = getUserId();
        if (!userId) return;

        try {
            const response = await fetch(`${API_BASE_URL}/api/v1/notifications/${notificationId}/read`, {
                method: 'PATCH',
                headers: {
                    'X-User-Id': userId.toString()
                }
            });
            if (response.ok) {
                setNotifications(prev =>
                    prev.map(n => n.id === notificationId ? { ...n, status: 'READ' } : n)
                );
                setUnreadCount(prev => Math.max(0, prev - 1));
            }
        } catch (error) {
            console.error('Failed to mark as read:', error);
        }
    }, []);

    // Mark all as read
    const markAllAsRead = useCallback(async () => {
        const userId = getUserId();
        if (!userId) return;

        try {
            const response = await fetch(`${API_BASE_URL}/api/v1/notifications/read-all`, {
                method: 'PATCH',
                headers: {
                    'X-User-Id': userId.toString()
                }
            });
            if (response.ok) {
                setNotifications(prev => prev.map(n => ({ ...n, status: 'READ' })));
                setUnreadCount(0);
            }
        } catch (error) {
            console.error('Failed to mark all as read:', error);
        }
    }, []);

    // Demo notification for testing
    const showDemoNotification = useCallback((type = 'ORDER_FILLED') => {
        const demoNotifications = {
            ORDER_FILLED: {
                type: 'ORDER_FILLED',
                title: 'Order Executed',
                message: 'Your order for 10 shares of RELIANCE has been executed at ₹2,485.50',
                data: { symbol: 'RELIANCE', quantity: 10, price: 2485.50 }
            },
            ORDER_CREATED: {
                type: 'ORDER_CREATED',
                title: 'Order Placed',
                message: 'Your BUY order for 5 shares of TCS at ₹3,520.00 has been placed.',
                data: { symbol: 'TCS', quantity: 5, price: 3520 }
            },
            FUNDS_DEPOSITED: {
                type: 'FUNDS_DEPOSITED',
                title: 'Deposit Confirmed',
                message: '₹50,000 has been credited to your wallet via UPI. New balance: ₹1,50,000',
                data: { amount: 50000 }
            },
            TRADE_EXECUTED: {
                type: 'TRADE_EXECUTED',
                title: 'Trade Executed',
                message: 'You bought 15 shares of INFY at ₹1,456.25. Total: ₹21,843.75',
                data: { symbol: 'INFY', quantity: 15, price: 1456.25 }
            }
        };

        const notification = demoNotifications[type] || demoNotifications.ORDER_FILLED;
        showToast(notification);
        setNotifications(prev => [{ ...notification, id: Date.now(), createdAt: new Date().toISOString() }, ...prev]);
        setUnreadCount(prev => prev + 1);
    }, [showToast]);

    // Connect on mount if logged in
    useEffect(() => {
        const userId = getUserId();
        if (userId) {
            connectWebSocket();
            fetchNotifications();
            fetchUnreadCount();
        }

        return () => {
            disconnectWebSocket();
        };
    }, [connectWebSocket, disconnectWebSocket, fetchNotifications, fetchUnreadCount]);

    const value = {
        notifications,
        toasts,
        unreadCount,
        isConnected,
        showToast,
        dismissToast,
        fetchNotifications,
        markAsRead,
        markAllAsRead,
        showDemoNotification,
        connectWebSocket,
        disconnectWebSocket
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
};

export default NotificationContext;
