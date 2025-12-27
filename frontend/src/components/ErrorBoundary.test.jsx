import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ErrorBoundary from './ErrorBoundary';

// Component that throws an error for testing
const ThrowError = ({ shouldThrow }) => {
    if (shouldThrow) {
        throw new Error('Test error');
    }
    return <div>No error</div>;
};

describe('ErrorBoundary', () => {
    // Suppress console.error during tests as ErrorBoundary logs errors
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => { });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('renders children when there is no error', () => {
        render(
            <ErrorBoundary>
                <div data-testid="child">Child content</div>
            </ErrorBoundary>
        );

        expect(screen.getByTestId('child')).toBeInTheDocument();
        expect(screen.getByText('Child content')).toBeInTheDocument();
    });

    it('renders error UI when child throws', () => {
        render(
            <ErrorBoundary>
                <ThrowError shouldThrow={true} />
            </ErrorBoundary>
        );

        expect(screen.getByText('Something went wrong')).toBeInTheDocument();
        expect(screen.getByText(/An unexpected error occurred/)).toBeInTheDocument();
    });

    it('displays custom error message when provided', () => {
        render(
            <ErrorBoundary message="Custom error message">
                <ThrowError shouldThrow={true} />
            </ErrorBoundary>
        );

        expect(screen.getByText('Custom error message')).toBeInTheDocument();
    });

    it('renders Try Again button', () => {
        render(
            <ErrorBoundary>
                <ThrowError shouldThrow={true} />
            </ErrorBoundary>
        );

        expect(screen.getByText('Try Again')).toBeInTheDocument();
    });

    it('renders Go Home button', () => {
        render(
            <ErrorBoundary>
                <ThrowError shouldThrow={true} />
            </ErrorBoundary>
        );

        expect(screen.getByText('Go Home')).toBeInTheDocument();
    });

    it('resets error state when Try Again is clicked', () => {
        // Create a stateful test component to control throwing behavior
        let throwError = true;
        const ControlledThrowError = () => {
            if (throwError) {
                throw new Error('Test error');
            }
            return <div>No error</div>;
        };

        const { container } = render(
            <ErrorBoundary>
                <ControlledThrowError />
            </ErrorBoundary>
        );

        // Error UI should be visible
        expect(screen.getByText('Something went wrong')).toBeInTheDocument();

        // Now change the behavior so it won't throw on next render
        throwError = false;

        // Click Try Again - this should reset the error boundary state
        fireEvent.click(screen.getByText('Try Again'));

        // After reset, the child should re-render without error
        expect(screen.getByText('No error')).toBeInTheDocument();
    });

    it('calls onError callback when error occurs', () => {
        const onErrorMock = vi.fn();

        render(
            <ErrorBoundary onError={onErrorMock}>
                <ThrowError shouldThrow={true} />
            </ErrorBoundary>
        );

        expect(onErrorMock).toHaveBeenCalled();
        expect(onErrorMock.mock.calls[0][0]).toBeInstanceOf(Error);
    });

    it('uses custom fallback when provided', () => {
        const customFallback = ({ error, retry }) => (
            <div>
                <span>Custom error: {error?.message}</span>
                <button onClick={retry}>Custom retry</button>
            </div>
        );

        render(
            <ErrorBoundary fallback={customFallback}>
                <ThrowError shouldThrow={true} />
            </ErrorBoundary>
        );

        expect(screen.getByText('Custom error: Test error')).toBeInTheDocument();
        expect(screen.getByText('Custom retry')).toBeInTheDocument();
    });
});
