package co.edu.uniminuto.hyeda.catalog.repository;

import co.edu.uniminuto.hyeda.catalog.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Bloqueo pesimista ({@code SELECT ... FOR UPDATE}) para serializar los
     * cambios de stock de un producto. Dos alquileres que pidan la misma unidad
     * a la vez se atienden en orden de llegada; el segundo ve el disponible ya
     * descontado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    @Query("""
            select p from Product p
            where (:search is null or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(p.sku, '')) like lower(concat('%', :search, '%')))
              and (:category is null or p.category = :category)
              and (:active is null or p.active = :active)
            """)
    Page<Product> search(@Param("search") String search,
                         @Param("category") String category,
                         @Param("active") Boolean active,
                         Pageable pageable);

    @Query("select p from Product p where p.active = true and p.stockAvailable <= p.minStockThreshold "
            + "order by p.stockAvailable asc")
    List<Product> findLowStock();

    @Query("select distinct p.category from Product p where p.category is not null order by p.category")
    List<String> findCategories();

    List<Product> findByActiveTrueOrderByNameAsc();
}
