package co.edu.uniminuto.hyeda.rental.service;

import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.domain.RentalItem;
import co.edu.uniminuto.hyeda.shared.company.CompanySettingsService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Unico lugar donde se calcula dinero.
 *
 * <p>Concentrar aqui la aritmetica evita que el contrato, la pantalla del
 * wizard, la factura final y las analiticas muestren cifras distintas para el
 * mismo alquiler.
 *
 * <h2>Reglas</h2>
 * <ul>
 *   <li>Los dias se cuentan entre la fecha de entrega y la de devolucion,
 *       con minimo de un dia.</li>
 *   <li>Devolucion anticipada: se cobran solo los dias usados y se devuelve la
 *       diferencia.</li>
 *   <li>Entrega tardia: los dias pactados van a tarifa normal y el exceso al
 *       recargo por mora, descontando los dias de gracia de la empresa.</li>
 *   <li>Una extension corre la fecha pactada, de modo que esos dias se cobran
 *       a tarifa normal y no como mora.</li>
 * </ul>
 */
@Service
public class PricingService {

    private final CompanySettingsService companySettingsService;

    public PricingService(CompanySettingsService companySettingsService) {
        this.companySettingsService = companySettingsService;
    }

    /**
     * @return dias facturables entre dos fechas; nunca menos de uno, porque un
     *         alquiler de unas horas se cobra como dia completo
     */
    public int billableDays(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Faltan las fechas del alquiler");
        }
        if (to.isBefore(from)) {
            throw new BusinessException("La fecha de devolucion no puede ser anterior a la de entrega");
        }
        long days = ChronoUnit.DAYS.between(from, to);
        return (int) Math.max(1, days);
    }

    /**
     * Calcula la estimacion que se le comunica al cliente al abrir el alquiler.
     * Deja actualizados los totales del alquiler y de cada linea.
     */
    public Quote quoteEstimate(Rental rental) {
        int days = rental.getEstimatedDays();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (RentalItem item : rental.getItems()) {
            item.setEstimatedDays(days);
            BigDecimal line = Money.multiply(item.getUnitPrice(), (long) item.getQuantity() * days);
            item.setLineEstimated(line);
            subtotal = subtotal.add(line);
        }

        BigDecimal hauling = Boolean.TRUE.equals(rental.getHaulingEnabled())
                ? Money.of(rental.getHaulingCost())
                : Money.ZERO;
        BigDecimal discount = Money.of(rental.getDiscountAmount());

        validateDiscount(Money.sum(subtotal, hauling), discount, rental.getDiscountReason());

        BigDecimal total = Money.atLeastZero(Money.sum(subtotal, hauling).subtract(discount));

        rental.setItemsSubtotal(Money.of(subtotal));
        rental.setEstimatedTotal(total);

        return new Quote(Money.of(subtotal), hauling, discount, Money.ZERO, Money.ZERO, total);
    }

    /**
     * Liquidacion definitiva al recibir el material.
     *
     * @param incidentCharges cobros por danos y faltantes ya valorados
     */
    public Quote quoteFinal(Rental rental, LocalDate returnDate, BigDecimal incidentCharges) {
        int actualDays = billableDays(rental.getStartDate(), returnDate);
        int agreedDays = rental.getEstimatedDays();
        int graceDays = companySettingsService.get().getLateGraceDays();

        // Si devuelve antes, se cobran solo los dias usados; nunca mas de los pactados
        // a tarifa normal, porque el exceso va al recargo por mora.
        int chargedNormalDays = Math.min(actualDays, agreedDays);
        int lateDays = Math.max(0, actualDays - agreedDays - graceDays);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal lateFees = BigDecimal.ZERO;

        for (RentalItem item : rental.getItems()) {
            item.setActualDays(actualDays);

            BigDecimal line = Money.multiply(item.getUnitPrice(),
                    (long) item.getQuantity() * chargedNormalDays);
            item.setLineFinal(line);
            subtotal = subtotal.add(line);

            if (lateDays > 0) {
                BigDecimal lateFee = Money.isZero(item.getLateFeeSnapshot())
                        ? item.getUnitPrice()
                        : item.getLateFeeSnapshot();
                lateFees = lateFees.add(Money.multiply(lateFee, (long) item.getQuantity() * lateDays));
            }
        }

        BigDecimal hauling = Boolean.TRUE.equals(rental.getHaulingEnabled())
                ? Money.of(rental.getHaulingCost())
                : Money.ZERO;
        BigDecimal discount = Money.of(rental.getDiscountAmount());
        BigDecimal incidents = Money.of(incidentCharges);

        BigDecimal total = Money.atLeastZero(
                Money.sum(subtotal, hauling, lateFees, incidents).subtract(discount));

        rental.setActualDays(actualDays);
        rental.setActualReturnDate(returnDate);
        rental.setItemsSubtotal(Money.of(subtotal));
        rental.setLateFeeTotal(Money.of(lateFees));
        rental.setIncidentChargeTotal(incidents);
        rental.setFinalAmount(total);

        return new Quote(Money.of(subtotal), hauling, discount, Money.of(lateFees), incidents, total);
    }

    /** Valor de reposicion a cobrar por unidades danadas o no devueltas. */
    public BigDecimal incidentCharge(BigDecimal replacementValue, int quantity, boolean chargeable) {
        if (!chargeable || replacementValue == null) {
            return Money.ZERO;
        }
        return Money.multiply(replacementValue, quantity);
    }

    /** Un descuento sin motivo escrito no se acepta, y nunca puede superar el valor del alquiler. */
    public void validateDiscount(BigDecimal base, BigDecimal discount, String reason) {
        if (!Money.isPositive(discount)) {
            return;
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Todo descuento debe indicar el motivo por el que se otorga");
        }
        validateDiscountCap(base, discount);
    }

    public void validateDiscountCap(BigDecimal base, BigDecimal discount) {
        if (Money.isPositive(discount) && Money.gt(discount, base)) {
            throw BusinessException.of("El descuento (%s) no puede superar el valor del alquiler (%s)",
                    Money.format(discount), Money.format(base));
        }
    }

    /**
     * Detalle de una cotizacion.
     *
     * @param lateFees   recargo por dias de exceso
     * @param incidents  cobros por danos y faltantes
     */
    public record Quote(
            BigDecimal itemsSubtotal,
            BigDecimal hauling,
            BigDecimal discount,
            BigDecimal lateFees,
            BigDecimal incidents,
            BigDecimal total
    ) {
        public static Quote empty() {
            return new Quote(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);
        }
    }

    /** Simulacion para el wizard, sin persistir nada. */
    public BigDecimal simulate(List<LineInput> lines, int days, BigDecimal hauling, BigDecimal discount) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineInput line : lines) {
            subtotal = subtotal.add(Money.multiply(line.unitPrice(), (long) line.quantity() * days));
        }
        BigDecimal base = Money.sum(subtotal, hauling);
        validateDiscountCap(base, discount);
        return Money.atLeastZero(base.subtract(Money.of(discount)));
    }

    public record LineInput(BigDecimal unitPrice, int quantity) {
    }
}
