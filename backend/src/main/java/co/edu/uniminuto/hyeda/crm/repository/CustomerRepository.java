package co.edu.uniminuto.hyeda.crm.repository;

import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.domain.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByDocument(String document);

    boolean existsByDocument(String document);

    @Query("""
            select c from Customer c
            where (:search is null
                   or lower(c.name) like lower(concat('%', :search, '%'))
                   or c.document like concat('%', :search, '%')
                   or coalesce(c.phone, '') like concat('%', :search, '%'))
              and (:type is null or c.customerType = :type)
              and (:active is null or c.active = :active)
            """)
    Page<Customer> search(@Param("search") String search,
                          @Param("type") CustomerType type,
                          @Param("active") Boolean active,
                          Pageable pageable);
}
