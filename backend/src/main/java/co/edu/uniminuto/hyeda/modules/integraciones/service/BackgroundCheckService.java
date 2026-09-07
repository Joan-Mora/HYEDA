package co.edu.uniminuto.hyeda.modules.integraciones.service;

import co.edu.uniminuto.hyeda.modules.clientes.entity.BackgroundCheck;
import co.edu.uniminuto.hyeda.modules.clientes.entity.Customer;
import co.edu.uniminuto.hyeda.modules.clientes.repository.BackgroundCheckRepository;
import co.edu.uniminuto.hyeda.modules.clientes.service.CustomerService;
import co.edu.uniminuto.hyeda.modules.integraciones.adapters.BackgroundCheckAdapter;
import co.edu.uniminuto.hyeda.shared.util.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class BackgroundCheckService {

    private final BackgroundCheckRepository repository;
    private final BackgroundCheckAdapter adapter;
    private final CustomerService customerService;
    private final FileStorageService fileStorageService;

    public BackgroundCheckService(BackgroundCheckRepository repository,
                                  BackgroundCheckAdapter adapter,
                                  CustomerService customerService,
                                  FileStorageService fileStorageService) {
        this.repository = repository;
        this.adapter = adapter;
        this.customerService = customerService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public BackgroundCheck consult(Long customerId, String document) {
        Customer customer = customerId == null ? null : customerService.get(customerId);
        String doc = document != null ? document : (customer == null ? null : customer.getDocument());
        Map<String, String> result = adapter.consult(doc);
        BackgroundCheck check = BackgroundCheck.builder()
                .customer(customer)
                .document(doc)
                .provider(result.get("provider"))
                .result(result.get("result") + " [" + result.get("status") + "]")
                .build();
        return repository.save(check);
    }

    @Transactional
    public BackgroundCheck attachEvidence(Long checkId, MultipartFile file, String observations) {
        BackgroundCheck check = repository.findById(checkId).orElseThrow();
        check.setEvidencePath(fileStorageService.store(file, "uploads"));
        if (observations != null) {
            check.setResult((check.getResult() == null ? "" : check.getResult() + " | ") + observations);
        }
        return repository.save(check);
    }

    public List<BackgroundCheck> byCustomer(Long customerId) {
        return repository.findByCustomerId(customerId);
    }

    public List<BackgroundCheck> findAll() {
        return repository.findAllByOrderByCheckedAtDesc();
    }
}
