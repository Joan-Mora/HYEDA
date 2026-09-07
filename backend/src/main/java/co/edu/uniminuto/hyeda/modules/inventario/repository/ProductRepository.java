package co.edu.uniminuto.hyeda.modules.inventario.repository;

import co.edu.uniminuto.hyeda.modules.inventario.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    List<Product> findByNameContainingIgnoreCase(String name);
    @Query("select p from Product p where p.stockAvailable <= p.minStockThreshold and p.active = true")
    List<Product> findLowStock();
}
