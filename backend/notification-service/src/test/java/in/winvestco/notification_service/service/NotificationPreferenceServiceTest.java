package in.winvestco.notification_service.service;

import in.winvestco.notification_service.dto.MuteSettingsDTO;
import in.winvestco.notification_service.model.NotificationPreference;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    @Test
    void isNotificationMuted_whenMuted_shouldReturnTrue() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[] { "ORDER_CREATED" })
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        assertTrue(preferenceService.isNotificationMuted(1L, NotificationType.ORDER_CREATED));
    }

    @Test
    void isNotificationMuted_whenNotMuted_shouldReturnFalse() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[0])
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        assertFalse(preferenceService.isNotificationMuted(1L, NotificationType.ORDER_CREATED));
    }

    @Test
    void isNotificationMuted_securityType_shouldAlwaysReturnFalse() {
        // Security types cannot be muted even if preferences say so
        assertFalse(preferenceService.isNotificationMuted(1L, NotificationType.USER_LOGIN));
        assertFalse(preferenceService.isNotificationMuted(1L, NotificationType.USER_PASSWORD_CHANGED));
        assertFalse(preferenceService.isNotificationMuted(1L, NotificationType.USER_STATUS_CHANGED));
    }

    @Test
    void isNotificationMuted_noPreference_shouldReturnFalse() {
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertFalse(preferenceService.isNotificationMuted(1L, NotificationType.ORDER_CREATED));
    }

    @Test
    void getMuteSettings_shouldReturnCorrectDTO() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[] { "ORDER_CREATED", "TRADE_EXECUTED" })
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        MuteSettingsDTO settings = preferenceService.getMuteSettings(1L);

        assertEquals(1L, settings.getUserId());
        assertFalse(settings.getMuteAll());
        assertEquals(2, settings.getMutedTypes().size());
        assertTrue(settings.getMutedTypes().contains(NotificationType.ORDER_CREATED));
        assertFalse(settings.getUnmutableTypes().isEmpty());
    }

    @Test
    void muteType_shouldSavePreference() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[0])
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));
        when(preferenceRepository.save(any())).thenReturn(pref);

        MuteSettingsDTO result = preferenceService.muteType(1L, NotificationType.ORDER_CREATED);

        assertNotNull(result);
        verify(preferenceRepository).save(any());
    }

    @Test
    void muteType_securityType_shouldNotMute() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[0])
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        MuteSettingsDTO result = preferenceService.muteType(1L, NotificationType.USER_LOGIN);

        assertNotNull(result);
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void unmuteType_shouldRemoveFromList() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[] { "ORDER_CREATED" })
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));
        when(preferenceRepository.save(any())).thenReturn(pref);

        MuteSettingsDTO result = preferenceService.unmuteType(1L, NotificationType.ORDER_CREATED);

        assertNotNull(result);
        verify(preferenceRepository).save(any());
    }

    @Test
    void muteAll_shouldSetMuteAllFlag() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(false)
                .mutedTypes(new String[0])
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));
        when(preferenceRepository.save(any())).thenReturn(pref);

        MuteSettingsDTO result = preferenceService.muteAll(1L);

        assertNotNull(result);
        verify(preferenceRepository).save(any());
    }

    @Test
    void unmuteAll_shouldClearMuteSettings() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(1L)
                .muteAll(true)
                .mutedTypes(new String[] { "ORDER_CREATED" })
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));
        when(preferenceRepository.save(any())).thenReturn(pref);

        MuteSettingsDTO result = preferenceService.unmuteAll(1L);

        assertNotNull(result);
        verify(preferenceRepository).save(any());
    }
}
