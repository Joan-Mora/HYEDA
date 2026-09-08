package co.edu.uniminuto.hyeda.rental.domain;

/**
 * El material salio hacia la obra.
 *
 * <p>Se publica como evento para que el modulo de alquiler no tenga que conocer
 * al de notificaciones: quien quiera reaccionar (mandar el resumen por WhatsApp,
 * por ejemplo) se suscribe.
 */
public record RentalDispatchedEvent(Long rentalId, String code) {
}
