package in.winvestco.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;

import in.winvestco.common.enums.AccountStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusChangedEvent implements Serializable {

    private Long userId;
    private AccountStatus oldStatus;
    private AccountStatus newStatus;
    private String changedBy;
    private String reason;
    private LocalDateTime changedAt;

}
