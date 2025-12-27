import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Navbar from './Navbar';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';

// Wrapper to provide required contexts
const TestWrapper = ({ children }) => (
    <ThemeProvider>
        <NotificationProvider>
            {children}
        </NotificationProvider>
    </ThemeProvider>
);

describe('Navbar', () => {
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

    it('renders logo and brand name', () => {
        render(
            <TestWrapper>
                <Navbar />
            </TestWrapper>
        );

        expect(screen.getByText('Winvestco')).toBeInTheDocument();
    });

    it('renders navigation links', () => {
        render(
            <TestWrapper>
                <Navbar />
            </TestWrapper>
        );

        expect(screen.getByText('Indices')).toBeInTheDocument();
        expect(screen.getByText('Market Data')).toBeInTheDocument();
        expect(screen.getByText('Portfolio')).toBeInTheDocument();
        expect(screen.getByText('Orders')).toBeInTheDocument();
        expect(screen.getByText('Trades')).toBeInTheDocument();
        expect(screen.getByText('Wallet')).toBeInTheDocument();
    });

    it('shows login buttons when not logged in', () => {
        render(
            <TestWrapper>
                <Navbar />
            </TestWrapper>
        );

        expect(screen.getByText('Log In')).toBeInTheDocument();
        expect(screen.getByText('Get Started')).toBeInTheDocument();
    });

    it('shows Profile link when logged in', () => {
        // Pre-set localStorage with access token
        localStorageStore.accessToken = 'fake-token';

        render(
            <TestWrapper>
                <Navbar />
            </TestWrapper>
        );

        // Profile link should be visible when logged in (in desktop menu)
        const profileLinks = screen.getAllByText('Profile');
        expect(profileLinks.length).toBeGreaterThan(0);
    });

    it('calls onLogin callback when Log In is clicked', () => {
        const onLoginMock = vi.fn();

        render(
            <TestWrapper>
                <Navbar onLogin={onLoginMock} />
            </TestWrapper>
        );

        fireEvent.click(screen.getByText('Log In'));
        expect(onLoginMock).toHaveBeenCalledTimes(1);
    });

    it('calls onLogin callback when Get Started is clicked', () => {
        const onLoginMock = vi.fn();

        render(
            <TestWrapper>
                <Navbar onLogin={onLoginMock} />
            </TestWrapper>
        );

        fireEvent.click(screen.getByText('Get Started'));
        expect(onLoginMock).toHaveBeenCalledTimes(1);
    });

    it('renders theme toggle button', () => {
        render(
            <TestWrapper>
                <Navbar />
            </TestWrapper>
        );

        // Should have a theme toggle button with a title
        const themeToggle = screen.getByTitle(/Switch to/);
        expect(themeToggle).toBeInTheDocument();
    });

    it('toggles mobile menu when menu button is clicked', () => {
        render(
            <TestWrapper>
                <Navbar />
            </TestWrapper>
        );

        // Find the mobile menu button by icon button class
        const mobileMenuBtn = document.querySelector('.mobile-menu-btn button');
        expect(mobileMenuBtn).toBeInTheDocument();

        // Mobile menu should not be visible initially
        expect(document.querySelector('.mobile-menu')).not.toBeInTheDocument();

        // Click to open
        fireEvent.click(mobileMenuBtn);
        expect(document.querySelector('.mobile-menu')).toBeInTheDocument();

        // Click to close
        fireEvent.click(mobileMenuBtn);
        expect(document.querySelector('.mobile-menu')).not.toBeInTheDocument();
    });
});

