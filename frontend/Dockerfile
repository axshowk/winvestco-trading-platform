# =============================================================================
# WINVESTCO Trading Platform - Frontend
# Optimized Multi-Stage Docker Build
# =============================================================================

# Stage 1: Build
FROM node:20-alpine AS build

# OCI Labels (will be inherited)
LABEL org.opencontainers.image.title="WINVESTCO Frontend" \
    org.opencontainers.image.description="React 19 Trading Platform UI" \
    org.opencontainers.image.vendor="WINVESTCO" \
    org.opencontainers.image.source="https://github.com/axshowk/winvestco-trading-platform"

WORKDIR /app

# Copy package files first for dependency caching
COPY package*.json ./

# Install dependencies (cached layer)
RUN npm ci --only=production=false

# Copy source code
COPY . .

# Build for production
RUN npm run build

# Stage 2: Production (minimal nginx image)
FROM nginx:alpine

# OCI Labels
LABEL org.opencontainers.image.title="WINVESTCO Frontend" \
    org.opencontainers.image.description="React 19 Trading Platform UI" \
    org.opencontainers.image.vendor="WINVESTCO" \
    org.opencontainers.image.source="https://github.com/axshowk/winvestco-trading-platform"

# Create non-root user
RUN addgroup -S winvestco && adduser -S appuser -G winvestco

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Set proper permissions
RUN chown -R appuser:winvestco /usr/share/nginx/html && \
    chown -R appuser:winvestco /var/cache/nginx && \
    chown -R appuser:winvestco /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R appuser:winvestco /var/run/nginx.pid

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget -q -O - http://localhost:80/ || exit 1

# Run nginx
CMD ["nginx", "-g", "daemon off;"]
