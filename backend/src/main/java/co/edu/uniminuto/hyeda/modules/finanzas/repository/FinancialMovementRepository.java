package co.edu.uniminuto.hyeda.modules.finanzas.repository;

import co.edu.uniminuto.hyeda.modules.finanzas.entity.FinancialMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialMovementRepository extends JpaRepository<FinancialMovement, Long> {
    List<FinancialMovement> findByMovementDateBetween(java.time.LocalDate from, java.time.LocalDate to);
}
