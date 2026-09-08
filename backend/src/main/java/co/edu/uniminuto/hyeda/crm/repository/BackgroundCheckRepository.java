package co.edu.uniminuto.hyeda.crm.repository;

import co.edu.uniminuto.hyeda.crm.domain.BackgroundCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BackgroundCheckRepository extends JpaRepository<BackgroundCheck, Long> {

    List<BackgroundCheck> findByCustomerIdOrderByCheckedAtDesc(Long customerId);

    List<BackgroundCheck> findByDocumentOrderByCheckedAtDesc(String document);

    Optional<BackgroundCheck> findFirstByDocumentOrderByCheckedAtDesc(String document);

    Page<BackgroundCheck> findAllByOrderByCheckedAtDesc(Pageable pageable);
}
