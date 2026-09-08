package co.edu.uniminuto.hyeda.rental;

import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.ProductView;
import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.SaveProductRequest;
import co.edu.uniminuto.hyeda.catalog.service.ProductService;
import co.edu.uniminuto.hyeda.crm.domain.IdDocumentType;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.CustomerView;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.SaveCustomerRequest;
import co.edu.uniminuto.hyeda.crm.service.CustomerService;
import co.edu.uniminuto.hyeda.documents.service.DocumentService;
import co.edu.uniminuto.hyeda.rental.domain.DocumentPolicy;
import co.edu.uniminuto.hyeda.rental.domain.RentalStatus;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.CreateRentalRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalItemRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.ReturnRequest;
import co.edu.uniminuto.hyeda.rental.service.RentalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RentalFlowTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private RentalService rentalService;

    @Autowired
    private DocumentService documentService;

    @Test
    @WithMockUser(username = "admin", authorities = {
            "CLIENTES_GESTIONAR", "INVENTARIO_GESTIONAR", "ALQUILERES_GESTIONAR", "ALQUILERES_VER"
    })
    void createDispatchAndReturnRental() {
        CustomerView customer = customerService.create(new SaveCustomerRequest(
                "99887766", IdDocumentType.CC, "Ana Patio", "0000000000", null,
                null, true, List.of()));

        ProductView product = productService.create(new SaveProductRequest(
                "VIB-01", "Vibrador de concreto", null, "Obra", "UNIDAD",
                new BigDecimal("40000"), null, new BigDecimal("800000"), 4, 1, true));

        LocalDate start = LocalDate.now();
        RentalView created = rentalService.create(new CreateRentalRequest(
                customer.id(), start, start.plusDays(2),
                List.of(new RentalItemRequest(product.id(), 1)),
                "Calle 10 # 8-20", "Obra del puente", null, null,
                false, BigDecimal.ZERO, BigDecimal.ZERO, null,
                DocumentPolicy.CONTRATO, "Prueba de flujo", true, "test-session"));

        assertThat(created.code()).startsWith("ALQ-");
        assertThat(created.estimatedTotal()).isEqualByComparingTo("80000");

        ProductView afterReserve = productService.findById(product.id());
        assertThat(afterReserve.stockAvailable()).isEqualTo(3);

        documentService.issueRentalDocument(created.id());
        RentalView dispatched = rentalService.dispatch(created.id());
        assertThat(dispatched.status()).isEqualTo(RentalStatus.EN_CURSO);

        RentalView returned = rentalService.registerReturn(created.id(),
                new ReturnRequest(start, List.of(), "Sin novedad"));
        assertThat(returned.status()).isIn(RentalStatus.DEVUELTO, RentalStatus.FACTURACION_FINAL);
        assertThat(productService.findById(product.id()).stockAvailable()).isEqualTo(4);
    }
}
