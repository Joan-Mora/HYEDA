package co.edu.uniminuto.hyeda.imports;

import co.edu.uniminuto.hyeda.catalog.repository.ProductRepository;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.imports.domain.ImportJobType;
import co.edu.uniminuto.hyeda.imports.dto.ImportDtos.ImportJobView;
import co.edu.uniminuto.hyeda.imports.dto.ImportDtos.PreviewResponse;
import co.edu.uniminuto.hyeda.imports.service.ImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ImportFlowTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @WithMockUser(username = "admin", authorities = "IMPORTACION_EJECUTAR")
    void previewAndImportProductsAndCustomers() {
        MockMultipartFile products = csv("productos.csv", """
                codigo,nombre,precio,stock
                AND-01,Andamio tubular,25000,8
                CON-02,Concretera 1 saco,80000,2
                """);

        PreviewResponse preview = importService.preview(products, ImportJobType.PRODUCTOS, null);
        assertThat(preview.suggestedMapping()).containsKeys("sku", "name", "dailyPrice", "stockTotal");
        assertThat(preview.validRows()).isEqualTo(2);

        ImportJobView job = importService.execute(products, ImportJobType.PRODUCTOS, preview.suggestedMapping());
        assertThat(job.rowsOk()).isEqualTo(2);
        assertThat(productRepository.findBySkuIgnoreCase("AND-01")).isPresent();

        MockMultipartFile customers = csv("clientes.csv", """
                documento,nombre,telefono
                11223344,Pedro Obra,3201112233
                """);
        ImportJobView customersJob = importService.execute(customers, ImportJobType.CLIENTES, null);
        assertThat(customersJob.rowsOk()).isEqualTo(1);
        assertThat(customerRepository.findByDocument("11223344")).isPresent();
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
