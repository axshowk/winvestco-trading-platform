package in.winvestco.user_service.controller;

import in.winvestco.common.annotation.Auditable;
import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.dto.RegisterRequest;
import in.winvestco.user_service.dto.UserResponse;
import in.winvestco.user_service.model.User;
import in.winvestco.user_service.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.Collections;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Register, view, and manage users")
public class UserController {

    private final UserService userService;
    private final LoggingUtils loggingUtils;

    // ===== Read endpoints =====   

    @GetMapping
    @Operation(summary = "List users (ADMIN)", 
              description = "View users with optional filters (status, role, name, email). " +
                         "If multiple filters are provided, they will be combined with AND logic.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> findAll(
            @Parameter(description = "Filter by account status") 
            @RequestParam(name = "status", required = false) AccountStatus status,
            
            @Parameter(description = "Filter by user role") 
            @RequestParam(name = "role", required = false) Role role,
            
            @Parameter(description = "Search by firstname (case-insensitive)") 
            @RequestParam(name = "firstName", required = false) String firstName,

            @Parameter(description = "Search by lastname (case-insensitive)") 
            @RequestParam(name = "lastName", required = false) String lastName,
            
            @Parameter(description = "Search by exact email") 
            @RequestParam(name = "email", required = false) String email,
            
            @Parameter(description = "Search by phonenumber") 
            @RequestParam(name = "phoneNumber", required = false) String phoneNumber) {
                
        // Create a mutable map to handle null values
        Map<String, Object> logParams = new HashMap<>();
        if (status != null) logParams.put("status", status);
        if (role != null) logParams.put("role", role);
        if (firstName != null) logParams.put("firstName", firstName);
        if (lastName != null) logParams.put("lastName", lastName);
        if (email != null) logParams.put("email", email);
        if (phoneNumber != null) logParams.put("phoneNumber", phoneNumber);
        
        loggingUtils.logDebug("UserController.findAll called with filters", logParams);
        
        if (email != null && !email.isBlank()) {
            UserResponse user = userService.findByEmail(email);
            return user != null ? List.of(user) : Collections.emptyList();
        }
        
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            List<UserResponse> users = userService.findByPhoneNumber(phoneNumber);
            return users != null ? users : Collections.emptyList();
        }
                
        if (firstName != null && !firstName.isBlank()) {
            return userService.findByFirstName(firstName);
        }
        
        if (lastName != null && !lastName.isBlank()) {
            return userService.findByLastName(lastName);
        }
        
        if (status != null) {
            return userService.findAllByStatus(status);
        }
        
        if (role != null) {
            return userService.findAllByRole(role);
        }
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (ADMIN)", description = "View a single user's details by ID")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        loggingUtils.logDebug("UserController.findById called", java.util.Map.of("userId", id));
        UserResponse user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account")
    @Auditable(action = "USER_REGISTRATION", context = "User registration via API")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        loggingUtils.logServiceStart("UserController", "register", request.getEmail());
        
        User newUser = userService.register(
            request.getEmail(),
            request.getFirstName(),
            request.getLastName(),
            request.getPassword(),
            request.getPhoneNumber()
        );
        
        loggingUtils.logServiceEnd("UserController", "register", newUser.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(newUser));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user (ADMIN)", description = "Admin-only: replace user details")
    @SecurityRequirement(name = "bearerAuth")
    @Auditable(action = "USER_UPDATE", context = "User profile update via API")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody User user) {
        loggingUtils.logServiceStart("UserController", "update", id);
        
        user.setId(id);
        User updatedUser = userService.update(user);
        
        loggingUtils.logServiceEnd("UserController", "update", updatedUser.getId());
        
        return UserResponse.fromUser(updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user (ADMIN)", description = "Admin-only: delete user by ID")
    @SecurityRequirement(name = "bearerAuth")
    @Auditable(action = "USER_DELETE", context = "User deletion via API")
    public void delete(@PathVariable Long id) {
        loggingUtils.logServiceStart("UserController", "delete", id);
        
        userService.deleteById(id);
        
        loggingUtils.logServiceEnd("UserController", "delete", id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update status (ADMIN)", description = "Admin-only: set user's account status")
    @SecurityRequirement(name = "bearerAuth")
    @Auditable(action = "USER_STATUS_UPDATE", context = "Status change via API")
    public UserResponse updateStatus(
            @PathVariable("id") Long id,
            @Parameter(description = "New account status", required = true, example = "ACTIVE")
            @RequestParam(value = "status", required = true) AccountStatus status) {
        loggingUtils.logServiceStart("UserController", "updateStatus", 
            java.util.Map.of("userId", id, "status", status));
        
        UserResponse result = userService.updateStatus(id, status);
        
        loggingUtils.logServiceEnd("UserController", "updateStatus", id);
        
        return result;
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update roles (ADMIN)", description = "Admin-only: replace user's roles set")
    @SecurityRequirement(name = "bearerAuth")
    @Auditable(action = "USER_ROLES_UPDATE", context = "Roles modification via API")
    public UserResponse updateRoles(@PathVariable Long id, @RequestBody Set<Role> roles) {
        loggingUtils.logServiceStart("UserController", "updateRoles", 
            java.util.Map.of("userId", id, "roles", roles));
        
        UserResponse updatedUser = userService.updateRoles(id, roles);
        
        loggingUtils.logServiceEnd("UserController", "updateRoles", id);
        
        return updatedUser;
    }
}
