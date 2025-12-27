import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { NotificationProvider, useNotifications } from './NotificationContext';

describe('NotificationContext', () => {
    let localStorageStore;
    let originalLocalStorage;

    beforeEach(() => {
        // Store original localStorage
        originalLocalStorage = window.localStorage;

        // Create a fresh store for each test
        localStorageStore = {};

        // Create mock localStorage
        const mockLocalStorage = {
            getItem: vi.fn((key) => localStorageStore[key] ?? null),
            setItem: vi.fn((key, value) => {
                localStorageStore[key] = value;
            }),
            removeItem: vi.fn((key) => {
                delete localStorageStore[key];
            }),
            clear: vi.fn(() => {
                localStorageStore = {};
            }),
        };

        Object.defineProperty(window, 'localStorage', {
            value: mockLocalStorage,
            writable: true,
        });
    });

    afterEach(() => {
        // Restore original localStorage
        Object.defineProperty(window, 'localStorage', {
            value: originalLocalStorage,
            writable: true,
        });
    });

    describe('useNotifications hook', () => {
        it('throws error when used outside NotificationProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });

            expect(() => {
                renderHook(() => useNotifications());
            }).toThrow('useNotifications must be used within a NotificationProvider');

            consoleSpy.mockRestore();
        });

        it('provides initial state with empty notifications', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            expect(result.current.notifications).toEqual([]);
            expect(result.current.unreadCount).toBe(0);
            expect(result.current.toasts).toEqual([]);
        });

        it('provides isConnected as false initially', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            expect(result.current.isConnected).toBe(false);
        });

        it('showDemoNotification adds a notification', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showDemoNotification('ORDER_FILLED');
            });

            expect(result.current.notifications.length).toBe(1);
            expect(result.current.notifications[0].type).toBe('ORDER_FILLED');
            expect(result.current.unreadCount).toBe(1);
        });

        it('showDemoNotification adds toast with correct data', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showDemoNotification('FUNDS_DEPOSITED');
            });

            expect(result.current.toasts.length).toBe(1);
            expect(result.current.toasts[0].type).toBe('FUNDS_DEPOSITED');
            expect(result.current.toasts[0].title).toBe('Deposit Confirmed');
        });

        it('dismissToast removes a toast', async () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showDemoNotification('ORDER_CREATED');
            });

            const toastId = result.current.toasts[0].id;

            act(() => {
                result.current.dismissToast(toastId);
            });

            expect(result.current.toasts.length).toBe(0);
        });

        it('exposes all required functions', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            expect(typeof result.current.showToast).toBe('function');
            expect(typeof result.current.dismissToast).toBe('function');
            expect(typeof result.current.fetchNotifications).toBe('function');
            expect(typeof result.current.markAsRead).toBe('function');
            expect(typeof result.current.markAllAsRead).toBe('function');
            expect(typeof result.current.showDemoNotification).toBe('function');
            expect(typeof result.current.connectWebSocket).toBe('function');
            expect(typeof result.current.disconnectWebSocket).toBe('function');
        });
    });

    describe('demo notification types', () => {
        it('ORDER_CREATED notification has correct content', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showDemoNotification('ORDER_CREATED');
            });

            expect(result.current.notifications[0].title).toBe('Order Placed');
            expect(result.current.notifications[0].data.symbol).toBe('TCS');
        });

        it('TRADE_EXECUTED notification has correct content', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showDemoNotification('TRADE_EXECUTED');
            });

            expect(result.current.notifications[0].title).toBe('Trade Executed');
            expect(result.current.notifications[0].data.symbol).toBe('INFY');
        });

        it('unknown type falls back to ORDER_FILLED', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showDemoNotification('UNKNOWN_TYPE');
            });

            expect(result.current.notifications[0].type).toBe('ORDER_FILLED');
        });
    });

    describe('showToast function', () => {
        it('adds custom toast', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showToast({
                    type: 'CUSTOM',
                    title: 'Custom Title',
                    message: 'Custom message',
                });
            });

            expect(result.current.toasts.length).toBe(1);
            expect(result.current.toasts[0].title).toBe('Custom Title');
        });

        it('toast has createdAt timestamp', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showToast({
                    type: 'TEST',
                    title: 'Test',
                    message: 'Test message',
                });
            });

            expect(result.current.toasts[0].createdAt).toBeDefined();
        });
    });

    describe('multiple notifications', () => {
        it('adds multiple notifications correctly', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showDemoNotification('ORDER_FILLED');
                result.current.showDemoNotification('ORDER_CREATED');
                result.current.showDemoNotification('FUNDS_DEPOSITED');
            });

            expect(result.current.notifications.length).toBe(3);
            expect(result.current.unreadCount).toBe(3);
        });

        it('notifications are added in reverse order (newest first)', () => {
            const { result } = renderHook(() => useNotifications(), {
                wrapper: NotificationProvider,
            });

            act(() => {
                result.current.showDemoNotification('ORDER_FILLED');
            });

            act(() => {
                result.current.showDemoNotification('ORDER_CREATED');
            });

            // First notification should be the most recent (ORDER_CREATED)
            expect(result.current.notifications[0].type).toBe('ORDER_CREATED');
            expect(result.current.notifications[1].type).toBe('ORDER_FILLED');
        });
    });
});

