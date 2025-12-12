package in.winvestco.user_service.security;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.model.User;
import in.winvestco.user_service.repository.UserRepository;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of UserDetailsService that loads user-specific data.
 */
@Service
@RequiredArgsConstructor    
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoggingUtils loggingUtils;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        loggingUtils.logServiceStart("UserDetailsServiceImpl", "loadUserByUsername", new Object[]{email});
        
        try {
            // Debug: Log the incoming email
            loggingUtils.logDebug("Attempting to load user with email: " + email, 
                Map.of("service", "UserDetailsServiceImpl", "operation", "loadUserByUsername", "email", email));
            
            // Debug: Check if email is null or empty
            if (email == null || email.trim().isEmpty()) {
                String errorMsg = "Email is null or empty";
                loggingUtils.logError("UserDetailsServiceImpl", "loadUserByUsername", 
                    new UsernameNotFoundException(errorMsg), new Object[]{errorMsg});
                throw new UsernameNotFoundException(errorMsg);
            }
            
            // Debug: Log before database query
            loggingUtils.logDebug("Querying database for user with email: " + email, 
                Map.of("service", "UserDetailsServiceImpl", "operation", "loadUserByUsername", "email", email));
                
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        String errorMsg = "User not found with email: " + email;
                        loggingUtils.logError("UserDetailsServiceImpl", "loadUserByUsername", 
                            new UsernameNotFoundException(errorMsg), errorMsg);
                        return new UsernameNotFoundException(errorMsg);
                    });
            
            // Debug: Log user found
            loggingUtils.logDebug("User found in database", 
                Map.of("service", "UserDetailsServiceImpl", 
                       "operation", "loadUserByUsername",
                       "userId", user.getId(),
                       "status", user.getStatus(),
                       "roles", user.getRoles()));
                    
            if (user.getStatus() != AccountStatus.ACTIVE) {
                String errorMsg = "User is not active: " + email;
                loggingUtils.logError("UserDetailsServiceImpl", "loadUserByUsername", 
                    new UsernameNotFoundException(errorMsg), new Object[]{errorMsg});
                throw new UsernameNotFoundException(errorMsg);
            }
            
            List<GrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .collect(Collectors.toList());
            
            // Debug: Log authorities
            loggingUtils.logDebug("Assigned authorities", 
                Map.of("service", "UserDetailsServiceImpl",
                       "operation", "loadUserByUsername",
                       "authorities", authorities));
                    
            UserDetails userDetails = new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                authorities
            );
            
            // Debug: Log successful user details creation
            loggingUtils.logDebug("Successfully created UserDetails", 
                Map.of("service", "UserDetailsServiceImpl",
                       "operation", "loadUserByUsername",
                       "email", user.getEmail()));
            
            loggingUtils.logServiceEnd("UserDetailsServiceImpl", "loadUserByUsername", new Object[]{user.getId()});
            
            return userDetails;
        } catch (Exception e) {
            // Log any unexpected errors
            loggingUtils.logError("UserDetailsServiceImpl", "loadUserByUsername", e, 
                new Object[]{"Error loading user with email: " + email});
            throw e;
        }
    }
}
