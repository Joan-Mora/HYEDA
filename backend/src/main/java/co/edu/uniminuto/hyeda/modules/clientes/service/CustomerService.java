package co.edu.uniminuto.hyeda.modules.clientes.service;

import co.edu.uniminuto.hyeda.modules.clientes.dto.CustomerDto;
import co.edu.uniminuto.hyeda.modules.clientes.dto.CustomerRequest;
import co.edu.uniminuto.hyeda.modules.clientes.entity.Customer;
import co.edu.uniminuto.hyeda.modules.clientes.entity.CustomerType;
import co.edu.uniminuto.hyeda.modules.clientes.mapper.CustomerMapper;
import co.edu.uniminuto.hyeda.modules.clientes.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.shared.exception.BusinessException;
import co.edu.uniminuto.hyeda.shared.exception.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.util.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final FileStorageService fileStorageService;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerMapper customerMapper,
                           FileStorageService fileStorageService) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.fileStorageService = fileStorageService;
    }

    public List<CustomerDto> findAll(String q) {
        if (q != null && !q.isBlank()) {
            return customerRepository.findByNameContainingIgnoreCase(q).stream().map(customerMapper::toDto).toList();
        }
        return customerRepository.findAll().stream().map(customerMapper::toDto).toList();
    }

    public CustomerDto findById(Long id) {
        return customerMapper.toDto(get(id));
    }

    @Transactional
    public CustomerDto create(CustomerRequest request) {
        if (!Boolean.TRUE.equals(request.dataConsent())) {
            throw new BusinessException("Debe aceptar el tratamiento de datos personales");
        }
        customerRepository.findByDocument(request.document()).ifPresent(c -> {
            throw new BusinessException("Ya existe un cliente con ese documento");
        });
        Customer entity = customerMapper.toEntity(request);
        if (entity.getCustomerType() == null) entity.setCustomerType(CustomerType.NUEVO);
        if (entity.getReputationScore() == null) entity.setReputationScore(BigDecimal.valueOf(50));
        return customerMapper.toDto(customerRepository.save(entity));
    }

    @Transactional
    public CustomerDto update(Long id, CustomerRequest request) {
        Customer entity = get(id);
        customerMapper.update(entity, request);
        return customerMapper.toDto(customerRepository.save(entity));
    }

    @Transactional
    public CustomerDto uploadIdCopy(Long id, MultipartFile file) {
        Customer entity = get(id);
        entity.setIdCopyPath(fileStorageService.store(file, "id-copies"));
        return customerMapper.toDto(customerRepository.save(entity));
    }

    @Transactional
    public void adjustReputation(Long customerId, BigDecimal delta) {
        Customer c = get(customerId);
        BigDecimal score = c.getReputationScore().add(delta);
        if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) score = BigDecimal.valueOf(100);
        c.setReputationScore(score);
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) c.setCustomerType(CustomerType.CONFIANZA);
        else if (score.compareTo(BigDecimal.valueOf(60)) >= 0) c.setCustomerType(CustomerType.FRECUENTE);
        customerRepository.save(c);
    }

    public Customer get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }
}
