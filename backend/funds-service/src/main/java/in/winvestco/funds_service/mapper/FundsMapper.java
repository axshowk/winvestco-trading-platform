package in.winvestco.funds_service.mapper;

import in.winvestco.funds_service.dto.*;
import in.winvestco.funds_service.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for funds service entities and DTOs.
 * Note: LedgerEntry mappings are now in ledger-service.
 */
@Mapper(componentModel = "spring")
public interface FundsMapper {

    // Wallet mappings
    @Mapping(target = "totalBalance", expression = "java(wallet.getTotalBalance())")
    WalletDTO toWalletDTO(Wallet wallet);
    
    List<WalletDTO> toWalletDTOList(List<Wallet> wallets);

    // FundsLock mappings
    FundsLockDTO toFundsLockDTO(FundsLock fundsLock);
    
    List<FundsLockDTO> toFundsLockDTOList(List<FundsLock> fundsLocks);

    // Transaction mappings
    TransactionDTO toTransactionDTO(Transaction transaction);
    
    List<TransactionDTO> toTransactionDTOList(List<Transaction> transactions);
}
