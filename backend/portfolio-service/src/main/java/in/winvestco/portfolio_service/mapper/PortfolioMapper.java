package in.winvestco.portfolio_service.mapper;

import in.winvestco.portfolio_service.dto.HoldingDTO;
import in.winvestco.portfolio_service.dto.PortfolioDTO;
import in.winvestco.portfolio_service.model.Holding;
import in.winvestco.portfolio_service.model.Portfolio;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Portfolio and related entities
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @Builder(disableBuilder = true))
public interface PortfolioMapper {

    /**
     * Convert Portfolio entity to DTO
     */
    @Mapping(target = "profitLoss", ignore = true)
    @Mapping(target = "profitLossPercentage", ignore = true)
    PortfolioDTO toDTO(Portfolio portfolio);

    /**
     * Convert list of Portfolio entities to DTOs
     */
    List<PortfolioDTO> toDTOList(List<Portfolio> portfolios);

    /**
     * Convert Holding entity to DTO
     */
    @Mapping(target = "portfolioId", source = "portfolio.id")
    @Mapping(target = "currentPrice", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "profitLoss", ignore = true)
    @Mapping(target = "profitLossPercentage", ignore = true)
    @Mapping(target = "dayChange", ignore = true)
    @Mapping(target = "dayChangePercentage", ignore = true)
    HoldingDTO toDTO(Holding holding);

    /**
     * Convert list of Holding entities to DTOs
     */
    List<HoldingDTO> toHoldingDTOList(List<Holding> holdings);

    /**
     * Update Portfolio entity from DTO (for partial updates)
     */
    void updatePortfolioFromDTO(PortfolioDTO dto, @MappingTarget Portfolio portfolio);
}
