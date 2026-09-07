package co.edu.uniminuto.hyeda.modules.clientes.repository;

import co.edu.uniminuto.hyeda.modules.clientes.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByDocument(String document);
    List<Customer> findByNameContainingIgnoreCase(String name);
}
