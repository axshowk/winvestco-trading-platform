package in.winvestco.ledger_service.mapper;

import in.winvestco.ledger_service.dto.LedgerEntryDTO;
import in.winvestco.ledger_service.model.LedgerEntry;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for ledger entities and DTOs
 */
@Mapper(componentModel = "spring")
public interface LedgerMapper {

    LedgerEntryDTO toDTO(LedgerEntry ledgerEntry);

    List<LedgerEntryDTO> toDTOList(List<LedgerEntry> ledgerEntries);
}
