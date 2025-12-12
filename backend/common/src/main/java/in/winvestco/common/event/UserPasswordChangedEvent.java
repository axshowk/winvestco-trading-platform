package in.winvestco.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPasswordChangedEvent implements Serializable {

    private Long userId;
    private String email;
    private String changedBy;
    private LocalDateTime changedAt;
    private String ipAddress;
    private String userAgent;

}
