import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ErrorState from './ErrorState';

describe('ErrorState', () => {
    describe('default variant', () => {
        it('renders error icon and title', () => {
            render(<ErrorState />);

            expect(screen.getByText('Something went wrong')).toBeInTheDocument();
        });

        it('renders provided title', () => {
            render(<ErrorState title="Custom Title" />);

            expect(screen.getByText('Custom Title')).toBeInTheDocument();
        });

        it('renders provided message', () => {
            render(<ErrorState message="Custom error message" />);

            expect(screen.getByText('Custom error message')).toBeInTheDocument();
        });

        it('calls onRetry when retry button is clicked', () => {
            const onRetryMock = vi.fn();
            render(<ErrorState onRetry={onRetryMock} />);

            fireEvent.click(screen.getByText('Try Again'));
            expect(onRetryMock).toHaveBeenCalledTimes(1);
        });

        it('hides retry button when showRetry is false', () => {
            render(<ErrorState onRetry={() => { }} showRetry={false} />);

            expect(screen.queryByText('Try Again')).not.toBeInTheDocument();
        });

        it('shows back button when showBack is true', () => {
            const onBackMock = vi.fn();
            render(<ErrorState onBack={onBackMock} showBack={true} />);

            expect(screen.getByText('Go Back')).toBeInTheDocument();
        });

        it('calls onBack when back button is clicked', () => {
            const onBackMock = vi.fn();
            render(<ErrorState onBack={onBackMock} showBack={true} />);

            fireEvent.click(screen.getByText('Go Back'));
            expect(onBackMock).toHaveBeenCalledTimes(1);
        });

        it('uses custom retryLabel', () => {
            render(<ErrorState onRetry={() => { }} retryLabel="Refresh" />);

            expect(screen.getByText('Refresh')).toBeInTheDocument();
        });

        it('uses custom backLabel', () => {
            render(<ErrorState onBack={() => { }} showBack={true} backLabel="Return" />);

            expect(screen.getByText('Return')).toBeInTheDocument();
        });
    });

    describe('network error detection', () => {
        it('shows connection error for status 0', () => {
            render(<ErrorState error={{ status: 0 }} />);

            expect(screen.getByText('Connection Error')).toBeInTheDocument();
            expect(screen.getByText(/Unable to connect/)).toBeInTheDocument();
        });

        it('shows connection error for NETWORK_ERROR code', () => {
            render(<ErrorState error={{ code: 'NETWORK_ERROR' }} />);

            expect(screen.getByText('Connection Error')).toBeInTheDocument();
        });
    });

    describe('server error detection', () => {
        it('shows server error for status 500', () => {
            render(<ErrorState error={{ status: 500 }} />);

            expect(screen.getByText('Server Error')).toBeInTheDocument();
            expect(screen.getByText(/servers are experiencing issues/)).toBeInTheDocument();
        });

        it('shows server error for status 503', () => {
            render(<ErrorState error={{ status: 503 }} />);

            expect(screen.getByText('Server Error')).toBeInTheDocument();
        });
    });

    describe('compact variant', () => {
        it('renders compact error state', () => {
            render(<ErrorState variant="compact" message="Compact error" />);

            expect(screen.getByText('Compact error')).toBeInTheDocument();
        });

        it('shows retry link in compact mode', () => {
            render(<ErrorState variant="compact" onRetry={() => { }} />);

            expect(screen.getByText('Retry')).toBeInTheDocument();
        });
    });

    describe('inline variant', () => {
        it('renders inline error state', () => {
            render(<ErrorState variant="inline" title="Inline Error" />);

            expect(screen.getByText('Inline Error')).toBeInTheDocument();
        });

        it('renders retry button in inline mode', () => {
            render(<ErrorState variant="inline" onRetry={() => { }} retryLabel="Refresh Now" />);

            expect(screen.getByText('Refresh Now')).toBeInTheDocument();
        });
    });

    describe('retryable hint', () => {
        it('shows retryable hint when error.isRetryable is true', () => {
            render(<ErrorState error={{ isRetryable: true }} />);

            expect(screen.getByText(/This issue may be temporary/)).toBeInTheDocument();
        });

        it('hides retryable hint when error.isRetryable is false', () => {
            render(<ErrorState error={{ isRetryable: false }} />);

            expect(screen.queryByText(/This issue may be temporary/)).not.toBeInTheDocument();
        });
    });

    describe('custom className', () => {
        it('applies custom className', () => {
            const { container } = render(<ErrorState className="custom-class" />);

            expect(container.querySelector('.custom-class')).toBeInTheDocument();
        });
    });
});
