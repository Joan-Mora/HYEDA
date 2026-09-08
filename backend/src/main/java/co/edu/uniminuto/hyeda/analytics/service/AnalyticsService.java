package co.edu.uniminuto.hyeda.analytics.service;

import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.CustomerRank;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.Dashboard;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.HeldItem;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.IncidentSummary;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.Kpis;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.ProductRank;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.RentalDigestRow;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.RevenuePoint;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.StatusCount;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.StockAlert;
import co.edu.uniminuto.hyeda.billing.repository.DebtRepository;
import co.edu.uniminuto.hyeda.catalog.domain.Product;
import co.edu.uniminuto.hyeda.catalog.repository.ProductRepository;
import co.edu.uniminuto.hyeda.catalog.repository.StockLedgerRepository;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.finance.domain.MovementType;
import co.edu.uniminuto.hyeda.finance.repository.FinancialMovementRepository;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.domain.RentalItem;
import co.edu.uniminuto.hyeda.rental.domain.RentalStatus;
import co.edu.uniminuto.hyeda.rental.repository.RentalItemRepository;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.rental.repository.ReturnIncidentRepository;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Metricas del negocio para un rango de fechas.
 *
 * <p>Todo se calcula sobre el mismo rango para que las cifras del tablero, del
 * PDF y del Excel coincidan. Un alquiler cuenta en el rango por su fecha de
 * apertura; el dinero, por la fecha en que se movio la caja.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int TOP_SIZE = 10;

    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
    private final ReturnIncidentRepository incidentRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final DebtRepository debtRepository;
    private final FinancialMovementRepository movementRepository;
    private final StockLedgerRepository stockLedgerRepository;

    @Transactional(readOnly = true)
    public Dashboard dashboard(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        List<Rental> opened = rentalRepository.findCreatedBetween(start, end);
        List<Rental> closed = rentalRepository.findClosedBetween(start, end);

        return new Dashboard(from, to,
                kpis(from, to, opened, closed),
                topProducts(start, end),
                topCustomers(opened, true),
                topCustomers(opened, false),
                stockouts(start, end),
                incidents(start, end),
                revenueSeries(from, to, opened),
                rentalsByStatus(opened));
    }

    // ------------------------------------------------------------------- kpis

    private Kpis kpis(LocalDate from, LocalDate to, List<Rental> opened, List<Rental> closed) {
        BigDecimal income = Money.of(movementRepository.totalBetween(MovementType.INGRESO, from, to));
        BigDecimal expense = Money.of(movementRepository.totalBetween(MovementType.GASTO, from, to));

        BigDecimal billed = opened.stream()
                .filter(rental -> rental.getStatus() != RentalStatus.CANCELADO)
                .map(Rental::currentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Rental> active = rentalRepository.findActive();
        long overdue = active.stream().filter(Rental::isOverdue).count();

        int unitsOut = active.stream()
                .flatMap(rental -> rental.getItems().stream())
                .mapToInt(item -> item.getQuantity() - item.getQuantityReturned())
                .sum();

        long problemReturns = incidentRepository
                .findByReportedAtBetweenOrderByReportedAtDesc(from.atStartOfDay(), to.atTime(23, 59, 59))
                .stream()
                .map(incident -> incident.getRentalId())
                .distinct()
                .count();

        Set<Long> customers = new HashSet<>();
        opened.forEach(rental -> customers.add(rental.getCustomerId()));

        int billable = (int) opened.stream()
                .filter(rental -> rental.getStatus() != RentalStatus.CANCELADO).count();
        BigDecimal averageTicket = billable == 0 ? Money.ZERO
                : Money.of(billed.divide(BigDecimal.valueOf(billable), 2, RoundingMode.HALF_UP));

        return new Kpis(opened.size(), closed.size(), active.size(), (int) overdue,
                (int) problemReturns, income, expense, Money.subtract(income, expense),
                Money.of(billed), Money.of(debtRepository.totalOpenBalance()), averageTicket,
                unitsOut, customers.size());
    }

    // --------------------------------------------------------------- rankings

    private List<ProductRank> topProducts(LocalDateTime from, LocalDateTime to) {
        return rentalItemRepository.rankProducts(from, to).stream()
                .limit(TOP_SIZE)
                .map(row -> new ProductRank(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        Money.of((BigDecimal) row[3])))
                .toList();
    }

    /**
     * Cliente estrella, en las dos lecturas que pide el negocio: quien alquila
     * mas seguido y quien deja mas dinero.
     */
    private List<CustomerRank> topCustomers(List<Rental> rentals, boolean byCount) {
        Map<Long, long[]> counts = new HashMap<>();
        Map<Long, BigDecimal> amounts = new HashMap<>();

        for (Rental rental : rentals) {
            if (rental.getStatus() == RentalStatus.CANCELADO) {
                continue;
            }
            counts.computeIfAbsent(rental.getCustomerId(), key -> new long[1])[0]++;
            amounts.merge(rental.getCustomerId(), rental.currentAmount(), BigDecimal::add);
        }

        List<CustomerRank> ranks = new ArrayList<>();
        counts.forEach((customerId, count) -> {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            ranks.add(new CustomerRank(customerId,
                    customer == null ? "Cliente " + customerId : customer.getName(),
                    customer == null ? null : customer.getDocument(),
                    count[0], Money.of(amounts.getOrDefault(customerId, BigDecimal.ZERO))));
        });

        Comparator<CustomerRank> comparator = byCount
                ? Comparator.comparingLong(CustomerRank::rentals).reversed()
                        .thenComparing(CustomerRank::amount, Comparator.reverseOrder())
                : Comparator.comparing(CustomerRank::amount, Comparator.reverseOrder())
                        .thenComparing(Comparator.comparingLong(CustomerRank::rentals).reversed());

        return ranks.stream().sorted(comparator).limit(TOP_SIZE).toList();
    }

    /** Equipos que se quedaron sin unidades disponibles durante el rango. */
    private List<StockAlert> stockouts(LocalDateTime from, LocalDateTime to) {
        List<Long> ids = stockLedgerRepository.findProductIdsThatHitZero(from, to);
        List<StockAlert> alerts = new ArrayList<>();

        for (Long id : ids) {
            productRepository.findById(id).ifPresent(product -> alerts.add(toAlert(product)));
        }
        // Se suman los que estan bajos hoy aunque no hayan tocado cero en el rango.
        for (Product product : productRepository.findLowStock()) {
            if (!ids.contains(product.getId())) {
                alerts.add(toAlert(product));
            }
        }
        return alerts;
    }

    private StockAlert toAlert(Product product) {
        return new StockAlert(product.getId(), product.getName(), product.getStockAvailable(),
                product.getStockTotal(), product.isOutOfStock());
    }

    private List<IncidentSummary> incidents(LocalDateTime from, LocalDateTime to) {
        return incidentRepository.summarizeByCondition(from, to).stream()
                .map(row -> new IncidentSummary(String.valueOf(row[0]),
                        ((Number) row[1]).longValue(), Money.of((BigDecimal) row[2])))
                .toList();
    }

    /** Serie diaria de facturado, ingresos y gastos: el grafico del tablero. */
    private List<RevenuePoint> revenueSeries(LocalDate from, LocalDate to, List<Rental> opened) {
        Map<LocalDate, BigDecimal[]> byDate = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDate.put(day, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Rental rental : opened) {
            if (rental.getStatus() == RentalStatus.CANCELADO) {
                continue;
            }
            BigDecimal[] slot = byDate.get(rental.getCreatedAt().toLocalDate());
            if (slot != null) {
                slot[0] = slot[0].add(rental.currentAmount());
            }
        }
        for (Object[] row : movementRepository.dailyTotals(from, to)) {
            BigDecimal[] slot = byDate.get((LocalDate) row[0]);
            if (slot != null) {
                int index = row[1] == MovementType.INGRESO ? 1 : 2;
                slot[index] = slot[index].add((BigDecimal) row[2]);
            }
        }

        List<RevenuePoint> points = new ArrayList<>();
        byDate.forEach((date, slot) -> points.add(new RevenuePoint(date,
                Money.of(slot[0]), Money.of(slot[1]), Money.of(slot[2]))));
        return points;
    }

    private List<StatusCount> rentalsByStatus(List<Rental> rentals) {
        Map<RentalStatus, Long> counts = new LinkedHashMap<>();
        for (RentalStatus status : RentalStatus.values()) {
            counts.put(status, 0L);
        }
        rentals.forEach(rental -> counts.merge(rental.getStatus(), 1L, Long::sum));

        List<StatusCount> result = new ArrayList<>();
        counts.forEach((status, count) -> {
            if (count > 0) {
                result.add(new StatusCount(status.name(), count));
            }
        });
        return result;
    }

    // ------------------------------------------------------- apoyo a reportes

    @Transactional(readOnly = true)
    public List<RentalDigestRow> rentalsIn(LocalDate from, LocalDate to) {
        return rentalRepository.findCreatedBetween(from.atStartOfDay(), to.atTime(23, 59, 59)).stream()
                .map(this::toDigestRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalDigestRow> overdueRentals() {
        return rentalRepository.findOverdue(LocalDate.now()).stream()
                .map(this::toDigestRow)
                .toList();
    }

    /** Que equipo tiene cada cliente en su poder ahora mismo. */
    @Transactional(readOnly = true)
    public List<HeldItem> heldItems() {
        List<HeldItem> held = new ArrayList<>();
        for (Rental rental : rentalRepository.findActive()) {
            Customer customer = customerRepository.findById(rental.getCustomerId()).orElse(null);
            for (RentalItem item : rental.getItems()) {
                int pending = item.getQuantity() - item.getQuantityReturned();
                if (pending > 0) {
                    held.add(new HeldItem(item.getProductName(), pending,
                            customer == null ? "Cliente " + rental.getCustomerId() : customer.getName(),
                            rental.getCode(), rental.getEndDate(), rental.isOverdue()));
                }
            }
        }
        return held;
    }

    @Transactional(readOnly = true)
    public List<StockAlert> lowStock() {
        return productRepository.findLowStock().stream().map(this::toAlert).toList();
    }

    private RentalDigestRow toDigestRow(Rental rental) {
        Customer customer = customerRepository.findById(rental.getCustomerId()).orElse(null);
        return new RentalDigestRow(rental.getCode(),
                customer == null ? "Cliente " + rental.getCustomerId() : customer.getName(),
                rental.getStartDate(), rental.getEndDate(), rental.getStatus().name(),
                rental.currentAmount(), (int) rental.overdueDays());
    }
}
