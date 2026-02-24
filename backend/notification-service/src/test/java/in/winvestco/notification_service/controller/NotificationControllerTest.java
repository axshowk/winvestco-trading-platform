package in.winvestco.notification_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.NotificationStatus;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import in.winvestco.common.util.LoggingUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = NotificationController.class, excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        SpringDataWebAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private LoggingUtils loggingUtils;

    private NotificationDTO testDTO;

    @BeforeEach
    void setUp() {
        testDTO = NotificationDTO.builder()
                .id(1L)
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("Order Placed")
                .message("Your order has been placed")
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getNotifications_shouldReturnPagedResults() throws Exception {
        Page<NotificationDTO> page = new PageImpl<>(List.of(testDTO));
        when(notificationService.getNotificationsForUser(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Order Placed"));
    }

    @Test
    void getUnreadNotifications_shouldReturnUnreadOnly() throws Exception {
        Page<NotificationDTO> page = new PageImpl<>(List.of(testDTO));
        when(notificationService.getNotificationsByStatus(eq(1L), eq(NotificationStatus.UNREAD), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/unread")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("UNREAD"));
    }

    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/count")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void markAsRead_whenFound_shouldReturnOk() throws Exception {
        testDTO.setStatus(NotificationStatus.READ);
        when(notificationService.markAsRead(1L, 1L)).thenReturn(Optional.of(testDTO));

        mockMvc.perform(patch("/api/v1/notifications/1/read")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    void markAsRead_whenNotFound_shouldReturn404() throws Exception {
        when(notificationService.markAsRead(999L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/notifications/999/read")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllAsRead_shouldReturnCount() throws Exception {
        when(notificationService.markAllAsRead(1L)).thenReturn(3);

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedAsRead").value(3));
    }

    @Test
    void deleteNotification_whenFound_shouldReturn204() throws Exception {
        when(notificationService.deleteNotification(1L, 1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/notifications/1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNotification_whenNotFound_shouldReturn404() throws Exception {
        when(notificationService.deleteNotification(999L, 1L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/notifications/999")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveNotification_whenFound_shouldReturnOk() throws Exception {
        testDTO.setStatus(NotificationStatus.ARCHIVED);
        when(notificationService.archiveNotification(1L, 1L)).thenReturn(Optional.of(testDTO));

        mockMvc.perform(patch("/api/v1/notifications/1/archive")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void archiveNotification_whenNotFound_shouldReturn404() throws Exception {
        when(notificationService.archiveNotification(999L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/notifications/999/archive")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound());
    }
}
