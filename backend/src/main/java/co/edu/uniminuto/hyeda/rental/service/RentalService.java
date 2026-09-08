package co.edu.uniminuto.hyeda.rental.service;

import co.edu.uniminuto.hyeda.billing.repository.CustomerCreditRepository;
import co.edu.uniminuto.hyeda.billing.repository.DebtRepository;
import co.edu.uniminuto.hyeda.billing.repository.PaymentRepository;
import co.edu.uniminuto.hyeda.catalog.domain.Product;
import co.edu.uniminuto.hyeda.catalog.repository.ProductRepository;
import co.edu.uniminuto.hyeda.catalog.service.StockService;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.domain.ReputationEventType;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.crm.service.ReputationService;
import co.edu.uniminuto.hyeda.integrations.service.MapsService;
import co.edu.uniminuto.hyeda.rental.domain.DocumentPolicy;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.domain.RentalDispatchedEvent;
import co.edu.uniminuto.hyeda.rental.domain.RentalExtension;
import co.edu.uniminuto.hyeda.rental.domain.RentalItem;
import co.edu.uniminuto.hyeda.rental.domain.RentalStatus;
import co.edu.uniminuto.hyeda.rental.domain.RentalStatusHistory;
import co.edu.uniminuto.hyeda.rental.domain.ReturnIncident;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.ChangeStatusRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.CreateRentalRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.ExtensionRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.ExtensionView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.IncidentRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.IncidentView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.QuoteLine;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.QuoteRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.QuoteView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalEligibility;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalItemRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalItemView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalRow;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.ReturnRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.StatusHistoryView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.UpdateRentalRequest;
import co.edu.uniminuto.hyeda.rental.repository.RentalExtensionRepository;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.rental.repository.RentalStatusHistoryRepository;
import co.edu.uniminuto.hyeda.rental.repository.ReturnIncidentRepository;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.security.CurrentUser;
import co.edu.uniminuto.hyeda.shared.storage.FileStorageService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orquesta el ciclo de vida completo del alquiler.
 *
 * <p>El flujo es lineal y cada paso deja rastro en el historial de estados:
 * abrir (con stock apartado), emitir documento, entregar, devolver y liquidar.
 */
@Slf4j
@Service
public class RentalService {

    private final RentalRepository rentalRepository;
    private final RentalStatusHistoryRepository historyRepository;
    private final RentalExtensionRepository extensionRepository;
    private final ReturnIncidentRepository incidentRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final DebtRepository debtRepository;
    private final CustomerCreditRepository creditRepository;
    private final StockService stockService;
    private final PricingService pricingService;
    private final RentalNumberService numberService;
    private final ReputationService reputationService;
    private final MapsService mapsService;
    private final FileStorageService fileStorage;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;

    public RentalService(RentalRepository rentalRepository,
                         RentalStatusHistoryRepository historyRepository,
                         RentalExtensionRepository extensionRepository,
                         ReturnIncidentRepository incidentRepository,
                         ProductRepository productRepository,
                         CustomerRepository customerRepository,
                         PaymentRepository paymentRepository,
                         DebtRepository debtRepository,
                         CustomerCreditRepository creditRepository,
                         StockService stockService,
                         PricingService pricingService,
                         RentalNumberService numberService,
                         ReputationService reputationService,
                         MapsService mapsService,
                         FileStorageService fileStorage,
                         AuditService auditService,
                         CurrentUser currentUser,
                         ApplicationEventPublisher events) {
        this.rentalRepository = rentalRepository;
        this.historyRepository = historyRepository;
        this.extensionRepository = extensionRepository;
        this.incidentRepository = incidentRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.debtRepository = debtRepository;
        this.creditRepository = creditRepository;
        this.stockService = stockService;
        this.pricingService = pricingService;
        this.numberService = numberService;
        this.reputationService = reputationService;
        this.mapsService = mapsService;
        this.fileStorage = fileStorage;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.events = events;
    }

