package in.winvestco.order_service.mapper;

import in.winvestco.order_service.dto.OrderDTO;
import in.winvestco.order_service.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "remainingQuantity", expression = "java(order.getRemainingQuantity())")
    OrderDTO toDTO(Order order);

    List<OrderDTO> toDTOList(List<Order> orders);
}
