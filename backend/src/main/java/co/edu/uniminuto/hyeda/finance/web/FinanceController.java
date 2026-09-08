package co.edu.uniminuto.hyeda.finance.web;

import co.edu.uniminuto.hyeda.finance.domain.MovementType;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.CashSummary;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.CreditView;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.DebtPortfolio;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.DebtRow;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.MovementRequest;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.MovementView;
import co.edu.uniminuto.hyeda.finance.service.DebtService;
import co.edu.uniminuto.hyeda.finance.service.FinanceService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/finanzas")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final DebtService debtService;

    // ----------------------------------------------------------- movimientos

    @GetMapping("/movimientos")
    @PreAuthorize("hasAuthority('FINANZAS_VER')")
    public PageResponse<MovementView> movements(
            @RequestParam(required = false) MovementType tipo,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 25) Pageable pageable) {
        return financeService.search(tipo, categoria, desde, hasta, pageable);
    }

    @GetMapping("/categorias")
    @PreAuthorize("hasAuthority('FINANZAS_VER')")
    public List<String> categories() {
        return financeService.categories();
    }

    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FINANZAS_GESTIONAR')")
    public MovementView create(@Valid @RequestBody MovementRequest request) {
        return financeService.create(request);
    }

    @PutMapping("/movimientos/{id}")
    @PreAuthorize("hasAuthority('FINANZAS_GESTIONAR')")
    public MovementView update(@PathVariable Long id, @Valid @RequestBody MovementRequest request) {
        return financeService.update(id, request);
    }

    @DeleteMapping("/movimientos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FINANZAS_GESTIONAR')")
    public void delete(@PathVariable Long id) {
        financeService.delete(id);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('FINANZAS_VER')")
    public CashSummary summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return financeService.summary(desde, hasta);
    }

    // ---------------------------------------------------------------- cartera

    @GetMapping("/deudas")
    @PreAuthorize("hasAuthority('FINANZAS_VER')")
    public List<DebtRow> debts() {
        return debtService.openDebts();
    }

    @GetMapping("/deudas/cartera")
    @PreAuthorize("hasAuthority('FINANZAS_VER')")
    public DebtPortfolio portfolio() {
        return debtService.portfolio();
    }

    @GetMapping("/deudas/cliente/{customerId}")
    @PreAuthorize("hasAuthority('FINANZAS_VER')")
    public List<DebtRow> debtsOfCustomer(@PathVariable Long customerId) {
        return debtService.byCustomer(customerId);
    }

    @PostMapping("/deudas/{id}/condonar")
    @PreAuthorize("hasAuthority('FINANZAS_GESTIONAR')")
    public DebtRow forgive(@PathVariable Long id, @RequestParam String motivo) {
        return debtService.forgive(id, motivo);
    }

    @GetMapping("/saldo-a-favor/{customerId}")
    @PreAuthorize("hasAuthority('FINANZAS_VER')")
    public List<CreditView> credits(@PathVariable Long customerId) {
        return debtService.creditsOf(customerId);
    }
}
