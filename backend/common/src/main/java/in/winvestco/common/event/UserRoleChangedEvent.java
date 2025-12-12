package in.winvestco.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import in.winvestco.common.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleChangedEvent implements Serializable {

    private Long userId;
    private Set<Role> oldRoles;
    private Set<Role> newRoles;
    private String changedBy;
    private String reason;
    private LocalDateTime changedAt;

}
