package in.winvestco.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    private final PasswordConfig passwordConfig = new PasswordConfig();

    @Test
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        PasswordEncoder encoder = passwordConfig.passwordEncoder();

        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);
    }

    @Test
    void passwordEncoder_ShouldEncodePassword() {
        PasswordEncoder encoder = passwordConfig.passwordEncoder();
        String rawPassword = "mySecretPassword";

        String encodedPassword = encoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_ShouldNotMatchDifferentPasswords() {
        PasswordEncoder encoder = passwordConfig.passwordEncoder();
        String rawPassword = "mySecretPassword";
        String wrongPassword = "wrongPassword";

        String encodedPassword = encoder.encode(rawPassword);

        assertFalse(encoder.matches(wrongPassword, encodedPassword));
    }
}
