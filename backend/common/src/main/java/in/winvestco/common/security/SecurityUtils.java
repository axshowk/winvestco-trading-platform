package in.winvestco.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utility class for Spring Security.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class, not meant to be instantiated
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user or null if not authenticated
     */
    public static String getCurrentUserLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
            return springSecurityUser.getUsername();
        } else if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new IllegalStateException("User not authenticated");
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.getAuthorities().stream()
                   .noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ANONYMOUS"));
    }

    /**
     * Check if the current user has a specific authority (security role).
     *
     * @param authority the authority to check
     * @return true if the current user has the authority, false otherwise
     */
    public static boolean hasCurrentUserThisAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.getAuthorities().stream()
                   .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

}
