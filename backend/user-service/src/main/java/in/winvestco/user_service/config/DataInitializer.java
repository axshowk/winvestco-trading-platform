package in.winvestco.user_service.config;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.model.User;
import in.winvestco.user_service.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final LoggingUtils loggingUtils;

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            loggingUtils.logServiceStart("DataInitializer", "initDatabase", "system startup");

            // Check if admin already exists
            if (userRepository.findByEmail("admin@winvestco.in").isEmpty()) {
                User admin = User.builder()
                        .email("admin@winvestco.in")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .firstName("Ashok")
                        .lastName("Sachan")
                        .clientId("WIN-" + java.util.UUID.randomUUID().toString().substring(0, 5))
                        .phoneNumber("8090960963")
                        .status(AccountStatus.ACTIVE)
                        .roles(Set.of(Role.ADMIN, Role.USER, Role.VIEWER))
                        .build();

                userRepository.save(admin);
                loggingUtils.logServiceEnd("DataInitializer", "initDatabase", "admin user created");
            } else {
                loggingUtils.logServiceEnd("DataInitializer", "initDatabase", "admin already exists");
            }
        };
    }

}