    // ------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public PageResponse<RentalRow> search(RentalStatus status, Long customerId,
                                          LocalDate from, LocalDate to, Pageable pageable) {
        return PageResponse.from(rentalRepository.search(status, customerId, from, to, pageable), this::toRow);
    }

    @Transactional(readOnly = true)
    public RentalView findById(Long id) {
        return toView(find(id));
    }

    @Transactional(readOnly = true)
    public RentalView findByCode(String code) {
        return toView(rentalRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el alquiler " + code)));
    }

    @Transactional(readOnly = true)
    public List<RentalRow> active() {
        return rentalRepository.findActive().stream().map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public List<RentalRow> overdue() {
        return rentalRepository.findOverdue(LocalDate.now()).stream().map(this::toRow).toList();
    }

    /**
     * Informacion del primer paso del wizard: dice si al cliente se le puede
     * hacer solo remision y advierte sobre deudas o documentos faltantes.
     */
    @Transactional(readOnly = true)
    public RentalEligibility eligibility(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", customerId));

        BigDecimal debt = debtRepository.openBalanceForCustomer(customerId);
        BigDecimal credit = creditRepository.availableCredit(customerId);

        List<String> warnings = new ArrayList<>();
        if (!customer.hasIdCopy()) {
            warnings.add("Falta adjuntar la copia del documento de identidad");
        }
        if (!Boolean.TRUE.equals(customer.getDataConsent())) {
            warnings.add("El cliente no ha aceptado el tratamiento de datos");
        }
        if (Money.isPositive(debt)) {
            warnings.add("Tiene una deuda pendiente de " + Money.format(debt));
        }
        switch (customer.getCustomerType()) {
            case RESTRINGIDO -> warnings.add(
                    "Cliente restringido por mala reputacion: requiere autorizacion de la administradora");
            case NUEVO -> warnings.add("Cliente nuevo: el contrato de arrendamiento es obligatorio");
            default -> { }
        }
        if (!Boolean.TRUE.equals(customer.getActive())) {
            warnings.add("El cliente esta inactivo");
        }

        return new RentalEligibility(customer.getId(), customer.getName(), customer.getCustomerType().name(),
                customer.getReputationScore(), customer.getRentalCount(), customer.canSkipContract(),
                customer.hasIdCopy(), Boolean.TRUE.equals(customer.getDataConsent()), debt, credit,
                customer.canSkipContract() ? DocumentPolicy.REMISION : DocumentPolicy.CONTRATO,
                warnings);
    }

    /** Cotizacion en vivo del wizard; no reserva stock ni persiste nada. */
    @Transactional(readOnly = true)
    public QuoteView quote(QuoteRequest request) {
        int days = pricingService.billableDays(request.startDate(), request.endDate());
        List<QuoteLine> lines = new ArrayList<>();
        List<PricingService.LineInput> inputs = new ArrayList<>();

        for (RentalItemRequest item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Producto", item.productId()));
            BigDecimal lineTotal = Money.multiply(product.getDailyPrice(),
                    (long) item.quantity() * days);
            lines.add(new QuoteLine(product.getId(), product.getName(), item.quantity(),
                    product.getDailyPrice(), product.getStockAvailable(), lineTotal));
            inputs.add(new PricingService.LineInput(product.getDailyPrice(), item.quantity()));
        }

        BigDecimal hauling = Money.of(request.haulingCost());
        BigDecimal discount = Money.of(request.discountAmount());
        BigDecimal subtotal = lines.stream().map(QuoteLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = pricingService.simulate(inputs, days, hauling, discount);

        return new QuoteView(days, Money.of(subtotal), hauling, discount, total, lines);
    }

    // --------------------------------------------------------------- apertura

    /**
     * Abre el alquiler y aparta el stock en la misma transaccion. Si algun
     * equipo no alcanza, no se crea nada y el resto del stock queda libre.
     */
    @Transactional
    public RentalView create(CreateRentalRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", request.customerId()));

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new BusinessException("El cliente esta inactivo; actualiza su estado antes de alquilar");
        }
        if (!Boolean.TRUE.equals(customer.getDataConsent())) {
            throw new BusinessException(
                    "El cliente debe aceptar el tratamiento de datos antes de abrir un alquiler");
        }

        DocumentPolicy policy = resolvePolicy(customer, request.documentPolicy());
        int days = pricingService.billableDays(request.startDate(), request.endDate());

        Rental rental = Rental.builder()
                .code(numberService.nextCode())
                .customerId(customer.getId())
                .userId(currentUser.idOrNull())
                .status(RentalStatus.BORRADOR)
                .documentPolicy(policy)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .estimatedDays(days)
                .deliveryAddress(request.deliveryAddress().trim())
                .deliveryReference(blankToNull(request.deliveryReference()))
                .deliveryLat(request.deliveryLat())
                .deliveryLng(request.deliveryLng())
                .deliveryMapsUrl(mapsService.buildShareUrl(request.deliveryAddress(),
                        request.deliveryLat(), request.deliveryLng()))
                .haulingEnabled(Boolean.TRUE.equals(request.haulingEnabled()))
                .haulingCost(Boolean.TRUE.equals(request.haulingEnabled())
                        ? Money.of(request.haulingCost()) : Money.ZERO)
                .discountAmount(Money.of(request.discountAmount()))
                .discountReason(blankToNull(request.discountReason()))
                .dataConsentAccepted(true)
                .notes(blankToNull(request.notes()))
                .items(buildItems(request.items(), days))
                .build();

        pricingService.quoteEstimate(rental);
        List<RentalItem> lines = new ArrayList<>(rental.getItems());
        rental.getItems().clear();
        Rental saved = rentalRepository.save(rental);
        lines.forEach(item -> item.setRentalId(saved.getId()));
        saved.getItems().addAll(lines);
        Rental persisted = rentalRepository.save(saved);

        // El stock se aparta despues de tener el id, para dejar la reserva ligada al alquiler.
        reserveStock(persisted, request.sessionKey());

        transition(persisted, RentalStatus.RESERVADO, "Alquiler abierto con stock apartado");
        auditService.record("ABRIR_ALQUILER", "Rental", persisted.getId(),
                "%s para %s".formatted(persisted.getCode(), customer.getName()));

        return toView(rentalRepository.save(persisted));
    }

    @Transactional
    public RentalView update(Long id, UpdateRentalRequest request) {
        Rental rental = find(id);
        if (rental.getStatus() != RentalStatus.BORRADOR && rental.getStatus() != RentalStatus.RESERVADO) {
            throw BusinessException.of("Un alquiler en estado %s ya no se puede editar", rental.getStatus());
        }

        // Se sueltan las reservas actuales y se vuelven a tomar con las cantidades nuevas.
        stockService.releaseAllForRental(id, "Edicion del alquiler " + rental.getCode());

        int days = pricingService.billableDays(request.startDate(), request.endDate());
        rental.setStartDate(request.startDate());
        rental.setEndDate(request.endDate());
        rental.setEstimatedDays(days);
        rental.setDeliveryAddress(request.deliveryAddress().trim());
        rental.setDeliveryReference(blankToNull(request.deliveryReference()));
        rental.setDeliveryLat(request.deliveryLat());
        rental.setDeliveryLng(request.deliveryLng());
        rental.setDeliveryMapsUrl(mapsService.buildShareUrl(request.deliveryAddress(),
                request.deliveryLat(), request.deliveryLng()));
        rental.setHaulingEnabled(Boolean.TRUE.equals(request.haulingEnabled()));
        rental.setHaulingCost(Boolean.TRUE.equals(request.haulingEnabled())
                ? Money.of(request.haulingCost()) : Money.ZERO);
        rental.setDiscountAmount(Money.of(request.discountAmount()));
        rental.setDiscountReason(blankToNull(request.discountReason()));
        rental.setNotes(blankToNull(request.notes()));
        if (request.documentPolicy() != null) {
            Customer customer = customerRepository.findById(rental.getCustomerId()).orElseThrow();
            rental.setDocumentPolicy(resolvePolicy(customer, request.documentPolicy()));
        }

        rental.getItems().clear();
        rental.getItems().addAll(buildItems(request.items(), days));

        pricingService.quoteEstimate(rental);
        Rental saved = rentalRepository.save(rental);
        reserveStock(saved, null);

        auditService.record("EDITAR_ALQUILER", "Rental", id, saved.getCode());
        return toView(saved);
    }

    // ------------------------------------------------------------- transicion

    /** Confirma la salida del material hacia la obra. */
    @Transactional
    public RentalView dispatch(Long id) {
        Rental rental = find(id);
        requireStatus(rental, RentalStatus.DOCUMENTO_EMITIDO);

        stockService.dispatch(id);
        transition(rental, RentalStatus.EN_CURSO, "Material entregado al cliente");

        auditService.record("ENTREGAR_MATERIAL", "Rental", id, rental.getCode());
        events.publishEvent(new RentalDispatchedEvent(id, rental.getCode()));
        return toView(rentalRepository.save(rental));
    }

    /** Marca el documento como emitido; lo invoca el modulo de documentos. */
    @Transactional
    public Rental markDocumentIssued(Long id, String note) {
        Rental rental = find(id);
        if (rental.getStatus() == RentalStatus.DOCUMENTO_EMITIDO) {
            return rental;
        }
        requireStatus(rental, RentalStatus.RESERVADO);
        transition(rental, RentalStatus.DOCUMENTO_EMITIDO, note);
        return rentalRepository.save(rental);
    }

    @Transactional
    public RentalView extend(Long id, ExtensionRequest request) {
        Rental rental = find(id);
        if (rental.getStatus() != RentalStatus.EN_CURSO
                && rental.getStatus() != RentalStatus.DOCUMENTO_EMITIDO) {
            throw new BusinessException("Solo se puede extender un alquiler con material entregado");
        }

        LocalDate previousEnd = rental.getEndDate();
        LocalDate newEnd = previousEnd.plusDays(request.extraDays());

        rental.setEndDate(newEnd);
        rental.setEstimatedDays(pricingService.billableDays(rental.getStartDate(), newEnd));
        pricingService.quoteEstimate(rental);

        extensionRepository.save(RentalExtension.builder()
                .rentalId(id)
                .extraDays(request.extraDays())
                .previousEnd(previousEnd)
                .newEnd(newEnd)
                .reason(blankToNull(request.reason()))
                .approvedBy(currentUser.idOrNull())
                .build());

        historyRepository.save(RentalStatusHistory.builder()
                .rentalId(id)
                .fromStatus(rental.getStatus())
                .toStatus(rental.getStatus())
                .note("Extension de %d dia(s): nueva fecha %s".formatted(request.extraDays(), newEnd))
                .changedBy(currentUser.idOrNull())
                .build());

        auditService.record("EXTENDER_ALQUILER", "Rental", id,
                "%d dias hasta %s".formatted(request.extraDays(), newEnd));
        return toView(rentalRepository.save(rental));
    }

    /**
     * Recepcion del material y liquidacion definitiva.
     *
     * <p>Aqui se resuelve el caso central del negocio: si el cliente devolvio
     * antes, se le cobran solo los dias usados; si se paso, se suma la mora; y
     * si hubo danos o faltantes, se cobran y el equipo sale del inventario.
     */
    @Transactional
    public RentalView registerReturn(Long id, ReturnRequest request) {
        Rental rental = find(id);
        requireStatus(rental, RentalStatus.EN_CURSO);

        if (request.returnDate().isBefore(rental.getStartDate())) {
            throw new BusinessException("La fecha de devolucion no puede ser anterior a la de entrega");
        }
        if (request.returnDate().isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de devolucion no puede estar en el futuro");
        }

        Map<Long, RentalItem> itemsById = new HashMap<>();
        rental.getItems().forEach(item -> itemsById.put(item.getId(), item));

        // Cantidades que no vuelven al patio, por item.
        Map<Long, Integer> withheld = new HashMap<>();
        BigDecimal incidentCharges = BigDecimal.ZERO;

        List<IncidentRequest> incidents = request.incidents() == null ? List.of() : request.incidents();
        for (IncidentRequest incident : incidents) {
            RentalItem item = incident.rentalItemId() == null ? null : itemsById.get(incident.rentalItemId());
            if (incident.rentalItemId() != null && item == null) {
                throw new BusinessException("La novedad hace referencia a un equipo que no esta en el alquiler");
            }
            if (item != null && incident.quantity() > item.getQuantity()) {
                throw BusinessException.of("No puedes reportar %d unidades de '%s': el alquiler tiene %d",
                        incident.quantity(), item.getProductName(), item.getQuantity());
            }

            BigDecimal charge = resolveIncidentCharge(incident, item);
            incidentCharges = incidentCharges.add(charge);

            incidentRepository.save(ReturnIncident.builder()
                    .rentalId(id)
                    .rentalItemId(incident.rentalItemId())
                    .itemCondition(incident.itemCondition())
                    .quantity(incident.quantity())
                    .reason(incident.reason().trim())
                    .description(blankToNull(incident.description()))
                    .chargeAmount(charge)
                    .reportedBy(currentUser.idOrNull())
                    .build());

            if (item != null && incident.itemCondition().removesFromInventory()) {
                withheld.merge(item.getId(), incident.quantity(), Integer::sum);
            }
        }

        // Devolucion fisica al patio, descontando lo que no volvio.
        for (RentalItem item : rental.getItems()) {
            int lost = withheld.getOrDefault(item.getId(), 0);
            int returned = Math.max(0, item.getQuantity() - lost);

            if (returned > 0) {
                stockService.restock(item.getProductId(), returned, id,
                        "Devolucion del alquiler " + rental.getCode());
            }
            if (lost > 0) {
                stockService.writeOff(item.getProductId(), lost, id,
                        "Dano o faltante en el alquiler " + rental.getCode());
            }
            item.setQuantityReturned(returned);
        }

        pricingService.quoteFinal(rental, request.returnDate(), incidentCharges);
        if (request.notes() != null && !request.notes().isBlank()) {
            rental.setNotes(appendNote(rental.getNotes(), request.notes()));
        }

        transition(rental, RentalStatus.DEVUELTO, "Material recibido en el patio");
        transition(rental, RentalStatus.FACTURACION_FINAL,
                "Liquidacion final: " + Money.format(rental.getFinalAmount()));

        applyReputationForReturn(rental, incidents);

        auditService.record("REGISTRAR_DEVOLUCION", "Rental", id,
                "Total final %s".formatted(Money.format(rental.getFinalAmount())));

        return toView(rentalRepository.save(rental));
    }

    @Transactional
    public RentalView cancel(Long id, String reason) {
        Rental rental = find(id);
        if (rental.getStatus().isMaterialOut()) {
            throw new BusinessException("No puedes cancelar un alquiler con material en la calle");
        }
        if (rental.getStatus().isTerminal()) {
            throw new BusinessException("Este alquiler ya esta cerrado");
        }

        stockService.releaseAllForRental(id, "Alquiler cancelado: " + rental.getCode());
        transition(rental, RentalStatus.CANCELADO,
                reason == null || reason.isBlank() ? "Cancelado por el operador" : reason);

        auditService.record("CANCELAR_ALQUILER", "Rental", id, reason);
        return toView(rentalRepository.save(rental));
    }

    @Transactional
    public RentalView changeStatus(Long id, ChangeStatusRequest request) {
        Rental rental = find(id);
        transition(rental, request.status(), request.note());
        return toView(rentalRepository.save(rental));
    }

    @Transactional
    public RentalView attachSignature(Long id, byte[] pngContent) {
        Rental rental = find(id);
        String path = fileStorage.storeBytes(pngContent, FileStorageService.SIGNATURES, "png");
        rental.setSignaturePath(path);
        auditService.record("FIRMAR_ALQUILER", "Rental", id, rental.getCode());
        return toView(rentalRepository.save(rental));
    }

    // -------------------------------------------------------- uso interno

    @Transactional(readOnly = true)
    public Rental find(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Alquiler", id));
    }

    /**
     * Aplica una transicion validando la maquina de estados y dejandola en el
     * historial. Es el unico camino por el que cambia {@code status}.
     */
    @Transactional
    public void transition(Rental rental, RentalStatus target, String note) {
        RentalStatus current = rental.getStatus();
        if (current == target) {
            return;
        }
        if (!current.canTransitionTo(target)) {
            throw BusinessException.of("No se puede pasar de %s a %s", current, target);
        }

        rental.setStatus(target);
        if (target == RentalStatus.CERRADO) {
            rental.setClosedAt(LocalDateTime.now());
        }

        historyRepository.save(RentalStatusHistory.builder()
                .rentalId(rental.getId())
                .fromStatus(current)
                .toStatus(target)
                .note(note)
                .changedBy(currentUser.idOrNull())
                .build());
    }

    /** Suma un alquiler exitoso al historial del cliente. Lo llama el cierre de pago. */
    @Transactional
    public void registerSuccessfulClosure(Rental rental) {
        customerRepository.findById(rental.getCustomerId()).ifPresent(customer -> {
            boolean onTime = rental.getActualReturnDate() == null
                    || !rental.getActualReturnDate().isAfter(rental.getEndDate());
            reputationService.registerCompletedRental(customer, rental.getId(), onTime);
        });
    }

    // ------------------------------------------------------------- internos

    private DocumentPolicy resolvePolicy(Customer customer, DocumentPolicy requested) {
        if (requested == null) {
            return customer.canSkipContract() ? DocumentPolicy.REMISION : DocumentPolicy.CONTRATO;
        }
        // Un cliente que aun no se ha ganado la confianza no puede saltarse el contrato.
        if (requested == DocumentPolicy.REMISION && !customer.canSkipContract()) {
            throw BusinessException.of(
                    "%s todavia no es cliente de confianza: el alquiler requiere contrato de arrendamiento",
                    customer.getName());
        }
        return requested;
    }

    private List<RentalItem> buildItems(List<RentalItemRequest> requests, int days) {
        Set<Long> seen = new LinkedHashSet<>();
        List<RentalItem> items = new ArrayList<>();

        for (RentalItemRequest request : requests) {
            if (!seen.add(request.productId())) {
                throw new BusinessException("Hay un equipo repetido en la lista; suma las cantidades");
            }
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Producto", request.productId()));
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw BusinessException.of("El equipo '%s' esta inactivo", product.getName());
            }

            items.add(RentalItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(request.quantity())
                    .unitPrice(product.getDailyPrice())
                    .lateFeeSnapshot(product.getLateFeePerDay())
                    .estimatedDays(days)
                    .build());
        }
        return items;
    }

    private void reserveStock(Rental rental, String sessionKey) {
        for (RentalItem item : rental.getItems()) {
            stockService.reserve(item.getProductId(), item.getQuantity(), rental.getId(), sessionKey);
        }
    }

    private BigDecimal resolveIncidentCharge(IncidentRequest incident, RentalItem item) {
        if (incident.chargeAmount() != null) {
            return Money.atLeastZero(incident.chargeAmount());
        }
        if (item == null || !incident.itemCondition().removesFromInventory()) {
            return Money.ZERO;
        }
        // Sin monto explicito se cobra el valor de reposicion del equipo.
        return productRepository.findById(item.getProductId())
                .map(product -> Money.multiply(product.getReplacementValue(), incident.quantity()))
                .orElse(Money.ZERO);
    }

    private void applyReputationForReturn(Rental rental, List<IncidentRequest> incidents) {
        customerRepository.findById(rental.getCustomerId()).ifPresent(customer -> {
            for (IncidentRequest incident : incidents) {
                switch (incident.itemCondition()) {
                    case DANIO -> reputationService.register(customer, ReputationEventType.DANIO,
                            rental.getId(), "Dano reportado: " + incident.reason());
                    case FALTANTE -> reputationService.register(customer, ReputationEventType.FALTANTE,
                            rental.getId(), "Faltante reportado: " + incident.reason());
                    default -> { }
                }
            }
        });
    }

    private void requireStatus(Rental rental, RentalStatus expected) {
        if (rental.getStatus() != expected) {
            throw BusinessException.of("La operacion requiere el alquiler en estado %s, pero esta en %s",
                    expected, rental.getStatus());
        }
    }

    private String appendNote(String existing, String addition) {
        return existing == null || existing.isBlank() ? addition : existing + "\n" + addition;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // --------------------------------------------------------------- mapeo

    public RentalView toView(Rental rental) {
        Customer customer = customerRepository.findById(rental.getCustomerId()).orElse(null);
        BigDecimal paid = paymentRepository.netPaidForRental(rental.getId());
        BigDecimal pending = Money.atLeastZero(Money.subtract(rental.currentAmount(), paid));

        return new RentalView(
                rental.getId(), rental.getCode(), rental.getStatus(), rental.getDocumentPolicy(),
                rental.getCustomerId(),
                customer == null ? null : customer.getName(),
                customer == null ? null : customer.getDocument(),
                customer == null ? null : customer.getPhone(),
                rental.getStartDate(), rental.getEndDate(), rental.getActualReturnDate(),
                rental.getEstimatedDays(), rental.getActualDays(), rental.overdueDays(), rental.isOverdue(),
                rental.getDeliveryAddress(), rental.getDeliveryReference(),
                rental.getDeliveryLat(), rental.getDeliveryLng(), rental.getDeliveryMapsUrl(),
                mapsService.buildEmbedUrl(rental.getDeliveryAddress(), rental.getDeliveryLat(),
                        rental.getDeliveryLng()),
                Boolean.TRUE.equals(rental.getHaulingEnabled()), rental.getHaulingCost(),
                rental.getDiscountAmount(), rental.getDiscountReason(),
                rental.getItemsSubtotal(), rental.getEstimatedTotal(), rental.getLateFeeTotal(),
                rental.getIncidentChargeTotal(), rental.getFinalAmount(), rental.currentAmount(),
                Money.of(paid), pending, rental.getRefundAmount(), rental.getCreditApplied(),
                rental.getSignaturePath() != null, rental.getNotes(),
                rental.getCreatedAt(), rental.getClosedAt(),
                rental.getItems().stream().map(this::toItemView).toList(),
                historyRepository.findByRentalIdOrderByChangedAtAsc(rental.getId()).stream()
                        .map(h -> new StatusHistoryView(h.getFromStatus(), h.getToStatus(), h.getNote(),
                                h.getChangedAt())).toList(),
                extensionRepository.findByRentalIdOrderByRequestedAtAsc(rental.getId()).stream()
                        .map(e -> new ExtensionView(e.getId(), e.getExtraDays(), e.getPreviousEnd(),
                                e.getNewEnd(), e.getReason(), e.getRequestedAt())).toList(),
                incidentRepository.findByRentalIdOrderByReportedAtAsc(rental.getId()).stream()
                        .map(i -> toIncidentView(i, rental)).toList());
    }

    private RentalItemView toItemView(RentalItem item) {
        return new RentalItemView(item.getId(), item.getProductId(), item.getProductName(),
                item.getQuantity(), item.getQuantityReturned(), item.getUnitPrice(),
                item.getLateFeeSnapshot(), item.getEstimatedDays(), item.getActualDays(),
                item.getLineEstimated(), item.getLineFinal());
    }

    private IncidentView toIncidentView(ReturnIncident incident, Rental rental) {
        String productName = rental.getItems().stream()
                .filter(item -> item.getId().equals(incident.getRentalItemId()))
                .map(RentalItem::getProductName)
                .findFirst()
                .orElse(null);
        return new IncidentView(incident.getId(), incident.getRentalItemId(), productName,
                incident.getItemCondition(), incident.getQuantity(), incident.getReason(),
                incident.getDescription(), incident.getChargeAmount(),
                incident.getEvidencePath() != null, incident.getReportedAt());
    }

    public RentalRow toRow(Rental rental) {
        Customer customer = customerRepository.findById(rental.getCustomerId()).orElse(null);
        BigDecimal paid = paymentRepository.netPaidForRental(rental.getId());
        BigDecimal amount = rental.currentAmount();

        return new RentalRow(rental.getId(), rental.getCode(), rental.getStatus(), rental.getDocumentPolicy(),
                rental.getCustomerId(),
                customer == null ? null : customer.getName(),
                customer == null ? null : customer.getDocument(),
                rental.getStartDate(), rental.getEndDate(), rental.getActualReturnDate(),
                amount, Money.atLeastZero(Money.subtract(amount, paid)),
                rental.getItems().size(), rental.isOverdue(), rental.overdueDays(), rental.getCreatedAt());
    }

    /** Novedad de devolucion con evidencia fotografica adjunta. */
    @Transactional
    public void attachIncidentEvidence(Long incidentId, byte[] content, String extension) {
        ReturnIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Novedad", incidentId));
        incident.setEvidencePath(fileStorage.storeBytes(content, FileStorageService.EVIDENCE, extension));
        incidentRepository.save(incident);
    }
}
