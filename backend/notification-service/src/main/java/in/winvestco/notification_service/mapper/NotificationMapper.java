package in.winvestco.notification_service.mapper;

import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.Notification;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for Notification entity.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDTO toDTO(Notification notification);
    
    List<NotificationDTO> toDTOList(List<Notification> notifications);
}
