package in.winvestco.user_service.controller;

import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.dto.LoginRequest;
import in.winvestco.user_service.dto.UserResponse;
import in.winvestco.user_service.service.JwtService;
import in.winvestco.user_service.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final LoggingUtils loggingUtils;
        private final UserService userService;
        private final JwtService jwtService;

        // ===== Request/Response DTOs =====

        @Schema(name = "UserInfoResponse", description = "User information response")
        public record UserInfoResponse(
                        @Schema(description = "User ID") Long id,
                        @Schema(description = "User email") String email,
                        @Schema(description = "User first name") String firstName,
                        @Schema(description = "User last name") String lastName,
                        @Schema(description = "User roles") List<String> roles) {
        }

        @Schema(name = "TokenVerificationResponse", description = "Token verification response")
        public record TokenVerificationResponse(
                        @Schema(description = "Verification status message") String message,
                        @Schema(description = "Whether the token is valid") boolean valid,
                        @Schema(description = "User information") UserInfoResponse user) {
        }

        @Schema(name = "LoginResponse", description = "Login response with JWT token")
        public record LoginResponse(
                        @Schema(description = "JWT access token") String accessToken,
                        @Schema(description = "Token type") String tokenType,
                        @Schema(description = "User information") UserInfoResponse user) {
        }

        // ===== API Endpoints =====

        @GetMapping("/me")
        @Operation(summary = "Get current user info", description = "Get information about the currently authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User information retrieved", content = @Content(schema = @Schema(implementation = TokenVerificationResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
        })
        public ResponseEntity<TokenVerificationResponse> getCurrentUser(
                        Authentication authentication,
                        @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Email", required = false) String headerEmail,
                        @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String headerUserId,
                        @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Roles", required = false) String headerRoles) {
                try {
                        String email = null;
                        List<String> roles = null;

                        // First, try to get user info from custom headers (set by API Gateway)
                        if (headerEmail != null && !headerEmail.isEmpty()) {
                                email = headerEmail;
                                roles = headerRoles != null && !headerRoles.isEmpty()
                                                ? List.of(headerRoles.split(","))
                                                : List.of();
                                log.debug("Using user info from API Gateway headers: email={}", email);
                        }
                        // Fallback to JWT authentication if headers not present
                        else if (authentication != null && authentication instanceof JwtAuthenticationToken) {
                                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
                                Jwt jwt = (Jwt) jwtAuth.getPrincipal();
                                email = jwt.getClaim("email");
                                roles = jwtAuth.getAuthorities().stream()
                                                .map(GrantedAuthority::getAuthority)
                                                .collect(Collectors.toList());
                                log.debug("Using user info from JWT token: email={}", email);
                        }

                        // If no authentication found
                        if (email == null) {
                                return ResponseEntity.status(401)
                                                .body(new TokenVerificationResponse("Not authenticated", false, null));
                        }

                        // Get user details from the database
                        UserResponse user = userService.findByEmail(email);

                        if (user == null) {
                                return ResponseEntity.status(401)
                                                .body(new TokenVerificationResponse("User not found", false, null));
                        }

                        UserInfoResponse userInfo = new UserInfoResponse(
                                        user.getId(),
                                        user.getEmail(),
                                        user.getFirstName(),
                                        user.getLastName(),
                                        roles != null ? roles : List.of());

                        return ResponseEntity.ok(new TokenVerificationResponse("Token is valid", true, userInfo));

                } catch (Exception e) {
                        loggingUtils.logError("AuthController", "getCurrentUser", e, "Failed to get user info");
                        return ResponseEntity.status(401)
                                        .body(new TokenVerificationResponse(
                                                        "Failed to get user info: " + e.getMessage(), false, null));
                }
        }

        @PostMapping("/login")
        @Operation(summary = "Login", description = "Authenticate with email/password and receive JWT token", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoginRequest.class), examples = {
                        @ExampleObject(name = "Sample credentials", value = "{\n  \"email\": \"admin@winvestco.in\",\n  \"password\": \"admin123\"\n}")
        })))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
        })
        public ResponseEntity<LoginResponse> login(
                        @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest request) {
                try {
                        loggingUtils.logDebug("Login attempt", Map.of(
                                        "email", request.email() != null ? request.email() : "null",
                                        "timestamp", System.currentTimeMillis()));

                        // Validate request
                        if (request.email() == null || request.password() == null) {
                                throw new BadCredentialsException("Email and password are required");
                        }

                        // Authenticate user with email as username
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.email().trim(),
                                                        request.password()));

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // Get user details
                        UserResponse user = userService.findByEmail(request.email());

                        if (user == null) {
                                throw new BadCredentialsException("User not found");
                        }

                        // Generate JWT token
                        String jwtToken = jwtService.generateToken(
                                        (UserDetails) authentication.getPrincipal(),
                                        user.getEmail(),
                                        user.getId());

                        // Extract roles
                        List<String> roles = authentication.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .collect(Collectors.toList());

                        UserInfoResponse userInfo = new UserInfoResponse(
                                        user.getId(),
                                        user.getEmail(),
                                        user.getFirstName(),
                                        user.getLastName(),
                                        roles);

                        LoginResponse response = new LoginResponse(jwtToken, "Bearer", userInfo);

                        loggingUtils.logInfo("AuthController", "login",
                                        "email=" + user.getEmail(), "userId=" + user.getId());

                        return ResponseEntity.ok(response);

                } catch (BadCredentialsException ex) {
                        loggingUtils.logError("AuthController", "login", ex,
                                        request.email() != null ? request.email() : "null");
                        throw ex;
                }
        }
}