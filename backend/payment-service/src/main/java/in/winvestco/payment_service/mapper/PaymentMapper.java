package in.winvestco.payment_service.mapper;

import in.winvestco.payment_service.dto.PaymentResponse;
import in.winvestco.payment_service.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Payment entity
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);
}
