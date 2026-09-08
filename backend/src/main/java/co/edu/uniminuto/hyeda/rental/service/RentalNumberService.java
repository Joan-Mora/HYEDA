package co.edu.uniminuto.hyeda.rental.service;

import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Consecutivo de alquileres, reiniciado cada ano: ALQ-2026-00042.
 */
@Service
@RequiredArgsConstructor
public class RentalNumberService {

    private final RentalRepository rentalRepository;

    @Transactional
    public String nextCode() {
        int year = LocalDate.now().getYear();
        LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(year + 1, 1, 1).atStartOfDay();

        long sequence = rentalRepository.countCreatedBetween(from, to) + 1;
        String candidate = format(year, sequence);

        // Si un alquiler se cancelo y se borro, el conteo puede chocar; se avanza.
        while (rentalRepository.findByCode(candidate).isPresent()) {
            candidate = format(year, ++sequence);
        }
        return candidate;
    }

    private String format(int year, long sequence) {
        return "ALQ-%d-%05d".formatted(year, sequence);
    }

    /** Marca de tiempo del cierre del dia, util para reportes. */
    public LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
