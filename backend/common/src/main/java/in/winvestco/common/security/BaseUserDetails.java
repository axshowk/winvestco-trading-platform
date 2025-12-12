package in.winvestco.common.security;

import org.springframework.security.core.userdetails.UserDetails;


/**
 * Base implementation of Spring Security's UserDetails interface.
 * This class can be extended by user service implementations.
 */
public abstract class BaseUserDetails implements UserDetails {
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
