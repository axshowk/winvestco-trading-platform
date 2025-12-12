package in.winvestco.user_service.dto;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.user_service.model.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String clientId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Instant createdAt;
    private Set<Role> roles;
    private AccountStatus status;
    private Instant lastLoginAt;

    public static UserResponse fromUser(User user) {
        return new UserResponse(
            user.getId(),
            user.getClientId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhoneNumber(),
            user.getCreatedAt(),
            user.getRoles(),
            user.getStatus(),
            user.getLastLoginAt()
        );
    }
}
