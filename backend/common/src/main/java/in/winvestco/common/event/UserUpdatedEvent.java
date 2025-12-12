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
public class UserUpdatedEvent implements Serializable {

    private Long userId;
    private String email;
    private String oldEmail;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDateTime updatedAt;
    private String updatedBy;

}
