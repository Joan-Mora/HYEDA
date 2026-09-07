package co.edu.uniminuto.hyeda.modules.clientes.repository;

import co.edu.uniminuto.hyeda.modules.clientes.entity.BackgroundCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackgroundCheckRepository extends JpaRepository<BackgroundCheck, Long> {
    List<BackgroundCheck> findByCustomerId(Long customerId);
    List<BackgroundCheck> findByDocument(String document);
    List<BackgroundCheck> findAllByOrderByCheckedAtDesc();
}
