package in.winvestco.user_service.mapper;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.user_service.dto.RegisterRequest;
import in.winvestco.user_service.dto.UserResponse;
import in.winvestco.user_service.model.User;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", expression = "java(in.winvestco.common.enums.AccountStatus.ACTIVE)")
    @Mapping(target = "roles", expression = "java(java.util.Collections.singleton(in.winvestco.common.enums.Role.USER))")
    User toEntity(RegisterRequest registerRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateFromDto(RegisterRequest registerRequest, @MappingTarget User user);

    UserResponse toDto(User user);

    List<UserResponse> toDtoList(List<User> users);

    @Named("mapStatus")
    default String mapStatus(AccountStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("toAccountStatus")
    default AccountStatus toAccountStatus(String status) {
        return status != null ? AccountStatus.valueOf(status) : null;
    }
}
