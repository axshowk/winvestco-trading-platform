package in.winvestco.notification_service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferenceTest {

    @Test
    void isTypeMuted_whenMuteAll_shouldReturnTrueForMutableTypes() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(true)
                .mutedTypes(new String[0])
                .build();

        assertTrue(pref.isTypeMuted(NotificationType.ORDER_CREATED));
        assertTrue(pref.isTypeMuted(NotificationType.TRADE_EXECUTED));
    }

    @Test
    void isTypeMuted_securityType_shouldAlwaysReturnFalse() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(true)
                .mutedTypes(new String[] { "USER_LOGIN", "USER_PASSWORD_CHANGED" })
                .build();

        // Security types cannot be muted even with muteAll=true
        assertFalse(pref.isTypeMuted(NotificationType.USER_LOGIN));
        assertFalse(pref.isTypeMuted(NotificationType.USER_PASSWORD_CHANGED));
        assertFalse(pref.isTypeMuted(NotificationType.USER_STATUS_CHANGED));
    }

    @Test
    void isTypeMuted_whenInMutedList_shouldReturnTrue() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[] { "ORDER_CREATED", "TRADE_EXECUTED" })
                .build();

        assertTrue(pref.isTypeMuted(NotificationType.ORDER_CREATED));
        assertTrue(pref.isTypeMuted(NotificationType.TRADE_EXECUTED));
        assertFalse(pref.isTypeMuted(NotificationType.FUNDS_DEPOSITED));
    }

    @Test
    void muteType_shouldAddToMutedList() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[0])
                .build();

        pref.muteType(NotificationType.ORDER_CREATED);

        assertTrue(pref.isTypeMuted(NotificationType.ORDER_CREATED));
        assertEquals(1, pref.getMutedTypes().length);
    }

    @Test
    void muteType_securityType_shouldBeIgnored() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[0])
                .build();

        pref.muteType(NotificationType.USER_LOGIN);

        assertEquals(0, pref.getMutedTypes().length);
        assertFalse(pref.isTypeMuted(NotificationType.USER_LOGIN));
    }

    @Test
    void muteType_alreadyMuted_shouldBeIdempotent() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[] { "ORDER_CREATED" })
                .build();

        pref.muteType(NotificationType.ORDER_CREATED);

        assertEquals(1, pref.getMutedTypes().length);
    }

    @Test
    void unmuteType_shouldRemoveFromList() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[] { "ORDER_CREATED", "TRADE_EXECUTED" })
                .build();

        pref.unmuteType(NotificationType.ORDER_CREATED);

        assertFalse(pref.isTypeMuted(NotificationType.ORDER_CREATED));
        assertTrue(pref.isTypeMuted(NotificationType.TRADE_EXECUTED));
        assertEquals(1, pref.getMutedTypes().length);
    }

    @Test
    void muteAllNotifications_shouldSetMuteAllTrue() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .build();

        pref.muteAllNotifications();

        assertTrue(pref.getMuteAll());
    }

    @Test
    void unmuteAllNotifications_shouldClearEverything() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(true)
                .mutedTypes(new String[] { "ORDER_CREATED", "TRADE_EXECUTED" })
                .build();

        pref.unmuteAllNotifications();

        assertFalse(pref.getMuteAll());
        assertEquals(0, pref.getMutedTypes().length);
    }
}
