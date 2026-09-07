package co.edu.uniminuto.hyeda.modules.finanzas.service;

import co.edu.uniminuto.hyeda.modules.clientes.service.CustomerService;
import co.edu.uniminuto.hyeda.modules.finanzas.dto.DebtDto;
import co.edu.uniminuto.hyeda.modules.finanzas.dto.FinancialMovementDto;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.Debt;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.DebtStatus;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.FinancialMovement;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.MovementType;
import co.edu.uniminuto.hyeda.modules.finanzas.repository.DebtRepository;
import co.edu.uniminuto.hyeda.modules.finanzas.repository.FinancialMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FinanceService {

    private final FinancialMovementRepository movementRepository;
    private final DebtRepository debtRepository;
    private final CustomerService customerService;

    public FinanceService(FinancialMovementRepository movementRepository,
                          DebtRepository debtRepository,
                          CustomerService customerService) {
        this.movementRepository = movementRepository;
        this.debtRepository = debtRepository;
        this.customerService = customerService;
    }

    public List<FinancialMovementDto> movements(LocalDate from, LocalDate to) {
        List<FinancialMovement> list = (from != null && to != null)
                ? movementRepository.findByMovementDateBetween(from, to)
                : movementRepository.findAll();
        return list.stream().map(this::toMov).toList();
    }

    @Transactional
    public FinancialMovementDto createMovement(MovementType type, BigDecimal amount, String description, String category, LocalDate date) {
        FinancialMovement m = FinancialMovement.builder()
                .movementType(type)
                .amount(amount)
                .description(description)
                .category(category)
                .movementDate(date == null ? LocalDate.now() : date)
                .build();
        return toMov(movementRepository.save(m));
    }

    public List<DebtDto> debts() {
        return debtRepository.findAll().stream().map(this::toDebt).toList();
    }

    @Transactional
    public void refreshOverdue() {
        LocalDate today = LocalDate.now();
        for (Debt debt : debtRepository.findAll()) {
            if (debt.getStatus() == DebtStatus.PAGADA) continue;
            if (debt.getDueDate() != null && debt.getDueDate().isBefore(today)) {
                debt.setOverdueDays((int) ChronoUnit.DAYS.between(debt.getDueDate(), today));
                debt.setStatus(DebtStatus.VENCIDA);
                if (debt.getOverdueDays() > 15) {
                    customerService.adjustReputation(debt.getCustomer().getId(), BigDecimal.valueOf(-3));
                }
                debtRepository.save(debt);
            }
        }
    }

    private FinancialMovementDto toMov(FinancialMovement m) {
        return new FinancialMovementDto(m.getId(), m.getMovementType(), m.getAmount(),
                m.getDescription(), m.getCategory(), m.getMovementDate());
    }

    private DebtDto toDebt(Debt d) {
        return new DebtDto(d.getId(), d.getCustomer().getId(), d.getCustomer().getName(),
                d.getRental() == null ? null : d.getRental().getId(),
                d.getBalance(), d.getDueDate(), d.getOverdueDays(), d.getStatus());
    }
}
