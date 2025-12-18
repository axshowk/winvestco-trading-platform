package in.winvestco.user_service.service;

import in.winvestco.common.annotation.Auditable;
import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.common.event.UserCreatedEvent;
import in.winvestco.common.event.UserLoginEvent;
import in.winvestco.common.event.UserPasswordChangedEvent;
import in.winvestco.common.event.UserRoleChangedEvent;
import in.winvestco.common.event.UserStatusChangedEvent;
import in.winvestco.common.event.UserUpdatedEvent;
import in.winvestco.common.security.SecurityUtils;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.dto.UserResponse;
import in.winvestco.user_service.exception.UserNotFoundException;
import in.winvestco.user_service.model.User;
import in.winvestco.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher userEventPublisher;
    private final LoggingUtils loggingUtils;

    // ===== Read operations =====
    @Cacheable(value = "users", key = "'id:' + #id")
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "findById", "userId=" + id);

        try {
            UserResponse result = userRepository.findById(id).map(UserResponse::fromUser).orElse(null);

            loggingUtils.logServiceEnd("UserService", "findById", "userId=" + id);

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "findById", e, "userId=" + id);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Cacheable(value = "users", key = "'email:' + #email")
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "findByEmail", "email=" + email);

        try {
            UserResponse result = userRepository.findByEmail(email).map(UserResponse::fromUser).orElse(null);

            loggingUtils.logServiceEnd("UserService", "findByEmail", "email=" + email);

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "findByEmail", e, "email=" + email);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Cacheable(value = "users", key = "'all'")
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "findAll");

        try {
            List<UserResponse> result = userRepository.findAll().stream()
                    .map(UserResponse::fromUser)
                    .collect(Collectors.toList());

            loggingUtils.logServiceEnd("UserService", "findAll", "totalUsers=" + result.size());

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "findAll", e);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Cacheable(value = "users", key = "'status:' + #status")
    @Transactional(readOnly = true)
    public List<UserResponse> findAllByStatus(AccountStatus status) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "findAllByStatus", "status=" + status);

        try {
            List<UserResponse> result = userRepository.findAllByStatus(status).stream()
                    .map(UserResponse::fromUser)
                    .collect(Collectors.toList());

            loggingUtils.logServiceEnd("UserService", "findAllByStatus", "status=" + status,
                    "totalUsers=" + result.size());

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "findAllByStatus", e, "status=" + status);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Cacheable(value = "users", key = "'role:' + #role")
    @Transactional(readOnly = true)
    public List<UserResponse> findAllByRole(Role role) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "findAllByRole", "role=" + role);

        try {
            List<UserResponse> result = userRepository.findAllByRolesContaining(role).stream()
                    .map(UserResponse::fromUser)
                    .collect(Collectors.toList());

            loggingUtils.logServiceEnd("UserService", "findAllByRole", "role=" + role, "totalUsers=" + result.size());

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "findAllByRole", e, "role=" + role);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Cacheable(value = "users", key = "'firstName:' + #firstName")
    @Transactional(readOnly = true)
    public List<UserResponse> findByFirstName(String firstName) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "findByFirstName", "firstName=" + firstName);

        try {
            List<UserResponse> result = userRepository.findByFirstName(firstName).stream()
                    .map(UserResponse::fromUser)
                    .collect(Collectors.toList());

            loggingUtils.logServiceEnd("UserService", "findByFirstName", "firstName=" + firstName,
                    "totalUsers=" + result.size());

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "findByFirstName", e, "firstName=" + firstName);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Cacheable(value = "users", key = "'lastName:' + #lastName")
    @Transactional(readOnly = true)
    public List<UserResponse> findByLastName(String lastName) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "findByLastName", "lastName=" + lastName);

        try {
            List<UserResponse> result = userRepository.findByLastName(lastName).stream()
                    .map(UserResponse::fromUser)
                    .collect(Collectors.toList());

            loggingUtils.logServiceEnd("UserService", "findByLastName", "lastName=" + lastName,
                    "totalUsers=" + result.size());

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "findByLastName", e, "lastName=" + lastName);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Cacheable(value = "users", key = "'phoneNumber:' + #phoneNumber")
    @Transactional(readOnly = true)
    public List<UserResponse> findByPhoneNumber(String phoneNumber) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "findByPhoneNumber", "phoneNumber=" + phoneNumber);

        try {
            List<UserResponse> result = userRepository.findByPhoneNumber(phoneNumber).stream()
                    .map(UserResponse::fromUser)
                    .collect(Collectors.toList());

            loggingUtils.logServiceEnd("UserService", "findByPhoneNumber", "phoneNumber=" + phoneNumber,
                    "totalUsers=" + result.size());

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "findByPhoneNumber", e, "phoneNumber=" + phoneNumber);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Cacheable(value = "users", key = "'exists:email:' + #email")
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "existsByEmail", "email=" + email);

        try {
            boolean result = userRepository.existsByEmail(email);

            loggingUtils.logServiceEnd("UserService", "existsByEmail", "email=" + email, "exists=" + result);

            return result;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "existsByEmail", e, "email=" + email);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    // ===== Write operations =====
    @Caching(evict = {
            @CacheEvict(value = "users", key = "'all'", allEntries = true),
            @CacheEvict(value = "users", key = "'status:' + #user.status", allEntries = true),
            @CacheEvict(value = "users", key = "'firstName:' + #user.firstName", allEntries = true),
            @CacheEvict(value = "users", key = "'lastName:' + #user.lastName", allEntries = true)
    })
    @Transactional
    public User create(User user) {
        long startTime = System.currentTimeMillis();

        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "create",
                "userId=" + (user.getId() != null ? user.getId() : "new"));

        try {
            user.setId(null); // ensure create
            User saved = userRepository.save(user);

            loggingUtils.logServiceEnd("UserService", "create", "createdUserId=" + saved.getId(),
                    "email=" + saved.getEmail());

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("create", startTime, endTime);

            userEventPublisher.publishUserCreated(
                    UserCreatedEvent.builder()
                            .userId(saved.getId())
                            .email(saved.getEmail())
                            .firstName(saved.getFirstName())
                            .lastName(saved.getLastName())
                            .build());

            return saved;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "create", e,
                    "email=" + (user.getEmail() != null ? user.getEmail() : "unknown"));
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "'all'", allEntries = true),
            @CacheEvict(value = "users", key = "'email:' + #email"),
            @CacheEvict(value = "users", key = "'exists:email:' + #email"),
            @CacheEvict(value = "users", key = "'firstName:' + #firstName", allEntries = true),
            @CacheEvict(value = "users", key = "'lastName:' + #lastName", allEntries = true)
    })
    @Transactional
    @Auditable(action = "USER_REGISTER", context = "User registration process")
    public User register(String email, String firstName, String lastName, String rawPassword, String phoneNumber) {
        long startTime = System.currentTimeMillis();

        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "register", "email=" + email);

        try {
            if (existsByEmail(email)) {
                loggingUtils.logError("UserService", "register", new IllegalArgumentException("Email already exists"),
                        email);
                throw new IllegalArgumentException("Email already in use");
            }

            User toSave = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .phoneNumber(phoneNumber)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .status(AccountStatus.ACTIVE)
                    .roles(Set.of(Role.USER, Role.VIEWER))
                    .build();

            User saved = userRepository.save(toSave);
            loggingUtils.logServiceEnd("UserService", "register", "registeredUserId=" + saved.getId(),
                    "email=" + saved.getEmail());

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("register", startTime, endTime);

            userEventPublisher.publishUserCreated(
                    UserCreatedEvent.builder()
                            .userId(saved.getId())
                            .email(saved.getEmail())
                            .firstName(saved.getFirstName())
                            .lastName(saved.getLastName())
                            .build());

            return saved;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "register", e, "email=" + email);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id:' + #user.id"),
            @CacheEvict(value = "users", key = "'email:' + #user.email"),
            @CacheEvict(value = "users", key = "'exists:email:' + #user.email"),
            @CacheEvict(value = "users", key = "'all'", allEntries = true),
            @CacheEvict(value = "users", key = "'status:' + #user.status", allEntries = true),
            @CacheEvict(value = "users", key = "'firstName:' + #user.firstName", allEntries = true),
            @CacheEvict(value = "users", key = "'lastName:' + #user.lastName", allEntries = true)
    })
    @Transactional
    @Auditable(action = "USER_UPDATE", context = "User profile update")
    public User update(User user) {
        long startTime = System.currentTimeMillis();

        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "update", "userId=" + user.getId());

        try {
            if (user.getId() == null) {
                throw new IllegalArgumentException("User id is required for update");
            }

            if (!userRepository.existsById(user.getId())) {
                loggingUtils.logError("UserService", "update", new UserNotFoundException(user.getId()),
                        "userId=" + user.getId());
                throw new UserNotFoundException(user.getId());
            }

            // Capture old user data for event
            User existingUser = userRepository.findById(user.getId()).orElse(null);
            String oldEmail = existingUser != null ? existingUser.getEmail() : null;

            User saved = userRepository.save(user);
            loggingUtils.logServiceEnd("UserService", "update", "updatedUserId=" + saved.getId(),
                    "email=" + saved.getEmail());

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("update", startTime, endTime);

            // Publish user updated event
            userEventPublisher.publishUserUpdated(
                    UserUpdatedEvent.builder()
                            .userId(saved.getId())
                            .email(saved.getEmail())
                            .oldEmail(oldEmail)
                            .firstName(saved.getFirstName())
                            .lastName(saved.getLastName())
                            .phoneNumber(saved.getPhoneNumber())
                            .updatedAt(java.time.LocalDateTime.now())
                            .updatedBy(SecurityUtils.getCurrentUserLogin())
                            .build());

            return saved;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "update", e, "userId=" + user.getId());
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id:' + #id"),
            @CacheEvict(value = "users", key = "'all'", allEntries = true),
            @CacheEvict(value = "users", key = "'status:*'", allEntries = true),
            @CacheEvict(value = "users", key = "'firstName:*'", allEntries = true),
            @CacheEvict(value = "users", key = "'lastName:*'", allEntries = true)
    })
    @Transactional
    @Auditable(action = "USER_DELETE", context = "User account deletion")
    public void deleteById(Long id) {
        long startTime = System.currentTimeMillis();

        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "deleteById", "userId=" + id);

        try {
            if (!userRepository.existsById(id)) {
                loggingUtils.logError("UserService", "deleteById", new UserNotFoundException(id), "userId=" + id);
                throw new UserNotFoundException(id);
            }

            userRepository.deleteById(id);
            loggingUtils.logServiceEnd("UserService", "deleteById", "deletedUserId=" + id);

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("deleteById", startTime, endTime);
        } catch (Exception e) {
            loggingUtils.logError("UserService", "deleteById", e, "userId=" + id);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id:' + #id"),
            @CacheEvict(value = "users", key = "'all'", allEntries = true),
            @CacheEvict(value = "users", key = "'status:' + #status", allEntries = true),
            @CacheEvict(value = "users", key = "'firstName:' + #user.firstName", allEntries = true),
            @CacheEvict(value = "users", key = "'lastName:' + #user.lastName", allEntries = true)
    })
    @Transactional
    @Auditable(action = "USER_STATUS_CHANGE", context = "Account status modification")
    public UserResponse updateStatus(Long id, AccountStatus status) {
        long startTime = System.currentTimeMillis();

        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "updateStatus", "userId=" + id, "newStatus=" + status);

        try {
            User user = require(id);
            AccountStatus oldStatus = user.getStatus(); // Capture old status

            user.setStatus(status);
            userRepository.save(user);

            loggingUtils.logServiceEnd("UserService", "updateStatus", "userId=" + user.getId(),
                    "statusChangedTo=" + status);

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("updateStatus", startTime, endTime);

            // Publish user status changed event
            userEventPublisher.publishUserStatusChanged(
                    UserStatusChangedEvent.builder()
                            .userId(user.getId())
                            .oldStatus(oldStatus)
                            .newStatus(status)
                            .changedBy(SecurityUtils.getCurrentUserLogin())
                            .changedAt(java.time.LocalDateTime.now())
                            .build());

            return UserResponse.fromUser(user);
        } catch (Exception e) {
            loggingUtils.logError("UserService", "updateStatus", e, "userId=" + id, "status=" + status);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id:' + #id"),
            @CacheEvict(value = "users", key = "'all'", allEntries = true)
    })
    @Transactional
    @Auditable(action = "USER_ROLES_CHANGE", context = "User roles modification")
    public UserResponse updateRoles(Long id, Set<Role> roles) {
        long startTime = System.currentTimeMillis();

        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "updateRoles", "userId=" + id, "newRolesCount=" + roles.size());

        try {
            User user = require(id);
            Set<Role> oldRoles = Set.copyOf(user.getRoles()); // Capture old roles

            user.setRoles(roles);
            userRepository.save(user);

            loggingUtils.logServiceEnd("UserService", "updateRoles", "userId=" + user.getId(),
                    "rolesUpdatedTo=" + roles);

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("updateRoles", startTime, endTime);

            // Publish user role changed event
            userEventPublisher.publishUserRoleChanged(
                    UserRoleChangedEvent.builder()
                            .userId(user.getId())
                            .oldRoles(oldRoles)
                            .newRoles(roles)
                            .changedBy(SecurityUtils.getCurrentUserLogin())
                            .changedAt(java.time.LocalDateTime.now())
                            .build());

            return UserResponse.fromUser(user);
        } catch (Exception e) {
            loggingUtils.logError("UserService", "updateRoles", e, "userId=" + id, "roles=" + roles);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id:' + #id"),
            @CacheEvict(value = "users", key = "'all'", allEntries = true),
            @CacheEvict(value = "users", key = "'email:' + #email"),
            @CacheEvict(value = "users", key = "'exists:email:' + #email"),
            @CacheEvict(value = "users", key = "'firstName:' + #user.firstName", allEntries = true),
            @CacheEvict(value = "users", key = "'lastName:' + #user.lastName", allEntries = true)
    })
    @Transactional
    @Auditable(action = "PASSWORD_CHANGE", context = "User password change")
    public void changePassword(Long id, String oldPassword, String newPassword) {
        long startTime = System.currentTimeMillis();

        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "changePassword", "userId=" + id);

        try {
            User user = require(id);
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("Old password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            loggingUtils.logServiceEnd("UserService", "changePassword", "userId=" + id, "passwordChanged=true");

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("changePassword", startTime, endTime);

            // Publish user password changed event
            userEventPublisher.publishUserPasswordChanged(
                    UserPasswordChangedEvent.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .changedBy(SecurityUtils.getCurrentUserLogin())
                            .changedAt(java.time.LocalDateTime.now())
                            .ipAddress(getCurrentRequestIp())
                            .userAgent(getCurrentRequestUserAgent())
                            .build());

        } catch (Exception e) {
            loggingUtils.logError("UserService", "changePassword", e, "userId=" + id);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    @Transactional
    public User markLastLogin(Long id) {
        long startTime = System.currentTimeMillis();

        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "markLastLogin", "userId=" + id);

        try {
            User user = require(id);
            user.setLastLoginAt(Instant.now());
            User saved = userRepository.save(user);

            loggingUtils.logServiceEnd("UserService", "markLastLogin", "userId=" + id, "lastLoginUpdated=true");

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("markLastLogin", startTime, endTime);

            // Publish user login event
            userEventPublisher.publishUserLogin(
                    UserLoginEvent.builder()
                            .userId(saved.getId())
                            .email(saved.getEmail())
                            .loginTime(java.time.LocalDateTime.now())
                            .ipAddress(getCurrentRequestIp())
                            .userAgent(getCurrentRequestUserAgent())
                            .loginMethod("WEB")
                            .build());

            return saved;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "markLastLogin", e, "userId=" + id);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    private User require(Long id) {
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "require", "userId=" + id);

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException(id));

            loggingUtils.logServiceEnd("UserService", "require", "userId=" + id, "userFound=true");

            return user;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "require", e, "userId=" + id);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    public User getCurrentUser() {
        long startTime = System.currentTimeMillis();

        String currentUserEmail = SecurityUtils.getCurrentUserLogin();
        loggingUtils.setServiceName("UserService");
        loggingUtils.logServiceStart("UserService", "getCurrentUser", "email=" + currentUserEmail);

        try {
            User user = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new UserNotFoundException(currentUserEmail));

            loggingUtils.logServiceEnd("UserService", "getCurrentUser", "userId=" + user.getId(),
                    "email=" + currentUserEmail);

            long endTime = System.currentTimeMillis();
            loggingUtils.logPerformance("getCurrentUser", startTime, endTime);

            return user;
        } catch (Exception e) {
            loggingUtils.logError("UserService", "getCurrentUser", e, "email=" + currentUserEmail);
            throw e;
        } finally {
            loggingUtils.clearContext();
        }
    }

    private String getCurrentRequestIp() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest().getRemoteAddr();
        } catch (Exception e) {
            loggingUtils.logError("UserService", "getCurrentRequestIp", e);
            return "unknown";
        }
    }

    private String getCurrentRequestUserAgent() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            loggingUtils.logError("UserService", "getCurrentRequestUserAgent", e);
            return "unknown";
        }
    }
}
