package co.edu.uniminuto.hyeda.notifications.service;

import co.edu.uniminuto.hyeda.billing.domain.Debt;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.domain.RentalItem;
import co.edu.uniminuto.hyeda.shared.company.CompanySettings;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Redaccion de los mensajes de WhatsApp.
 *
 * <p>Se escriben aparte del envio para poder revisarlos y ajustarlos sin tocar
 * la mecanica de la cola. El tono es el del mostrador: claro, corto y con las
 * cifras completas, porque el cliente los lee en el celular a pie de obra.
 */
@Component
public class MessageComposer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Resumen que recibe el cliente al abrir el alquiler. */
    public String rentalSummary(CompanySettings company, Customer customer, Rental rental,
                                BigDecimal paid, BigDecimal pending) {
        StringBuilder text = new StringBuilder();
        text.append("Hola ").append(firstName(customer.getName())).append(", ")
                .append("gracias por alquilar con ").append(company.getTradeName()).append(".\n\n")
                .append("*Alquiler ").append(rental.getCode()).append("*\n")
                .append("Entrega: ").append(rental.getStartDate().format(DATE)).append("\n")
                .append("Devolucion pactada: ").append(rental.getEndDate().format(DATE))
                .append(" (").append(rental.getEstimatedDays()).append(" dias)\n")
                .append("Direccion: ").append(rental.getDeliveryAddress()).append("\n\n")
                .append("*Equipos*\n");

        for (RentalItem item : rental.getItems()) {
            text.append("- ").append(item.getQuantity()).append(" x ").append(item.getProductName())
                    .append(" (").append(Money.format(item.getUnitPrice())).append(" por dia)\n");
        }

        if (Boolean.TRUE.equals(rental.getHaulingEnabled())) {
            text.append("- Acarreo: ").append(Money.format(rental.getHaulingCost())).append("\n");
        }
        if (Money.isPositive(rental.getDiscountAmount())) {
            text.append("- Descuento aplicado: ").append(Money.format(rental.getDiscountAmount()))
                    .append("\n");
        }

        text.append("\n*Total estimado:* ").append(Money.format(rental.getEstimatedTotal())).append("\n");
        if (Money.isPositive(paid)) {
            text.append("Abonado: ").append(Money.format(paid)).append("\n");
        }
        if (Money.isPositive(pending)) {
            text.append("*Pendiente por pagar:* ").append(Money.format(pending)).append("\n");
        } else {
            text.append("Pago recibido completo. Muchas gracias.\n");
        }

        text.append("\nSi devuelve el material despues del ")
                .append(rental.getEndDate().format(DATE))
                .append(" se cobra recargo por dia de atraso:\n");
        for (RentalItem item : rental.getItems()) {
            BigDecimal lateFee = Money.isZero(item.getLateFeeSnapshot())
                    ? item.getUnitPrice() : item.getLateFeeSnapshot();
            text.append("- ").append(item.getProductName()).append(": ")
                    .append(Money.format(lateFee)).append(" por dia\n");
        }

        text.append("\nAvisenos con anticipacion la fecha de recogida.\n")
                .append(company.getTradeName()).append(" - ").append(company.getPhone());
        return text.toString();
    }

    /** Aviso de que se acerca la fecha pactada de devolucion. */
    public String dueReminder(CompanySettings company, Customer customer, Rental rental, long daysLeft) {
        String when = daysLeft <= 0 ? "hoy" : "en " + daysLeft + (daysLeft == 1 ? " dia" : " dias");
        return """
                Hola %s, le recordamos que el alquiler %s vence %s (%s).

                Si necesita mas tiempo podemos extenderlo; si no, coordinemos la recogida \
                para evitar el recargo por mora.

                %s - %s""".formatted(firstName(customer.getName()), rental.getCode(), when,
                rental.getEndDate().format(DATE), company.getTradeName(), company.getPhone());
    }

    /** Mensaje que acompana la cuenta de cobro al cerrar el alquiler. */
    public String invoiceCaption(CompanySettings company, Customer customer, Rental rental) {
        StringBuilder text = new StringBuilder();
        text.append("Hola ").append(firstName(customer.getName()))
                .append(", adjuntamos la cuenta de cobro del alquiler ")
                .append(rental.getCode()).append(".\n\n")
                .append("Periodo: ").append(rental.getStartDate().format(DATE)).append(" al ")
                .append((rental.getActualReturnDate() == null
                        ? rental.getEndDate() : rental.getActualReturnDate()).format(DATE)).append("\n")
                .append("Total: ").append(Money.format(rental.currentAmount())).append("\n");

        if (Money.isPositive(rental.getLateFeeTotal())) {
            text.append("Incluye recargo por mora: ")
                    .append(Money.format(rental.getLateFeeTotal())).append("\n");
        }
        if (Money.isPositive(rental.getIncidentChargeTotal())) {
            text.append("Incluye danos o faltantes: ")
                    .append(Money.format(rental.getIncidentChargeTotal())).append("\n");
        }
        if (Money.isPositive(rental.getRefundAmount())) {
            text.append("Se le devolvieron ").append(Money.format(rental.getRefundAmount()))
                    .append(" por devolucion anticipada.\n");
        }

        text.append("\nGracias por su confianza.\n").append(company.getTradeName());
        return text.toString();
    }

    /** Cobro al cliente que se paso de la fecha limite de pago. */
    public String debtReminder(CompanySettings company, Customer customer, Debt debt, String rentalCode) {
        return """
                Hola %s, le escribimos de %s.

                Tenemos pendiente el saldo del alquiler %s por *%s*, con %d dias de vencimiento \
                (fecha limite: %s).

                Por favor comuniquese con nosotros para acordar el pago.

                %s""".formatted(firstName(customer.getName()), company.getLegalName(),
                rentalCode == null ? "registrado" : rentalCode,
                Money.format(debt.getBalance()), debt.getOverdueDays(),
                debt.getDueDate() == null ? "no registrada" : debt.getDueDate().format(DATE),
                company.getPhone());
    }

    /** Texto que acompana el PDF del reporte diario que recibe la administradora. */
    public String dailyReportCaption(CompanySettings company, LocalDate date, int rentalsOpened,
                                     BigDecimal income, BigDecimal expense, BigDecimal openDebt,
                                     int unitsOut) {
        return """
                *Reporte del %s* - %s

                Alquileres abiertos: %d
                Ingresos del dia: %s
                Gastos del dia: %s
                Resultado: %s

                Equipos en la calle: %d unidades
                Cartera pendiente: %s

                El detalle completo va en el PDF adjunto.""".formatted(
                date.format(DATE), company.getTradeName(), rentalsOpened,
                Money.format(income), Money.format(expense),
                Money.format(Money.subtract(income, expense)), unitsOut, Money.format(openDebt));
    }

    /** Listado de morosos para el seguimiento semanal de la administradora. */
    public String overdueDigest(CompanySettings company, List<String> lines, BigDecimal total) {
        StringBuilder text = new StringBuilder("*Seguimiento de cartera* - ")
                .append(company.getTradeName()).append("\n\n");
        if (lines.isEmpty()) {
            return text.append("No hay clientes con deudas vencidas. Todo al dia.").toString();
        }
        text.append("Clientes con deuda vencida:\n");
        lines.forEach(line -> text.append("- ").append(line).append("\n"));
        return text.append("\n*Total por cobrar:* ").append(Money.format(total)).toString();
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName.trim().split("\\s+")[0];
    }
}
