import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Custom hook for managing WebSocket connection to portfolio service.
 * Provides real-time portfolio updates.
 */
const usePortfolioWebSocket = (userId, onMessage, enabled = true) => {
    const [isConnected, setIsConnected] = useState(false);
    const [connectionStatus, setConnectionStatus] = useState('disconnected');
    const wsRef = useRef(null);
    const reconnectTimeoutRef = useRef(null);
    const reconnectAttemptsRef = useRef(0);
    const maxReconnectAttempts = 5;
    const baseReconnectDelay = 1000; // 1 second

    const connect = useCallback(() => {
        if (!enabled || !userId) {
            return;
        }

        // Determine WebSocket URL based on current location
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        const wsUrl = `${protocol}//${host}/ws/portfolio?userId=${userId}`;

        try {
            setConnectionStatus('connecting');
            const ws = new WebSocket(wsUrl);
            wsRef.current = ws;

            ws.onopen = () => {
                console.log('Portfolio WebSocket connected');
                setIsConnected(true);
                setConnectionStatus('connected');
                reconnectAttemptsRef.current = 0;

                // Send subscription message
                ws.send(JSON.stringify({ type: 'SUBSCRIBE' }));
            };

            ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    console.log('Portfolio WebSocket message:', data);

                    // Handle system messages internally
                    if (data.type === 'CONNECTED' || data.type === 'SUBSCRIBED') {
                        console.log('Portfolio service:', data.message);
                        return;
                    }

                    if (data.type === 'PONG') {
                        return; // Heartbeat response
                    }

                    // Forward other messages to handler
                    if (onMessage) {
                        onMessage(data);
                    }
                } catch (err) {
                    console.error('Error parsing WebSocket message:', err);
                }
            };

            ws.onclose = (event) => {
                console.log('Portfolio WebSocket closed:', event.code, event.reason);
                setIsConnected(false);
                wsRef.current = null;

                // Attempt to reconnect if not a normal closure
                if (event.code !== 1000 && reconnectAttemptsRef.current < maxReconnectAttempts) {
                    setConnectionStatus('reconnecting');
                    const delay = baseReconnectDelay * Math.pow(2, reconnectAttemptsRef.current);
                    reconnectAttemptsRef.current++;
                    
                    console.log(`Reconnecting in ${delay}ms (attempt ${reconnectAttemptsRef.current})`);
                    reconnectTimeoutRef.current = setTimeout(connect, delay);
                } else {
                    setConnectionStatus('disconnected');
                }
            };

            ws.onerror = (error) => {
                console.error('Portfolio WebSocket error:', error);
                setConnectionStatus('error');
            };

        } catch (err) {
            console.error('Failed to create WebSocket:', err);
            setConnectionStatus('error');
        }
    }, [userId, enabled, onMessage]);

    const disconnect = useCallback(() => {
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
            reconnectTimeoutRef.current = null;
        }

        if (wsRef.current) {
            wsRef.current.close(1000, 'User disconnected');
            wsRef.current = null;
        }

        setIsConnected(false);
        setConnectionStatus('disconnected');
        reconnectAttemptsRef.current = 0;
    }, []);

    const sendMessage = useCallback((message) => {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            const payload = typeof message === 'string' ? message : JSON.stringify(message);
            wsRef.current.send(payload);
        }
    }, []);

    // Heartbeat to keep connection alive
    useEffect(() => {
        if (!isConnected) return;

        const pingInterval = setInterval(() => {
            sendMessage({ type: 'PING' });
        }, 30000); // Ping every 30 seconds

        return () => clearInterval(pingInterval);
    }, [isConnected, sendMessage]);

    // Connect on mount, disconnect on unmount
    useEffect(() => {
        if (enabled && userId) {
            connect();
        }

        return () => {
            disconnect();
        };
    }, [enabled, userId, connect, disconnect]);

    return {
        isConnected,
        connectionStatus,
        sendMessage,
        reconnect: connect,
        disconnect
    };
};

export default usePortfolioWebSocket;
