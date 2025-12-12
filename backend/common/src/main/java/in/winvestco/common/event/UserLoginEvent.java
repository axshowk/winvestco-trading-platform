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
public class UserLoginEvent implements Serializable {

    private Long userId;
    private String email;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String userAgent;
    private String loginMethod; // e.g., "WEB", "MOBILE", "API"

}
