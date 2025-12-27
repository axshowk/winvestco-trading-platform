import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import NotificationBell from './NotificationBell';
import { NotificationProvider } from '../context/NotificationContext';

// Wrapper to provide required context
const TestWrapper = ({ children }) => (
    <NotificationProvider>
        {children}
    </NotificationProvider>
);

describe('NotificationBell', () => {
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

    it('renders bell icon button', () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        expect(screen.getByRole('button', { name: /notifications/i })).toBeInTheDocument();
    });

    it('opens dropdown when bell is clicked', () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        expect(screen.getByText('Notifications')).toBeInTheDocument();
    });

    it('closes dropdown when bell is clicked again', async () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });

        // Open
        fireEvent.click(bellButton);
        expect(screen.getByText('Notifications')).toBeInTheDocument();

        // Close
        fireEvent.click(bellButton);
        // Wait for AnimatePresence to complete exit animation
        await waitFor(() => {
            expect(screen.queryByText('Notifications')).not.toBeInTheDocument();
        }, { timeout: 1000 });
    });

    it('shows empty state when no notifications', () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        expect(screen.getByText('No notifications yet')).toBeInTheDocument();
        expect(screen.getByText(/We'll notify you when something/)).toBeInTheDocument();
    });

    it('shows demo notification button in empty state', () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        expect(screen.getByText('Show Demo Notification')).toBeInTheDocument();
    });

    it('clicking demo notification adds a notification', async () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        const demoButton = screen.getByText('Show Demo Notification');
        fireEvent.click(demoButton);

        // Wait for notification to appear
        await waitFor(() => {
            expect(screen.getByText('Order Executed')).toBeInTheDocument();
        });
    });

    it('shows notification badge when unread count > 0', async () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        // Open and add a demo notification
        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        const demoButton = screen.getByText('Show Demo Notification');
        fireEvent.click(demoButton);

        // Wait and check for badge
        await waitFor(() => {
            const badge = document.querySelector('.notification-badge');
            expect(badge).toBeInTheDocument();
        });
    });

    it('closes dropdown when clicking outside', async () => {
        render(
            <TestWrapper>
                <div data-testid="outside">Outside</div>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        // Dropdown should be open
        expect(screen.getByText('Notifications')).toBeInTheDocument();

        // Click outside
        fireEvent.mouseDown(screen.getByTestId('outside'));

        // Dropdown should close (wait for animation)
        await waitFor(() => {
            expect(screen.queryByText('No notifications yet')).not.toBeInTheDocument();
        });
    });

    it('renders notification list with items after demo notification', async () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        // Add demo notification
        const demoButton = screen.getByText('Show Demo Notification');
        fireEvent.click(demoButton);

        // Check notification details
        await waitFor(() => {
            expect(screen.getByText('Order Executed')).toBeInTheDocument();
            expect(screen.getByText(/Your order for 10 shares/)).toBeInTheDocument();
        });
    });

    it('shows mark all read button when has unread notifications', async () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        // Add demo notification
        const demoButton = screen.getByText('Show Demo Notification');
        fireEvent.click(demoButton);

        await waitFor(() => {
            expect(screen.getByText('Mark all read')).toBeInTheDocument();
        });
    });

    it('shows view all button when has notifications', async () => {
        render(
            <TestWrapper>
                <NotificationBell />
            </TestWrapper>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        // Add demo notification
        const demoButton = screen.getByText('Show Demo Notification');
        fireEvent.click(demoButton);

        await waitFor(() => {
            expect(screen.getByText('View all notifications')).toBeInTheDocument();
        });
    });
});

// Unit tests for helper functions (extracted for testing)
describe('NotificationBell helpers', () => {
    // Test the formatTimeAgo function by checking rendered time values
    it('renders time ago for notifications', async () => {
        render(
            <NotificationProvider>
                <NotificationBell />
            </NotificationProvider>
        );

        const bellButton = screen.getByRole('button', { name: /notifications/i });
        fireEvent.click(bellButton);

        const demoButton = screen.getByText('Show Demo Notification');
        fireEvent.click(demoButton);

        // The notification time should show "Just now" or similar
        await waitFor(() => {
            const timeElement = document.querySelector('.notification-item-time');
            expect(timeElement).toBeInTheDocument();
        });
    });
});
