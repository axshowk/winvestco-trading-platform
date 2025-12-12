package in.winvestco.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    private String email;

    @NotBlank(message = "First Name is required")
    @Size(min = 2, max = 100, message = "First Name must be between 2 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last Name is required")
    @Size(min = 2, max = 100, message = "Last Name must be between 2 and 100 characters")
    private String lastName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    private String phoneNumber;

}
