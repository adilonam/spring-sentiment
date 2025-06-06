package ma.code212.gateway.repository;

import ma.code212.gateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByKeycloakId(String keycloakId);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
}
