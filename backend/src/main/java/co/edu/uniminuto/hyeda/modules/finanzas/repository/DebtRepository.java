package co.edu.uniminuto.hyeda.modules.finanzas.repository;

import co.edu.uniminuto.hyeda.modules.finanzas.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByCustomerId(Long customerId);
    List<Debt> findByStatus(co.edu.uniminuto.hyeda.modules.finanzas.entity.DebtStatus status);
}
