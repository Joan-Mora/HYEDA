package co.edu.uniminuto.hyeda.billing.repository;

import co.edu.uniminuto.hyeda.billing.domain.CustomerCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerCreditRepository extends JpaRepository<CustomerCredit, Long> {

    List<CustomerCredit> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /** Saldo a favor disponible: los consumos se registran como montos negativos. */
    @Query("select coalesce(sum(c.amount), 0) from CustomerCredit c where c.customerId = :customerId")
    BigDecimal availableCredit(@Param("customerId") Long customerId);

    @Query("select coalesce(sum(c.amount), 0) from CustomerCredit c")
    BigDecimal totalOutstandingCredit();
}
