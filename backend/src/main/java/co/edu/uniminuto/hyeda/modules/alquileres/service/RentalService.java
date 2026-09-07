package co.edu.uniminuto.hyeda.modules.alquileres.service;

import co.edu.uniminuto.hyeda.config.HyedaProperties;
import co.edu.uniminuto.hyeda.config.StockLockService;
import co.edu.uniminuto.hyeda.modules.alquileres.dto.*;
import co.edu.uniminuto.hyeda.modules.alquileres.entity.*;
import co.edu.uniminuto.hyeda.modules.alquileres.repository.*;
import co.edu.uniminuto.hyeda.modules.auth.entity.UserAccount;
import co.edu.uniminuto.hyeda.modules.auth.repository.UserAccountRepository;
import co.edu.uniminuto.hyeda.modules.clientes.entity.Customer;
import co.edu.uniminuto.hyeda.modules.clientes.entity.CustomerType;
import co.edu.uniminuto.hyeda.modules.clientes.service.CustomerService;
import co.edu.uniminuto.hyeda.modules.documentos.service.DocumentService;
import co.edu.uniminuto.hyeda.modules.facturacion.entity.PaymentStatus;
import co.edu.uniminuto.hyeda.modules.facturacion.repository.PaymentRepository;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.Debt;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.DebtStatus;
import co.edu.uniminuto.hyeda.modules.finanzas.repository.DebtRepository;
import co.edu.uniminuto.hyeda.modules.integraciones.service.NotificationService;
import co.edu.uniminuto.hyeda.modules.inventario.entity.Product;
import co.edu.uniminuto.hyeda.modules.inventario.service.ProductService;
import co.edu.uniminuto.hyeda.shared.exception.BusinessException;
import co.edu.uniminuto.hyeda.shared.exception.ResourceNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RentalService {

    private final RentalRepository rentalRepository;
    private final StockReservationRepository stockReservationRepository;
    private final RentalExtensionRepository extensionRepository;
    private final ReturnIncidentRepository incidentRepository;
    private final PaymentRepository paymentRepository;
    private final DebtRepository debtRepository;
    private final UserAccountRepository userAccountRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final StockLockService stockLockService;
    private final HyedaProperties properties;
    private final DocumentService documentService;
    private final NotificationService notificationService;

    public RentalService(RentalRepository rentalRepository,
                         StockReservationRepository stockReservationRepository,
                         RentalExtensionRepository extensionRepository,
                         ReturnIncidentRepository incidentRepository,
                         PaymentRepository paymentRepository,
                         DebtRepository debtRepository,
                         UserAccountRepository userAccountRepository,
                         CustomerService customerService,
                         ProductService productService,
                         StockLockService stockLockService,
                         HyedaProperties properties,
                         @Lazy DocumentService documentService,
                         @Lazy NotificationService notificationService) {
        this.rentalRepository = rentalRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.extensionRepository = extensionRepository;
        this.incidentRepository = incidentRepository;
        this.paymentRepository = paymentRepository;
        this.debtRepository = debtRepository;
        this.userAccountRepository = userAccountRepository;
        this.customerService = customerService;
        this.productService = productService;
        this.stockLockService = stockLockService;
        this.properties = properties;
        this.documentService = documentService;
        this.notificationService = notificationService;
    }

    public List<RentalDto> findAll() {
        return rentalRepository.findAllWithDetails().stream().map(this::toDto).toList();
    }

    public RentalDto findById(Long id) {
        return toDto(get(id));
    }

    public List<RentalDto> byCustomer(Long customerId) {
        return rentalRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    @Transactional
    public RentalDto create(CreateRentalRequest request) {
        Customer customer = customerService.get(request.customerId());
        validateDiscount(request.discountAmount(), request.discountReason());
        DocumentType docType = resolveDocumentType(customer, request);
        LocalDate end = request.startDate().plusDays(request.estimatedDays());

        Rental rental = Rental.builder()
                .customer(customer)
                .user(currentUser())
                .status(RentalStatus.BORRADOR)
                .startDate(request.startDate())
                .endDate(end)
                .deliveryAddress(request.deliveryAddress())
                .deliveryReference(request.deliveryReference())
                .deliveryLat(request.deliveryLat())
                .deliveryLng(request.deliveryLng())
                .haulingEnabled(Boolean.TRUE.equals(request.haulingEnabled()))
                .haulingCost(nz(request.haulingCost()))
                .discountAmount(nz(request.discountAmount()))
                .discountReason(request.discountReason())
                .documentType(docType)
                .skipContract(Boolean.TRUE.equals(request.skipContract()))
                .estimatedDays(request.estimatedDays())
                .items(new ArrayList<>())
                .build();

        BigDecimal itemsTotal = BigDecimal.ZERO;
        Instant expires = Instant.now().plus(properties.getStockReservationMinutes(), ChronoUnit.MINUTES);

        for (RentalItemRequest itemReq : request.items()) {
            Product product = productService.get(itemReq.productId());
            stockLockService.lock(product.getId());
            try {
                productService.reserveStock(product, itemReq.quantity());
                RentalItem item = RentalItem.builder()
                        .rental(rental)
                        .product(product)
                        .quantity(itemReq.quantity())
                        .unitPrice(product.getDailyPrice())
                        .estimatedDays(request.estimatedDays())
                        .build();
                rental.getItems().add(item);
                itemsTotal = itemsTotal.add(product.getDailyPrice()
                        .multiply(BigDecimal.valueOf(itemReq.quantity()))
                        .multiply(BigDecimal.valueOf(request.estimatedDays())));
                stockReservationRepository.save(StockReservation.builder()
                        .product(product)
                        .rental(rental)
                        .quantity(itemReq.quantity())
                        .expiresAt(expires)
                        .consumed(false)
                        .build());
            } finally {
                stockLockService.unlock(product.getId());
            }
        }

        BigDecimal total = itemsTotal.add(nz(request.haulingCost())).subtract(nz(request.discountAmount()));
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
        rental.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        rental.setStatus(RentalStatus.RESERVADO);
        Rental saved = rentalRepository.save(rental);
        return toDto(saved);
    }

    @Transactional
    public RentalDto emitDocument(Long rentalId) {
        Rental rental = get(rentalId);
        if (rental.getStatus() != RentalStatus.RESERVADO && rental.getStatus() != RentalStatus.BORRADOR) {
            throw new BusinessException("Estado invalido para emitir documento");
        }
        documentService.generateForRental(rental);
        markReservationsConsumed(rental.getId());
        rental.setStatus(RentalStatus.DOCUMENTO_EMITIDO);
        return toDto(rentalRepository.save(rental));
    }

    @Transactional
    public RentalDto confirmDelivery(Long rentalId) {
        Rental rental = get(rentalId);
        if (rental.getStatus() != RentalStatus.DOCUMENTO_EMITIDO && rental.getStatus() != RentalStatus.RESERVADO) {
            throw new BusinessException("Estado invalido para confirmar entrega");
        }
        if (rental.getStatus() == RentalStatus.RESERVADO) {
            documentService.generateForRental(rental);
            markReservationsConsumed(rental.getId());
        }
        rental.setStatus(RentalStatus.EN_CURSO);
        Rental saved = rentalRepository.save(rental);
        notificationService.notifyRentalCreated(saved);
        return toDto(saved);
    }

    @Transactional
    public RentalDto extend(Long rentalId, ExtensionRequest request) {
        Rental rental = get(rentalId);
        if (rental.getStatus() != RentalStatus.EN_CURSO) {
            throw new BusinessException("Solo se pueden extender alquileres en curso");
        }
        extensionRepository.save(RentalExtension.builder().rental(rental).extraDays(request.extraDays()).build());
        rental.setEndDate(rental.getEndDate().plusDays(request.extraDays()));
        rental.setEstimatedDays(rental.getEstimatedDays() + request.extraDays());
        return toDto(rentalRepository.save(rental));
    }

    @Transactional
    public RentalDto registerReturn(Long rentalId, ReturnRentalRequest request) {
        Rental rental = get(rentalId);
        if (rental.getStatus() != RentalStatus.EN_CURSO) {
            throw new BusinessException("Solo se pueden devolver alquileres en curso");
        }
        LocalDate actual = request.actualReturnDate() == null ? LocalDate.now() : request.actualReturnDate();
        int actualDays = (int) Math.max(1, ChronoUnit.DAYS.between(rental.getStartDate(), actual));
        rental.setActualReturnDate(actual);
        rental.setActualDays(actualDays);

        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (RentalItem item : rental.getItems()) {
            item.setActualDays(actualDays);
            itemsTotal = itemsTotal.add(item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .multiply(BigDecimal.valueOf(actualDays)));
            productService.releaseStock(item.getProduct(), item.getQuantity());
        }
        BigDecimal finalAmount = itemsTotal.add(nz(rental.getHaulingCost())).subtract(nz(rental.getDiscountAmount()));
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;
        rental.setFinalAmount(finalAmount.setScale(2, RoundingMode.HALF_UP));

        if (request.incidentReason() != null && !request.incidentReason().isBlank()) {
            incidentRepository.save(ReturnIncident.builder()
                    .rental(rental)
                    .reason(request.incidentReason())
                    .description(request.incidentDescription())
                    .build());
        }

        if (rental.getDocumentType() == DocumentType.REMISION || Boolean.TRUE.equals(rental.getSkipContract())) {
            documentService.generateReturnSlip(rental);
        }

        rental.setStatus(RentalStatus.FACTURACION_FINAL);
        Rental saved = rentalRepository.save(rental);
        documentService.generateInvoice(saved);
        return toDto(saved);
    }

    @Transactional
    public RentalDto closeIfPaid(Long rentalId) {
        Rental rental = get(rentalId);
        BigDecimal due = rental.getFinalAmount() != null ? rental.getFinalAmount() : rental.getTotalAmount();
        BigDecimal paid = paymentRepository.findByRentalId(rentalId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.CONFIRMADO)
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (paid.compareTo(due) >= 0) {
            rental.setStatus(RentalStatus.CERRADO);
            customerService.adjustReputation(rental.getCustomer().getId(), BigDecimal.valueOf(2));
            notificationService.notifyPaymentComplete(rental);
        } else {
            BigDecimal balance = due.subtract(paid);
            rental.setStatus(RentalStatus.PENDIENTE_PAGO);
            debtRepository.save(Debt.builder()
                    .customer(rental.getCustomer())
                    .rental(rental)
                    .balance(balance)
                    .dueDate(LocalDate.now().plusDays(7))
                    .overdueDays(0)
                    .status(DebtStatus.ABIERTA)
                    .build());
        }
        return toDto(rentalRepository.save(rental));
    }

    @Transactional
    public void expireReservations() {
        Instant now = Instant.now();
        for (StockReservation reservation : stockReservationRepository.findByExpiresAtBeforeAndConsumedFalse(now)) {
            productService.releaseStock(reservation.getProduct(), reservation.getQuantity());
            reservation.setConsumed(true);
            stockReservationRepository.save(reservation);
            if (reservation.getRental() != null
                    && reservation.getRental().getStatus() == RentalStatus.RESERVADO) {
                reservation.getRental().setStatus(RentalStatus.CANCELADO);
                rentalRepository.save(reservation.getRental());
            }
        }
    }

    private void markReservationsConsumed(Long rentalId) {
        stockReservationRepository.findByRentalIdAndConsumedFalse(rentalId).forEach(r -> {
            r.setConsumed(true);
            stockReservationRepository.save(r);
        });
    }

    private DocumentType resolveDocumentType(Customer customer, CreateRentalRequest request) {
        if (customer.getCustomerType() != CustomerType.CONFIANZA && Boolean.TRUE.equals(request.skipContract())) {
            throw new BusinessException("Solo clientes de confianza pueden omitir el contrato");
        }
        if (Boolean.TRUE.equals(request.skipContract())) {
            return DocumentType.REMISION;
        }
        if (request.documentType() != null) return request.documentType();
        return customer.getCustomerType() == CustomerType.NUEVO ? DocumentType.CONTRATO : DocumentType.CONTRATO;
    }

    private void validateDiscount(BigDecimal amount, String reason) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0
                && (reason == null || reason.isBlank())) {
            throw new BusinessException("El motivo de descuento es obligatorio cuando hay descuento");
        }
    }

    private UserAccount currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByUsername(username).orElse(null);
    }

    public Rental get(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alquiler no encontrado"));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private RentalDto toDto(Rental r) {
        List<RentalItemDto> items = r.getItems() == null ? List.of() : r.getItems().stream()
                .map(i -> new RentalItemDto(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getEstimatedDays(),
                        i.getActualDays()))
                .toList();
        return new RentalDto(
                r.getId(),
                r.getCustomer().getId(),
                r.getCustomer().getName(),
                r.getStatus(),
                r.getStartDate(),
                r.getEndDate(),
                r.getActualReturnDate(),
                r.getDeliveryAddress(),
                r.getDeliveryReference(),
                r.getDeliveryLat(),
                r.getDeliveryLng(),
                r.getHaulingEnabled(),
                r.getHaulingCost(),
                r.getDiscountAmount(),
                r.getDiscountReason(),
                r.getDocumentType(),
                r.getSkipContract(),
                r.getTotalAmount(),
                r.getFinalAmount(),
                r.getEstimatedDays(),
                r.getActualDays(),
                items
        );
    }
}
