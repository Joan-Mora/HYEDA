package co.edu.uniminuto.hyeda.crm.repository;

import co.edu.uniminuto.hyeda.crm.domain.ReputationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReputationEventRepository extends JpaRepository<ReputationEvent, Long> {

    List<ReputationEvent> findTop20ByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
