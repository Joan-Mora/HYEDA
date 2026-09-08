package co.edu.uniminuto.hyeda.rental.web;

import co.edu.uniminuto.hyeda.rental.domain.RentalStatus;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.ChangeStatusRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.CreateRentalRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.ExtensionRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.QuoteRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.QuoteView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalEligibility;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalRow;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.RentalView;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.ReturnRequest;
import co.edu.uniminuto.hyeda.rental.dto.RentalDtos.UpdateRentalRequest;
import co.edu.uniminuto.hyeda.rental.service.RentalService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/alquileres")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService service;

    @GetMapping
    @PreAuthorize("hasAuthority('ALQUILERES_VER')")
    public PageResponse<RentalRow> search(
            @RequestParam(required = false) RentalStatus estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(estado, clienteId, desde, hasta, pageable);
    }

    @GetMapping("/en-curso")
    @PreAuthorize("hasAuthority('ALQUILERES_VER')")
    public List<RentalRow> active() {
        return service.active();
    }

    @GetMapping("/vencidos")
    @PreAuthorize("hasAuthority('ALQUILERES_VER')")
    public List<RentalRow> overdue() {
        return service.overdue();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ALQUILERES_VER')")
    public RentalView findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("hasAuthority('ALQUILERES_VER')")
    public RentalView findByCode(@PathVariable String codigo) {
        return service.findByCode(codigo);
    }

    @GetMapping("/elegibilidad/{clienteId}")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalEligibility eligibility(@PathVariable Long clienteId) {
        return service.eligibility(clienteId);
    }

    @PostMapping("/cotizar")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public QuoteView quote(@Valid @RequestBody QuoteRequest request) {
        return service.quote(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalView create(@Valid @RequestBody CreateRentalRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalView update(@PathVariable Long id, @Valid @RequestBody UpdateRentalRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/entregar")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalView dispatch(@PathVariable Long id) {
        return service.dispatch(id);
    }

    @PostMapping("/{id}/extender")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalView extend(@PathVariable Long id, @Valid @RequestBody ExtensionRequest request) {
        return service.extend(id, request);
    }

    @PostMapping("/{id}/devolucion")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalView registerReturn(@PathVariable Long id, @Valid @RequestBody ReturnRequest request) {
        return service.registerReturn(id, request);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalView cancel(@PathVariable Long id, @RequestParam(required = false) String motivo) {
        return service.cancel(id, motivo);
    }

    @PostMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalView changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(id, request);
    }

    @PostMapping("/{id}/firma")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public RentalView attachSignature(@PathVariable Long id, @RequestPart("file") MultipartFile file)
            throws IOException {
        return service.attachSignature(id, file.getBytes());
    }
}
