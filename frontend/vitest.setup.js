import '@testing-library/jest-dom';
import { cleanup } from '@testing-library/react';
import { afterEach, beforeEach, vi } from 'vitest';

// Cleanup after each test
afterEach(() => {
    cleanup();
    localStorage.clear();
});

// Mock localStorage
const localStorageMock = {
    getItem: vi.fn(),
    setItem: vi.fn(),
    removeItem: vi.fn(),
    clear: vi.fn(),
    length: 0,
    key: vi.fn(),
};

beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks();

    // Setup localStorage mock with default behavior
    Object.defineProperty(window, 'localStorage', {
        value: localStorageMock,
        writable: true,
    });
});

// Mock WebSocket
class MockWebSocket {
    constructor() {
        this.onopen = null;
        this.onclose = null;
        this.onmessage = null;
        this.onerror = null;
    }
    close() { }
    send() { }
}

global.WebSocket = MockWebSocket;
