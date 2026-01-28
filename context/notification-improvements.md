# Notification Module Improvements

> **Context Document for AI Agents**
> Last Updated: 2026-01-28
> Status: Pending Implementation

This document outlines required improvements for the WinvestCo Trading Platform's notification module to achieve production-grade quality.

---

## Current Architecture Summary

| Component | Technology | Location |
|-----------|------------|----------|
| Backend Service | Spring Boot + WebSocket | `backend/notification-service/` |
| Message Broker | RabbitMQ | Event listeners in `messaging/` package |
| Database | PostgreSQL | Flyway migrations in `resources/db/migration/` |
| Frontend | React + Framer Motion | `frontend/src/components/Notification*.jsx` |

### Key Files
- [NotificationService.java](file:///e:/winvestco-trading-platform/backend/notification-service/src/main/java/in/winvestco/notification_service/service/NotificationService.java)
- [WebSocketNotificationService.java](file:///e:/winvestco-trading-platform/backend/notification-service/src/main/java/in/winvestco/notification_service/service/WebSocketNotificationService.java)
- [NotificationController.java](file:///e:/winvestco-trading-platform/backend/notification-service/src/main/java/in/winvestco/notification_service/controller/NotificationController.java)
- [NotificationContext.jsx](file:///e:/winvestco-trading-platform/frontend/src/context/NotificationContext.jsx)

---

## Required Improvements

### 🔴 Priority 1: Critical for Production

#### 1.1 Multi-Channel Notification Delivery ✅ COMPLETED

**Problem:** Current implementation only supports WebSocket delivery, which requires an active browser session.

**Status:** ✅ Implemented on 2026-01-28

**Implemented Components:**
- [x] Create `DeliveryChannel` enum: `WEBSOCKET`, `PUSH`, `EMAIL`, `SMS`
- [x] Add `NotificationChannel` entity for user channel preferences
- [x] Implement `PushNotificationService` using Firebase Cloud Messaging (FCM)
- [x] Implement `EmailNotificationService` using SendGrid or AWS SES
- [x] Implement `SmsNotificationService` using Twilio for critical alerts
- [x] Create `NotificationDeliveryStrategy` to route notifications to appropriate channels
- [x] Add `NotificationPriority` enum for delivery channel selection
- [x] Create `NotificationChannelService` for preference management
- [x] Create `NotificationChannelController` REST API
- [x] Database migration V2 for notification_channels table
- [x] Configuration in application.yml for all channels

**Files Created:**
```
backend/notification-service/src/main/java/in/winvestco/notification_service/
├── model/
│   ├── DeliveryChannel.java
│   ├── NotificationChannel.java
│   └── NotificationPriority.java
├── config/
│   ├── NotificationChannelConfig.java
│   └── AsyncConfig.java
├── service/
│   ├── NotificationDeliveryStrategy.java
│   ├── NotificationChannelService.java
│   └── channel/
│       ├── ChannelDeliveryService.java
│       ├── PushNotificationService.java
│       ├── EmailNotificationService.java
│       └── SmsNotificationService.java
├── controller/
│   └── NotificationChannelController.java
├── repository/
│   └── NotificationChannelRepository.java
└── dto/
    ├── NotificationChannelDTO.java
    └── UpdateChannelPreferencesRequest.java

resources/db/migration/
└── V2__multi_channel_notification_support.sql
```

**API Endpoints Added:**
- `GET /api/v1/notifications/channels` - Get all channel preferences
- `GET /api/v1/notifications/channels/{type}` - Get preference for type
- `PUT /api/v1/notifications/channels/{type}` - Update preference
- `PATCH /api/v1/notifications/channels/fcm-token` - Register FCM token
- `PATCH /api/v1/notifications/channels/email` - Update email
- `PATCH /api/v1/notifications/channels/phone` - Update phone
- `POST /api/v1/notifications/channels/enable/{channel}` - Enable channel
- `POST /api/v1/notifications/channels/disable/{channel}` - Disable channel
- `GET /api/v1/notifications/channels/status` - Get channel availability

**To Enable Channels in Production:**
Set these environment variables:
```bash
# Push Notifications (Firebase)
PUSH_ENABLED=true
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CREDENTIALS_PATH=/path/to/credentials.json

# Email (SendGrid)
EMAIL_ENABLED=true
EMAIL_PROVIDER=sendgrid
SENDGRID_API_KEY=your-api-key

# SMS (Twilio)
SMS_ENABLED=true
SMS_PROVIDER=twilio
TWILIO_ACCOUNT_SID=your-sid
TWILIO_AUTH_TOKEN=your-token
TWILIO_FROM_NUMBER=+1234567890
```

---


#### 1.2 WebSocket Authentication & Security

**Problem:** User ID is passed via query parameter (`?userId=123`), which is vulnerable to spoofing.

**Required Changes:**
- [ ] Modify `WebSocketConfig.UserIdHandshakeInterceptor` to validate JWT tokens
- [ ] Extract user ID from validated JWT claims
- [ ] Add rate limiting for WebSocket connections (max 5 per user)
- [ ] Replace `setAllowedOrigins("*")` with specific allowed origins
- [ ] Implement connection heartbeat with stale session cleanup

**File to Modify:** [WebSocketConfig.java](file:///e:/winvestco-trading-platform/backend/notification-service/src/main/java/in/winvestco/notification_service/config/WebSocketConfig.java)

**Implementation:**
```java
@Override
public boolean beforeHandshake(...) {
    String authHeader = ((ServletServerHttpRequest) request)
        .getServletRequest().getHeader("Authorization");
    
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.validateToken(token);
            attributes.put("userId", claims.get("sub", Long.class));
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT for WebSocket: {}", e.getMessage());
            return false;
        }
    }
    return false;
}
```

---

#### 1.3 Notification Delivery Guarantees ✅ COMPLETED

**Problem:** Best-effort delivery with no retry on WebSocket failure.

**Status:** ✅ Implemented on 2026-01-28

**Implemented Components:**
- [x] Create `DeliveryStatus` enum: PENDING, IN_PROGRESS, DELIVERED, RETRYING, FAILED, SKIPPED, DEAD_LETTER
- [x] Create `NotificationDelivery` entity to track delivery status per notification-channel
- [x] Implement retry mechanism with exponential backoff (scheduled every 30 seconds)
- [x] Queue notifications for offline users, deliver on WebSocket reconnection
- [x] Add dead letter queue for permanently failed notifications (archived after 7 days)
- [x] Expose delivery status via API with health monitoring
- [x] Create `NotificationDeliveryTracker` service for delivery lifecycle management
- [x] Create `NotificationRetryService` with scheduled jobs
- [x] Update `NotificationDeliveryStrategy` to track delivery results
- [x] Update `NotificationWebSocketHandler` to deliver pending on reconnection

**Files Created:**
```
backend/notification-service/src/main/java/in/winvestco/notification_service/
├── model/
│   ├── DeliveryStatus.java
│   └── NotificationDelivery.java
├── dto/
│   └── NotificationDeliveryDTO.java
├── service/
│   ├── NotificationDeliveryTracker.java
│   └── NotificationRetryService.java
├── controller/
│   └── NotificationDeliveryController.java
└── repository/
    └── NotificationDeliveryRepository.java

resources/db/migration/
└── V3__notification_delivery_tracking.sql
```

**API Endpoints Added:**
- `GET /api/v1/notifications/delivery/{notificationId}` - Get delivery status
- `GET /api/v1/notifications/delivery/{notificationId}/{channel}` - Get channel delivery status
- `GET /api/v1/notifications/delivery/stats` - Get delivery statistics
- `GET /api/v1/notifications/delivery/failed` - Get failed deliveries
- `GET /api/v1/notifications/delivery/dead-letter` - Get dead letter queue
- `POST /api/v1/notifications/delivery/{deliveryId}/retry` - Manual retry
- `POST /api/v1/notifications/delivery/user/{userId}/deliver-pending` - Deliver pending to user
- `GET /api/v1/notifications/delivery/pending/count` - Get pending retry count
- `GET /api/v1/notifications/delivery/health` - Get delivery system health

**Scheduled Jobs:**
- Retry pending deliveries: every 30 seconds
- Reset stale in-progress: every 5 minutes
- Archive failed to dead letter: daily at 2 AM (after 7 days)
- Cleanup old dead letters: daily at 3 AM (after 30 days)

**Features:**
- Exponential backoff for retries (configurable base delay and multiplier)
- Max retry attempts configurable (default: 3)
- Privacy-aware destination masking in DTOs
- Health endpoint with success rate calculation
- Automatic pending notification delivery on user reconnection

---


### 🟠 Priority 2: Important Enhancements

#### 2.1 Notification Batching & Rate Limiting

**Problem:** High-frequency trading can generate hundreds of notifications per minute.

**Required Changes:**
- [ ] Create `NotificationBatcher` service with configurable window (5 seconds)
- [ ] Implement rate limiting (max 10 notifications/minute per user)
- [ ] Add digest mode option (hourly/daily email summaries)
- [ ] Batch similar notifications ("5 orders filled" instead of 5 separate)

**Configuration:**
```yaml
notification:
  batching:
    enabled: true
    window-seconds: 5
    max-batch-size: 10
  rate-limit:
    max-per-minute: 10
    burst-size: 5
```

---

#### 2.2 Notification Priority System

**Problem:** All notifications treated equally regardless of urgency.

**Required Changes:**
- [ ] Add `priority` field to `Notification` entity
- [ ] Create `NotificationPriority` enum: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`
- [ ] Priority affects delivery channels and batching eligibility
- [ ] Critical notifications bypass rate limits

**Priority Matrix:**

| Priority | Examples | Channels | Batchable |
|----------|----------|----------|-----------|
| CRITICAL | Margin call, security breach | All | No |
| HIGH | Order filled, large withdrawal | WebSocket + Push | No |
| MEDIUM | Order created, deposit | WebSocket | Yes |
| LOW | Price alerts, news | Digest | Yes |

---

#### 2.3 Notification Templates with i18n

**Problem:** Messages hardcoded in event listeners, no localization support.

**Required Changes:**
- [ ] Create `notification-templates.yml` configuration file
- [ ] Implement `NotificationTemplateService` for message rendering
- [ ] Support Hindi (`hi`) and English (`en`) initially
- [ ] Use ICU MessageFormat for number/currency formatting

**Template Format:**
```yaml
ORDER_FILLED:
  priority: HIGH
  title:
    en: "Order Executed"
    hi: "ऑर्डर निष्पादित"
  message:
    en: "Your order for {quantity} shares of {symbol} executed at ₹{price, number}"
    hi: "{symbol} के {quantity} शेयर ₹{price, number} पर निष्पादित"
```

---

#### 2.4 Cross-Device Notification Sync

**Problem:** Read/unread status not synced across user's devices.

**Required Changes:**
- [ ] Broadcast `NotificationSyncEvent` on status changes
- [ ] Frontend should listen for sync events and update local state
- [ ] Sync types: `MARK_READ`, `MARK_ALL_READ`, `DELETE`, `ARCHIVE`

---

### 🟢 Priority 3: Nice-to-Have Features

#### 3.1 Rich Notification Content

- [ ] Add action buttons (View Order, Acknowledge, Undo)
- [ ] Implement deep links to specific pages
- [ ] Optional thumbnail/chart for trade notifications
- [ ] Expandable notification details

#### 3.2 Notification Search & Filtering

- [ ] Filter by notification type
- [ ] Filter by date range
- [ ] Full-text search on title/message
- [ ] Filter by symbol for trading notifications
- [ ] Saved filters / notification views

#### 3.3 Notification Analytics

- [ ] Track delivery, view, and click rates
- [ ] Create `NotificationMetrics` entity
- [ ] Dashboard for ops team
- [ ] A/B testing support for notification content

#### 3.4 Custom User Alerts

- [ ] Allow users to create price alerts
- [ ] Volume spike alerts
- [ ] News mention alerts for watched stocks
- [ ] Alert management UI

#### 3.5 Scheduled Notifications

- [ ] Market open/close reminders
- [ ] End-of-day portfolio summary
- [ ] GTT order expiry reminders
- [ ] Custom scheduled messages

---

### 🔧 Technical Improvements

#### T1. Redis-Based WebSocket Session Management ✅ COMPLETED

**Problem:** In-memory `WebSocketSessionManager` doesn't scale horizontally.

**Status:** ✅ Implemented on 2026-01-28

**Implemented Components:**
- [x] Integrate `spring-boot-starter-data-redis`
- [x] Create `RedisConfig` for Pub/Sub topic `notification.topic`
- [x] Create `RedisNotificationPublisher` to broadcast messages across cluster
- [x] Create `RedisNotificationSubscriber` to receive and dispatch to local sessions
- [x] Update `WebSocketSessionManager` to persist session metadata in Redis (`ws:user:*`, `ws:sessions:*`)
- [x] Update `WebSocketNotificationService` to use Redis Pub/Sub for delivery
- [x] Implement graceful session cleanup on shutdown

**Redis Keys Used:**
```
ws:user:{userId} -> Set of {sessionId}:{instanceId}
ws:sessions:{sessionId} -> JSON Metadata {userId, instanceId, connectedAt}
```

---


#### T2. Event Sourcing for Notifications

- [ ] Create `NotificationEvent` entity for audit trail
- [ ] Events: CREATED, DELIVERED, READ, ARCHIVED, DELETED
- [ ] Enable replay for debugging

---

#### T3. Frontend Offline Support

- [ ] Service Worker for push notifications when browser closed
- [ ] IndexedDB for offline notification storage
- [ ] Sync unread count on reconnection
- [ ] PWA manifest configuration

---

## Implementation Roadmap

| Phase | Duration | Scope |
|-------|----------|-------|
| Phase 1 | 2 weeks | WebSocket security (#1.2), Delivery guarantees (#1.3) |
| Phase 2 | 3 weeks | Multi-channel (#1.1), Templates (#2.3) |
| Phase 3 | 2 weeks | Priority system (#2.2), Batching (#2.1), Sync (#2.4) |
| Phase 4 | 3 weeks | Alerts (#3.4), Scheduled (#3.5), Analytics (#3.3) |
| Phase 5 | 2 weeks | Redis scaling (#T1), Offline support (#T3) |

---

## Dependencies

| Improvement | External Dependencies |
|-------------|----------------------|
| Push Notifications | Firebase Cloud Messaging SDK |
| Email | SendGrid or AWS SES SDK |
| SMS | Twilio SDK |
| Redis Sessions | Spring Data Redis |
| Offline Support | Workbox (Service Worker library) |

---

## Testing Requirements

- [ ] Unit tests for all new services
- [ ] Integration tests for multi-channel delivery
- [ ] WebSocket security penetration testing
- [ ] Load testing for rate limiting
- [ ] E2E tests for frontend notification flow

---

## Related Documents

- [README.md](file:///e:/winvestco-trading-platform/README.md) - Project overview
- [API Gateway](file:///e:/winvestco-trading-platform/backend/api-gateway/) - Rate limiting config
- [Common Events](file:///e:/winvestco-trading-platform/backend/common/src/main/java/in/winvestco/common/event/) - Event definitions
