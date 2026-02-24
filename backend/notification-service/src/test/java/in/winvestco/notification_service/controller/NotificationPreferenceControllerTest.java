package in.winvestco.notification_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.notification_service.dto.MuteSettingsDTO;
import in.winvestco.notification_service.dto.MuteTypeRequest;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationPreferenceService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = NotificationPreferenceController.class, excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class NotificationPreferenceControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private NotificationPreferenceService preferenceService;

        @MockBean
        private LoggingUtils loggingUtils;

        @Test
        void getMuteSettings_shouldReturnSettings() throws Exception {
                MuteSettingsDTO settings = MuteSettingsDTO.builder()
                                .userId(1L)
                                .muteAll(false)
                                .mutedTypes(List.of(NotificationType.ORDER_CREATED))
                                .unmutableTypes(List.of(NotificationType.USER_LOGIN))
                                .build();
                when(preferenceService.getMuteSettings(1L)).thenReturn(settings);

                mockMvc.perform(get("/api/v1/notifications/preferences")
                                .header("X-User-Id", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.muteAll").value(false))
                                .andExpect(jsonPath("$.mutedTypes[0]").value("ORDER_CREATED"));
        }

        @Test
        void updateMuteType_mute_shouldReturnUpdatedSettings() throws Exception {
                MuteSettingsDTO settings = MuteSettingsDTO.builder()
                                .userId(1L)
                                .muteAll(false)
                                .mutedTypes(List.of(NotificationType.ORDER_CREATED))
                                .unmutableTypes(List.of())
                                .build();
                when(preferenceService.muteType(1L, NotificationType.ORDER_CREATED)).thenReturn(settings);

                MuteTypeRequest request = MuteTypeRequest.builder()
                                .type(NotificationType.ORDER_CREATED)
                                .mute(true)
                                .build();

                mockMvc.perform(post("/api/v1/notifications/preferences/mute-type")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mutedTypes[0]").value("ORDER_CREATED"));
        }

        @Test
        void updateMuteType_unmute_shouldReturnUpdatedSettings() throws Exception {
                MuteSettingsDTO settings = MuteSettingsDTO.builder()
                                .userId(1L)
                                .muteAll(false)
                                .mutedTypes(List.of())
                                .unmutableTypes(List.of())
                                .build();
                when(preferenceService.unmuteType(1L, NotificationType.ORDER_CREATED)).thenReturn(settings);

                MuteTypeRequest request = MuteTypeRequest.builder()
                                .type(NotificationType.ORDER_CREATED)
                                .mute(false)
                                .build();

                mockMvc.perform(post("/api/v1/notifications/preferences/mute-type")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mutedTypes").isEmpty());
        }

        @Test
        void muteAll_shouldReturnUpdatedSettings() throws Exception {
                MuteSettingsDTO settings = MuteSettingsDTO.builder()
                                .userId(1L)
                                .muteAll(true)
                                .mutedTypes(List.of())
                                .unmutableTypes(List.of())
                                .build();
                when(preferenceService.muteAll(1L)).thenReturn(settings);

                mockMvc.perform(post("/api/v1/notifications/preferences/mute-all")
                                .header("X-User-Id", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.muteAll").value(true));
        }

        @Test
        void unmuteAll_shouldReturnUpdatedSettings() throws Exception {
                MuteSettingsDTO settings = MuteSettingsDTO.builder()
                                .userId(1L)
                                .muteAll(false)
                                .mutedTypes(List.of())
                                .unmutableTypes(List.of())
                                .build();
                when(preferenceService.unmuteAll(1L)).thenReturn(settings);

                mockMvc.perform(post("/api/v1/notifications/preferences/unmute-all")
                                .header("X-User-Id", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.muteAll").value(false));
        }
}
