package co.edu.uniminuto.hyeda.modules.finanzas.controller;

import co.edu.uniminuto.hyeda.modules.finanzas.dto.DebtDto;
import co.edu.uniminuto.hyeda.modules.finanzas.dto.FinancialMovementDto;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.MovementType;
import co.edu.uniminuto.hyeda.modules.finanzas.service.FinanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finanzas")
@PreAuthorize("hasAnyAuthority('FINANZAS','ADMIN')")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/movimientos")
    public List<FinancialMovementDto> movements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return financeService.movements(from, to);
    }

    @PostMapping("/movimientos")
    public FinancialMovementDto create(@RequestBody Map<String, Object> body) {
        return financeService.createMovement(
                MovementType.valueOf(body.get("movementType").toString()),
                new BigDecimal(body.get("amount").toString()),
                (String) body.get("description"),
                (String) body.get("category"),
                body.get("movementDate") == null ? null : LocalDate.parse(body.get("movementDate").toString())
        );
    }

    @GetMapping("/deudas")
    public List<DebtDto> debts() {
        return financeService.debts();
    }
}
