package co.edu.uniminuto.hyeda.catalog.service;

import co.edu.uniminuto.hyeda.catalog.domain.Product;
import co.edu.uniminuto.hyeda.catalog.domain.StockLedgerEntry;
import co.edu.uniminuto.hyeda.catalog.domain.StockMovementType;
import co.edu.uniminuto.hyeda.catalog.domain.StockReservation;
import co.edu.uniminuto.hyeda.catalog.repository.ProductRepository;
import co.edu.uniminuto.hyeda.catalog.repository.StockLedgerRepository;
import co.edu.uniminuto.hyeda.catalog.repository.StockReservationRepository;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unico punto por el que se mueve el stock.
 *
 * <p>Todas las operaciones toman el producto con {@code SELECT ... FOR UPDATE},
 * de modo que dos transacciones concurrentes sobre el mismo producto se
 * serializan en la base de datos. El {@code @Version} de {@link Product} queda
 * como red de seguridad para escrituras que lleguen por otro camino.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final StockLedgerRepository ledgerRepository;
    private final StockReservationRepository reservationRepository;
    private final HyedaProperties properties;
    private final CurrentUser currentUser;

    /**
     * Aparta unidades. Falla si no hay disponible suficiente en el momento exacto
     * en que se ejecuta, ya con el registro bloqueado.
     */
    @Transactional
    public StockReservation reserve(Long productId, int quantity, Long rentalId, String sessionKey) {
        if (quantity <= 0) {
            throw new BusinessException("La cantidad a reservar debe ser mayor que cero");
        }
        Product product = lockProduct(productId);

        if (product.getStockAvailable() < quantity) {
            throw BusinessException.of(
                    "No hay stock suficiente de '%s': disponible %d, solicitado %d",
                    product.getName(), product.getStockAvailable(), quantity);
        }

        applyDelta(product, -quantity, StockMovementType.RESERVA, rentalId,
                "Reserva para alquiler " + (rentalId == null ? "en curso" : rentalId));

        return reservationRepository.save(StockReservation.builder()
                .productId(productId)
                .rentalId(rentalId)
                .sessionKey(sessionKey)
                .quantity(quantity)
                .expiresAt(LocalDateTime.now().plusMinutes(properties.getRental().getReservationMinutes()))
                .build());
    }

    /** Deshace una reserva y devuelve las unidades al patio. */
    @Transactional
    public void release(StockReservation reservation, String reason) {
        if (!reservation.isActive()) {
            return;
        }
        Product product = lockProduct(reservation.getProductId());
        applyDelta(product, reservation.getQuantity(), StockMovementType.LIBERACION,
                reservation.getRentalId(), reason);

        reservation.setReleased(true);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void releaseAllForRental(Long rentalId, String reason) {
        reservationRepository.findActiveByRental(rentalId).forEach(r -> release(r, reason));
    }

    /**
     * Confirma la salida fisica del material. El disponible no cambia porque ya
     * se descontó al reservar; solo se cierra la reserva y se deja el asiento.
     */
    @Transactional
    public void dispatch(Long rentalId) {
        List<StockReservation> reservations = reservationRepository.findActiveByRental(rentalId);
        if (reservations.isEmpty()) {
            return;
        }
        for (StockReservation reservation : reservations) {
            Product product = lockProduct(reservation.getProductId());
            ledgerRepository.save(StockLedgerEntry.builder()
                    .productId(product.getId())
                    .rentalId(rentalId)
                    .movementType(StockMovementType.DESPACHO)
                    .quantity(0)
                    .balanceAfter(product.getStockAvailable())
                    .note("Material entregado en obra")
                    .userId(currentUser.idOrNull())
                    .build());

            reservation.setConsumed(true);
            reservationRepository.save(reservation);
        }
    }

    /** El cliente devolvio unidades en buen estado. */
    @Transactional
    public void restock(Long productId, int quantity, Long rentalId, String note) {
        if (quantity <= 0) {
            return;
        }
        Product product = lockProduct(productId);
        applyDelta(product, quantity, StockMovementType.RETORNO, rentalId, note);
    }

    /**
     * Unidades que no volvieron o volvieron inservibles: salen del inventario
     * total, no solo del disponible.
     */
    @Transactional
    public void writeOff(Long productId, int quantity, Long rentalId, String note) {
        if (quantity <= 0) {
            return;
        }
        Product product = lockProduct(productId);
        product.setStockTotal(Math.max(0, product.getStockTotal() - quantity));

        ledgerRepository.save(StockLedgerEntry.builder()
                .productId(productId)
                .rentalId(rentalId)
                .movementType(StockMovementType.BAJA_POR_DANIO)
                .quantity(-quantity)
                .balanceAfter(product.getStockAvailable())
                .note(note)
                .userId(currentUser.idOrNull())
                .build());

        productRepository.save(product);
    }

    /** Correccion manual del inventario desde la pantalla de patio. */
    @Transactional
    public Product adjust(Long productId, int newTotal, String reason) {
        if (newTotal < 0) {
            throw new BusinessException("El stock total no puede ser negativo");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Todo ajuste de inventario necesita un motivo");
        }
        Product product = lockProduct(productId);

        int inUse = product.inUse();
        if (newTotal < inUse) {
            throw BusinessException.of(
                    "No puedes dejar el total en %d: hay %d unidad(es) en la calle o apartadas",
                    newTotal, inUse);
        }

        int delta = newTotal - product.getStockTotal();
        product.setStockTotal(newTotal);
        product.setStockAvailable(newTotal - inUse);

        ledgerRepository.save(StockLedgerEntry.builder()
                .productId(productId)
                .movementType(StockMovementType.AJUSTE)
                .quantity(delta)
                .balanceAfter(product.getStockAvailable())
                .note(reason)
                .userId(currentUser.idOrNull())
                .build());

        return productRepository.save(product);
    }

    /** Entrada de inventario proveniente de una importacion masiva. */
    @Transactional
    public void registerImport(Product product, int quantity, String note) {
        ledgerRepository.save(StockLedgerEntry.builder()
                .productId(product.getId())
                .movementType(StockMovementType.IMPORTACION)
                .quantity(quantity)
                .balanceAfter(product.getStockAvailable())
                .note(note)
                .userId(currentUser.idOrNull())
                .build());
    }

    /**
     * Job periodico: devuelve al patio el stock de reservas vencidas.
     *
     * @return identificadores de alquiler afectados, para que el modulo de
     *         alquileres pueda cancelar los borradores correspondientes
     */
    @Transactional
    public List<Long> expireStaleReservations() {
        List<StockReservation> expired = reservationRepository.findExpired(LocalDateTime.now());
        if (expired.isEmpty()) {
            return List.of();
        }
        expired.forEach(reservation -> release(reservation, "Reserva vencida sin confirmar"));
        log.info("Se liberaron {} reserva(s) de stock vencidas", expired.size());

        return expired.stream().map(StockReservation::getRentalId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private Product lockProduct(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", productId));
    }

    private void applyDelta(Product product, int delta, StockMovementType type, Long rentalId, String note) {
        int newAvailable = product.getStockAvailable() + delta;
        if (newAvailable < 0) {
            throw BusinessException.of("El stock de '%s' quedaria negativo", product.getName());
        }
        // Retornos y liberaciones nunca deben superar el total del inventario.
        if (newAvailable > product.getStockTotal()) {
            newAvailable = product.getStockTotal();
        }
        product.setStockAvailable(newAvailable);
        productRepository.save(product);

        ledgerRepository.save(StockLedgerEntry.builder()
                .productId(product.getId())
                .rentalId(rentalId)
                .movementType(type)
                .quantity(delta)
                .balanceAfter(newAvailable)
                .note(note)
                .userId(currentUser.idOrNull())
                .build());
    }
}
