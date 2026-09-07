package co.edu.uniminuto.hyeda.modules.auth.repository;

import co.edu.uniminuto.hyeda.modules.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
}
