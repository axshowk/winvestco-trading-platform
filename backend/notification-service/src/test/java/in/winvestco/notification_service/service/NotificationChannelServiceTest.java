package in.winvestco.notification_service.service;

import in.winvestco.notification_service.dto.NotificationChannelDTO;
import in.winvestco.notification_service.dto.UpdateChannelPreferencesRequest;
import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationChannel;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.repository.NotificationChannelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationChannelServiceTest {

    @Mock
    private NotificationChannelRepository channelRepository;

    @InjectMocks
    private NotificationChannelService channelService;

    @Test
    void getUserChannelPreferences_shouldReturnAllPreferences() {
        NotificationChannel channel = NotificationChannel.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.ORDER_CREATED)
                .websocketEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .pushEnabled(false)
                .build();
        when(channelRepository.findByUserId(1L)).thenReturn(List.of(channel));

        List<NotificationChannelDTO> result = channelService.getUserChannelPreferences(1L);

        assertEquals(1, result.size());
        assertEquals(NotificationType.ORDER_CREATED, result.get(0).getNotificationType());
    }

    @Test
    void getChannelPreference_found_shouldReturnDTO() {
        NotificationChannel channel = NotificationChannel.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.ORDER_CREATED)
                .websocketEnabled(true)
                .emailEnabled(true)
                .smsEnabled(false)
                .pushEnabled(false)
                .emailAddress("user@example.com")
                .build();
        when(channelRepository.findByUserIdAndNotificationType(1L, NotificationType.ORDER_CREATED))
                .thenReturn(Optional.of(channel));

        Optional<NotificationChannelDTO> result = channelService.getChannelPreference(1L,
                NotificationType.ORDER_CREATED);

        assertTrue(result.isPresent());
        assertTrue(result.get().getWebsocketEnabled());
        assertTrue(result.get().getEmailEnabled());
    }

    @Test
    void getChannelPreference_notFound_shouldReturnEmpty() {
        when(channelRepository.findByUserIdAndNotificationType(1L, NotificationType.ORDER_CREATED))
                .thenReturn(Optional.empty());

        Optional<NotificationChannelDTO> result = channelService.getChannelPreference(1L,
                NotificationType.ORDER_CREATED);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateChannelPreference_existingRecord_shouldUpdate() {
        NotificationChannel existing = NotificationChannel.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.ORDER_CREATED)
                .websocketEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .pushEnabled(false)
                .build();
        when(channelRepository.findByUserIdAndNotificationType(1L, NotificationType.ORDER_CREATED))
                .thenReturn(Optional.of(existing));
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateChannelPreferencesRequest request = new UpdateChannelPreferencesRequest();
        request.setNotificationType(NotificationType.ORDER_CREATED);
        request.setEnabledChannels(Set.of(DeliveryChannel.WEBSOCKET, DeliveryChannel.EMAIL));
        request.setEmailAddress("user@example.com");

        NotificationChannelDTO result = channelService.updateChannelPreference(1L, request);

        assertNotNull(result);
        verify(channelRepository).save(any());
    }

    @Test
    void updateChannelPreference_newRecord_shouldCreate() {
        when(channelRepository.findByUserIdAndNotificationType(1L, NotificationType.TRADE_EXECUTED))
                .thenReturn(Optional.empty());
        when(channelRepository.save(any())).thenAnswer(inv -> {
            NotificationChannel saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UpdateChannelPreferencesRequest request = new UpdateChannelPreferencesRequest();
        request.setNotificationType(NotificationType.TRADE_EXECUTED);
        request.setEnabledChannels(Set.of(DeliveryChannel.WEBSOCKET));

        NotificationChannelDTO result = channelService.updateChannelPreference(1L, request);

        assertNotNull(result);
        verify(channelRepository).save(any());
    }

    @Test
    void updateFcmToken_shouldUpdateAllRecords() {
        NotificationChannel channel = NotificationChannel.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.ORDER_CREATED)
                .websocketEnabled(true)
                .pushEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .build();
        when(channelRepository.findByUserId(1L)).thenReturn(List.of(channel));
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        channelService.updateFcmToken(1L, "new-fcm-token-123");

        verify(channelRepository).save(any());
    }

    @Test
    void updateEmailAddress_shouldUpdateAllRecords() {
        NotificationChannel channel = NotificationChannel.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.ORDER_CREATED)
                .websocketEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .smsEnabled(false)
                .build();
        when(channelRepository.findByUserId(1L)).thenReturn(List.of(channel));
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        channelService.updateEmailAddress(1L, "new@example.com");

        verify(channelRepository).save(any());
    }

    @Test
    void updatePhoneNumber_shouldUpdateAllRecords() {
        NotificationChannel channel = NotificationChannel.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.ORDER_CREATED)
                .websocketEnabled(true)
                .smsEnabled(true)
                .emailEnabled(false)
                .pushEnabled(false)
                .build();
        when(channelRepository.findByUserId(1L)).thenReturn(List.of(channel));
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        channelService.updatePhoneNumber(1L, "+919876543210");

        verify(channelRepository).save(any());
    }

    @Test
    void enableChannelForAllTypes_shouldEnableChannel() {
        NotificationChannel channel = NotificationChannel.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.ORDER_CREATED)
                .websocketEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .pushEnabled(false)
                .build();
        when(channelRepository.findByUserId(1L)).thenReturn(List.of(channel));
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        channelService.enableChannelForAllTypes(1L, DeliveryChannel.EMAIL);

        verify(channelRepository).save(argThat(ch -> ((NotificationChannel) ch).getEmailEnabled()));
    }

    @Test
    void disableChannelForAllTypes_shouldDisableChannel() {
        NotificationChannel channel = NotificationChannel.builder()
                .id(1L)
                .userId(1L)
                .notificationType(NotificationType.ORDER_CREATED)
                .websocketEnabled(true)
                .emailEnabled(true)
                .smsEnabled(false)
                .pushEnabled(false)
                .build();
        when(channelRepository.findByUserId(1L)).thenReturn(List.of(channel));
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        channelService.disableChannelForAllTypes(1L, DeliveryChannel.EMAIL);

        verify(channelRepository).save(argThat(ch -> !((NotificationChannel) ch).getEmailEnabled()));
    }

    @Test
    void deleteUserPreferences_shouldDeleteAll() {
        channelService.deleteUserPreferences(1L);

        verify(channelRepository).deleteByUserId(1L);
    }
}
