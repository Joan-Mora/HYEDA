package co.edu.uniminuto.hyeda.modules.auth.repository;

import co.edu.uniminuto.hyeda.modules.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
