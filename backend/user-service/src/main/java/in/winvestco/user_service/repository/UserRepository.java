package in.winvestco.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.user_service.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByStatus(AccountStatus status);
    
    
    // Derived queries to support service methods
    List<User> findByFirstName(String firstName);
    List<User> findByLastName(String lastName);
    List<User> findByPhoneNumber(String phoneNumber);
    List<User> findAllByRolesContaining(Role role);
}
