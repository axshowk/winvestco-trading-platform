package in.winvestco.trade_service.mapper;

import in.winvestco.trade_service.dto.CreateTradeRequest;
import in.winvestco.trade_service.dto.TradeDTO;
import in.winvestco.trade_service.model.Trade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper for Trade entity to DTO conversions.
 */
@Mapper(componentModel = "spring")
public interface TradeMapper {

    /**
     * Convert Trade entity to DTO
     */
    @Mapping(target = "remainingQuantity", expression = "java(trade.getRemainingQuantity())")
    TradeDTO toDTO(Trade trade);

    /**
     * Convert list of Trade entities to DTOs
     */
    List<TradeDTO> toDTOList(List<Trade> trades);

    /**
     * Convert CreateTradeRequest to Trade entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tradeId", ignore = true)
    @Mapping(target = "executedQuantity", ignore = true)
    @Mapping(target = "averagePrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "validatedAt", ignore = true)
    @Mapping(target = "placedAt", ignore = true)
    @Mapping(target = "executedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    Trade toEntity(CreateTradeRequest request);

    /**
     * Update Trade entity from request
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tradeId", ignore = true)
    @Mapping(target = "executedQuantity", ignore = true)
    @Mapping(target = "averagePrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "validatedAt", ignore = true)
    @Mapping(target = "placedAt", ignore = true)
    @Mapping(target = "executedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    void updateEntity(@MappingTarget Trade trade, CreateTradeRequest request);
}
