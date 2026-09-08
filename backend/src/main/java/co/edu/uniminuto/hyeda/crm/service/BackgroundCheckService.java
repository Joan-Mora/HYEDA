package co.edu.uniminuto.hyeda.crm.service;

import co.edu.uniminuto.hyeda.crm.domain.BackgroundCheck;
import co.edu.uniminuto.hyeda.crm.domain.BackgroundCheckStatus;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.BackgroundCheckRequest;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.BackgroundCheckView;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.ResolveBackgroundCheckRequest;
import co.edu.uniminuto.hyeda.crm.repository.BackgroundCheckRepository;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.integrations.port.BackgroundCheckPort;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.security.CurrentUser;
import co.edu.uniminuto.hyeda.shared.storage.FileStorageService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class BackgroundCheckService {

    private final BackgroundCheckRepository repository;
    private final CustomerRepository customerRepository;
    private final BackgroundCheckPort backgroundCheckPort;
    private final FileStorageService fileStorage;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public BackgroundCheckService(BackgroundCheckRepository repository,
                                  CustomerRepository customerRepository,
                                  BackgroundCheckPort backgroundCheckPort,
                                  FileStorageService fileStorage,
                                  AuditService auditService,
                                  CurrentUser currentUser) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.backgroundCheckPort = backgroundCheckPort;
        this.fileStorage = fileStorage;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    /**
     * Abre una consulta. Acepta una cedula suelta o la de un cliente ya
     * registrado; si el proveedor responde de inmediato, queda resuelta.
     */
    @Transactional
    public BackgroundCheckView consult(BackgroundCheckRequest request) {
        String document = request.document().trim();

        Long customerId = request.customerId();
        if (customerId == null) {
            customerId = customerRepository.findByDocument(document).map(Customer::getId).orElse(null);
        }

        BackgroundCheckPort.Result result = backgroundCheckPort.consult(document);

        BackgroundCheck check = BackgroundCheck.builder()
                .customerId(customerId)
                .document(document)
                .provider(backgroundCheckPort.providerName())
                .status(resolveStatus(result))
                .result(result.summary())
                .checkedBy(currentUser.idOrNull())
                .build();

        BackgroundCheck saved = repository.save(check);
        auditService.record("CONSULTAR_ANTECEDENTES", "BackgroundCheck", saved.getId(), document);
        return toView(saved);
    }

    /** Registra el veredicto del operador tras revisar el certificado. */
    @Transactional
    public BackgroundCheckView resolve(Long id, ResolveBackgroundCheckRequest request) {
        BackgroundCheck check = find(id);
        check.setStatus(request.status());
        if (request.result() != null && !request.result().isBlank()) {
            check.setResult(request.result().trim());
        }
        check.setObservations(request.observations());
        check.setCheckedBy(currentUser.idOrNull());

        BackgroundCheck saved = repository.save(check);
        auditService.record("RESOLVER_ANTECEDENTES", "BackgroundCheck", id, request.status().name());
        return toView(saved);
    }

    @Transactional
    public BackgroundCheckView attachEvidence(Long id, MultipartFile file) {
        BackgroundCheck check = find(id);
        check.setEvidencePath(fileStorage.store(file, FileStorageService.EVIDENCE));
        BackgroundCheck saved = repository.save(check);
        auditService.record("ADJUNTAR_EVIDENCIA_ANTECEDENTES", "BackgroundCheck", id,
                file.getOriginalFilename());
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public byte[] readEvidence(Long id) {
        BackgroundCheck check = find(id);
        if (check.getEvidencePath() == null) {
            throw new ResourceNotFoundException("Esta consulta no tiene evidencia adjunta");
        }
        return fileStorage.read(check.getEvidencePath());
    }

    @Transactional(readOnly = true)
    public PageResponse<BackgroundCheckView> list(Pageable pageable) {
        return PageResponse.from(repository.findAllByOrderByCheckedAtDesc(pageable), this::toView);
    }

    @Transactional(readOnly = true)
    public List<BackgroundCheckView> byCustomer(Long customerId) {
        return repository.findByCustomerIdOrderByCheckedAtDesc(customerId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<BackgroundCheckView> byDocument(String document) {
        return repository.findByDocumentOrderByCheckedAtDesc(document.trim()).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public Optional<BackgroundCheckView> lastCheckFor(String document) {
        return repository.findFirstByDocumentOrderByCheckedAtDesc(document).map(this::toView);
    }

    private BackgroundCheck find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Consulta de antecedentes", id));
    }

    private BackgroundCheckStatus resolveStatus(BackgroundCheckPort.Result result) {
        if (!result.resolved()) {
            return "manual".equals(backgroundCheckPort.providerName())
                    ? BackgroundCheckStatus.PENDIENTE_MANUAL
                    : BackgroundCheckStatus.ENVIADO;
        }
        return result.hasRecords() ? BackgroundCheckStatus.CON_ANTECEDENTES
                : BackgroundCheckStatus.SIN_ANTECEDENTES;
    }

    private BackgroundCheckView toView(BackgroundCheck check) {
        String customerName = check.getCustomerId() == null ? null
                : customerRepository.findById(check.getCustomerId()).map(Customer::getName).orElse(null);
        return new BackgroundCheckView(check.getId(), check.getCustomerId(), customerName, check.getDocument(),
                check.getStatus(), check.getResult(), check.getObservations(), check.getProvider(),
                check.getEvidencePath() != null, check.getCheckedAt());
    }
}
