package co.edu.uniminuto.hyeda.modules.facturacion.repository;

import co.edu.uniminuto.hyeda.modules.facturacion.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByRentalId(Long rentalId);
}
