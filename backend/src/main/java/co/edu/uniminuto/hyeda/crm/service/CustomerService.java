package co.edu.uniminuto.hyeda.crm.service;

import co.edu.uniminuto.hyeda.billing.repository.CustomerCreditRepository;
import co.edu.uniminuto.hyeda.billing.repository.DebtRepository;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.domain.CustomerAddress;
import co.edu.uniminuto.hyeda.crm.domain.CustomerType;
import co.edu.uniminuto.hyeda.crm.domain.IdDocumentType;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.AddressView;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.CustomerProfile;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.CustomerView;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.DebtSummary;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.DocumentSummary;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.HeldItem;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.RentalSummary;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.ReputationEventView;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.SaveAddressRequest;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.SaveCustomerRequest;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.documents.repository.GeneratedDocumentRepository;
import co.edu.uniminuto.hyeda.finance.service.DebtService;
import co.edu.uniminuto.hyeda.integrations.service.MapsService;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.storage.FileStorageService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BackgroundCheckService backgroundCheckService;
    private final ReputationService reputationService;
    private final RentalRepository rentalRepository;
    private final DebtRepository debtRepository;
    private final CustomerCreditRepository creditRepository;
    private final GeneratedDocumentRepository documentRepository;
    private final DebtService debtService;
    private final FileStorageService fileStorage;
    private final MapsService mapsService;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository,
                           BackgroundCheckService backgroundCheckService,
                           ReputationService reputationService,
                           RentalRepository rentalRepository,
                           DebtRepository debtRepository,
                           CustomerCreditRepository creditRepository,
                           GeneratedDocumentRepository documentRepository,
                           DebtService debtService,
                           FileStorageService fileStorage,
                           MapsService mapsService,
                           AuditService auditService) {
        this.customerRepository = customerRepository;
        this.backgroundCheckService = backgroundCheckService;
        this.reputationService = reputationService;
        this.rentalRepository = rentalRepository;
        this.debtRepository = debtRepository;
        this.creditRepository = creditRepository;
        this.documentRepository = documentRepository;
        this.debtService = debtService;
        this.fileStorage = fileStorage;
        this.mapsService = mapsService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerView> search(String query, CustomerType type, Boolean active, Pageable pageable) {
        String normalized = (query == null || query.isBlank()) ? null : query.trim();
        return PageResponse.from(customerRepository.search(normalized, type, active, pageable), this::toView);
    }

    @Transactional(readOnly = true)
    public CustomerView findById(Long id) {
        return toView(find(id));
    }

    @Transactional(readOnly = true)
    public CustomerProfile profile(Long id) {
        Customer customer = find(id);
        List<Rental> rentals = rentalRepository.findByCustomerIdOrderByCreatedAtDesc(id);
        List<RentalSummary> active = rentals.stream()
                .filter(rental -> !rental.getStatus().isTerminal())
                .map(this::toRentalSummary)
                .toList();
        List<RentalSummary> finished = rentals.stream()
                .filter(rental -> rental.getStatus().isTerminal())
                .map(this::toRentalSummary)
                .toList();

        List<HeldItem> held = rentals.stream()
                .filter(rental -> rental.getStatus().isMaterialOut())
                .flatMap(rental -> rental.getItems().stream()
                        .filter(item -> item.pendingReturn() > 0)
                        .map(item -> new HeldItem(item.getProductId(), item.getProductName(),
                                item.pendingReturn(), rental.getId(), rental.getCode(),
                                rental.getEndDate(), rental.isOverdue())))
                .toList();

        List<DebtSummary> debts = debtService.byCustomer(id).stream()
                .map(row -> new DebtSummary(row.id(), row.rentalId(), row.rentalCode(), row.balance(),
                        row.dueDate(), row.overdueDays(), row.status().name()))
                .toList();

        List<ReputationEventView> reputation = reputationService.history(id).stream()
                .map(event -> new ReputationEventView(event.getId(), event.getEventType(), event.getDelta(),
                        event.getScoreAfter(), event.getReason(), event.getRentalId(), event.getCreatedAt()))
                .toList();

        List<DocumentSummary> documents = documentRepository.findByCustomerIdOrderByGeneratedAtDesc(id).stream()
                .map(doc -> new DocumentSummary(doc.getId(), doc.getDocumentType().name(),
                        doc.getSerialNumber(), doc.getRentalId(), doc.getGeneratedAt()))
                .toList();

        BigDecimal lifetime = Money.of(rentalRepository.lifetimeValue(id));
        return new CustomerProfile(toView(customer), active, finished, held, debts,
                Money.of(debtRepository.openBalanceForCustomer(id)),
                Money.of(creditRepository.availableCredit(id)), lifetime,
                reputation, backgroundCheckService.byCustomer(id), documents);
    }

    @Transactional(readOnly = true)
    public CustomerView findByDocument(String document) {
        return customerRepository.findByDocument(document.trim())
                .map(this::toView)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay ningun cliente con documento " + document));
    }

    @Transactional
    public CustomerView create(SaveCustomerRequest request) {
        String document = request.document().trim();
        if (customerRepository.existsByDocument(document)) {
            throw BusinessException.of("Ya existe un cliente con documento %s", document);
        }
        if (!Boolean.TRUE.equals(request.dataConsent())) {
            throw new BusinessException(
                    "El cliente debe aceptar el tratamiento de datos personales para poder registrarlo");
        }

        Customer customer = Customer.builder()
                .document(document)
                .documentType(request.documentType() == null ? IdDocumentType.CC : request.documentType())
                .name(request.name().trim())
                .phone(blankToNull(request.phone()))
                .email(blankToNull(request.email()))
                .notes(blankToNull(request.notes()))
                .dataConsent(true)
                .dataConsentAt(LocalDateTime.now())
                .addresses(buildAddresses(request.addresses()))
                .build();

        Customer saved = customerRepository.save(customer);
        auditService.record("CREAR_CLIENTE", "Customer", saved.getId(),
                "%s (%s)".formatted(saved.getName(), saved.getDocument()));
        return toView(saved);
    }

    @Transactional
    public CustomerView update(Long id, SaveCustomerRequest request) {
        Customer customer = find(id);

        String document = request.document().trim();
        if (!document.equals(customer.getDocument()) && customerRepository.existsByDocument(document)) {
            throw BusinessException.of("Ya existe otro cliente con documento %s", document);
        }

        customer.setDocument(document);
        customer.setDocumentType(request.documentType() == null ? IdDocumentType.CC : request.documentType());
        customer.setName(request.name().trim());
        customer.setPhone(blankToNull(request.phone()));
        customer.setEmail(blankToNull(request.email()));
        customer.setNotes(blankToNull(request.notes()));

        // El consentimiento se puede otorgar pero nunca se revoca en silencio.
        if (Boolean.TRUE.equals(request.dataConsent()) && !Boolean.TRUE.equals(customer.getDataConsent())) {
            customer.setDataConsent(true);
            customer.setDataConsentAt(LocalDateTime.now());
        }

        if (request.addresses() != null) {
            customer.getAddresses().clear();
            customer.getAddresses().addAll(buildAddresses(request.addresses()));
        }

        Customer saved = customerRepository.save(customer);
        auditService.record("EDITAR_CLIENTE", "Customer", id, saved.getName());
        return toView(saved);
    }

    /**
     * Adjunta la copia de la cedula. Si el cliente ya tenia una, se reemplaza;
     * el wizard de alquiler nunca la vuelve a pedir mientras exista.
     */
    @Transactional
    public CustomerView uploadIdCopy(Long id, MultipartFile file) {
        Customer customer = find(id);
        String path = fileStorage.store(file, FileStorageService.ID_COPIES);
        customer.setIdCopyPath(path);
        Customer saved = customerRepository.save(customer);

        auditService.record("ADJUNTAR_CEDULA", "Customer", id, file.getOriginalFilename());
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public byte[] readIdCopy(Long id) {
        Customer customer = find(id);
        if (!customer.hasIdCopy()) {
            throw new ResourceNotFoundException("Este cliente no tiene copia de documento adjunta");
        }
        return fileStorage.read(customer.getIdCopyPath());
    }

    @Transactional
    public CustomerView setActive(Long id, boolean active) {
        Customer customer = find(id);
        customer.setActive(active);
        Customer saved = customerRepository.save(customer);
        auditService.record(active ? "ACTIVAR_CLIENTE" : "DESACTIVAR_CLIENTE", "Customer", id, saved.getName());
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", id));
    }

    public CustomerView toView(Customer customer) {
        return new CustomerView(
                customer.getId(), customer.getDocument(), customer.getDocumentType(), customer.getName(),
                customer.getPhone(), customer.getEmail(), customer.hasIdCopy(),
                Boolean.TRUE.equals(customer.getDataConsent()), customer.getDataConsentAt(),
                customer.getReputationScore(), customer.getCustomerType(), customer.getRentalCount(),
                customer.canSkipContract(), customer.getNotes(), Boolean.TRUE.equals(customer.getActive()),
                customer.getCreatedAt(),
                customer.getAddresses().stream().map(this::toAddressView).toList(),
                backgroundCheckService.lastCheckFor(customer.getDocument()).orElse(null));
    }

    private RentalSummary toRentalSummary(Rental rental) {
        return new RentalSummary(rental.getId(), rental.getCode(), rental.getStatus().name(),
                rental.getStartDate(), rental.getEndDate(), rental.getActualReturnDate(),
                rental.currentAmount(), rental.getItems().size(), rental.getDeliveryAddress(),
                rental.isOverdue());
    }

    private AddressView toAddressView(CustomerAddress address) {
        return new AddressView(address.getId(), address.getLabel(), address.getAddress(),
                address.getReference(), address.getLat(), address.getLng(), address.getMapsUrl(),
                Boolean.TRUE.equals(address.getIsDefault()));
    }

    private List<CustomerAddress> buildAddresses(List<SaveAddressRequest> requests) {
        List<CustomerAddress> addresses = new ArrayList<>();
        if (requests == null) {
            return addresses;
        }
        boolean defaultAssigned = false;
        for (SaveAddressRequest request : requests) {
            if (request.address() == null || request.address().isBlank()) {
                continue;
            }
            boolean isDefault = Boolean.TRUE.equals(request.isDefault()) && !defaultAssigned;
            defaultAssigned = defaultAssigned || isDefault;

            addresses.add(CustomerAddress.builder()
                    .label(request.label() == null || request.label().isBlank() ? "OBRA" : request.label())
                    .address(request.address().trim())
                    .reference(blankToNull(request.reference()))
                    .lat(request.lat())
                    .lng(request.lng())
                    .mapsUrl(mapsService.buildShareUrl(request.address(), request.lat(), request.lng()))
                    .isDefault(isDefault)
                    .build());
        }
        // Si nadie quedo marcada, la primera es la principal.
        if (!defaultAssigned && !addresses.isEmpty()) {
            addresses.getFirst().setIsDefault(true);
        }
        return addresses;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
