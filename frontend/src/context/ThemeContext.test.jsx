import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, render, screen } from '@testing-library/react';
import { ThemeProvider, useTheme } from './ThemeContext';

describe('ThemeContext', () => {
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

    describe('useTheme hook', () => {
        it('throws error when used outside ThemeProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });

            expect(() => {
                renderHook(() => useTheme());
            }).toThrow('useTheme must be used within a ThemeProvider');

            consoleSpy.mockRestore();
        });

        it('provides default dark theme', () => {
            const { result } = renderHook(() => useTheme(), {
                wrapper: ThemeProvider,
            });

            expect(result.current.theme).toBe('dark');
            expect(result.current.isDarkMode).toBe(true);
        });

        it('toggles theme from dark to light', () => {
            const { result } = renderHook(() => useTheme(), {
                wrapper: ThemeProvider,
            });

            act(() => {
                result.current.toggleTheme();
            });

            expect(result.current.theme).toBe('light');
            expect(result.current.isDarkMode).toBe(false);
        });

        it('toggles theme from light to dark', () => {
            // Pre-set localStorage to light theme
            localStorageStore.theme = 'light';

            const { result } = renderHook(() => useTheme(), {
                wrapper: ThemeProvider,
            });

            // Initial state should be light (from localStorage)
            expect(result.current.theme).toBe('light');

            act(() => {
                result.current.toggleTheme();
            });

            expect(result.current.theme).toBe('dark');
            expect(result.current.isDarkMode).toBe(true);
        });

        it('persists theme to localStorage', () => {
            const { result } = renderHook(() => useTheme(), {
                wrapper: ThemeProvider,
            });

            act(() => {
                result.current.toggleTheme();
            });

            expect(window.localStorage.setItem).toHaveBeenCalledWith('theme', 'light');
        });

        it('reads initial theme from localStorage', () => {
            // Pre-set localStorage to light theme
            localStorageStore.theme = 'light';

            const { result } = renderHook(() => useTheme(), {
                wrapper: ThemeProvider,
            });

            expect(result.current.theme).toBe('light');
            expect(result.current.isDarkMode).toBe(false);
        });
    });

    describe('ThemeProvider', () => {
        it('renders children', () => {
            render(
                <ThemeProvider>
                    <div data-testid="child">Child Content</div>
                </ThemeProvider>
            );

            expect(screen.getByTestId('child')).toBeInTheDocument();
        });

        it('adds light-theme class to body when theme is light', () => {
            // Pre-set localStorage to light theme
            localStorageStore.theme = 'light';

            render(
                <ThemeProvider>
                    <div>Test</div>
                </ThemeProvider>
            );

            expect(document.body.classList.contains('light-theme')).toBe(true);
        });
    });
});
