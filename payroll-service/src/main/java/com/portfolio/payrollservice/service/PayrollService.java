package com.portfolio.payrollservice.service;

import com.portfolio.payrollservice.client.EmployeeDto;
import com.portfolio.payrollservice.client.ResilientEmployeeClient;
import com.portfolio.payrollservice.client.ResilientLeaveClient;
import com.portfolio.payrollservice.dto.CreationResult;
import com.portfolio.payrollservice.entity.Payslip;
import com.portfolio.payrollservice.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final BigDecimal DEDUCTION_RATE = new BigDecimal("0.10");

    private final PayslipRepository repo;
    private final ResilientEmployeeClient employeeClient;
    private final ResilientLeaveClient leaveClient;
    private final TransactionTemplate tx;

    public List<Payslip> findAll() {
        return repo.findAll();
    }

    public Payslip findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payslip not found: " + id));
    }

    public List<Payslip> findByEmployee(Long employeeId) {
        return repo.findByEmployeeId(employeeId);
    }

    public CreationResult<Payslip> generate(Long employeeId, int month, int year,
                                            BigDecimal bonus, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }

        // 1. Rejeu : même clé, on renvoie la fiche déjà générée
        Optional<Payslip> existing = repo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new CreationResult<>(existing.get(), false);
        }

        // 2. Validations locales
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be 1..12");
        }
        if (repo.existsByEmployeeIdAndMonthAndYear(employeeId, month, year)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payslip already generated");
        }

        // 3. Deux appels distants protégés, HORS transaction
        EmployeeDto emp = employeeClient.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee not found: " + employeeId));
        long unpaidDays = leaveClient.getUnpaidDays(employeeId, month, year);

        // 4. Calcul
        BigDecimal base = emp.baseSalary();
        BigDecimal b = (bonus == null) ? BigDecimal.ZERO : bonus;
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        BigDecimal unpaidDeduction = base
                .multiply(BigDecimal.valueOf(unpaidDays))
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        BigDecimal gross = base.subtract(unpaidDeduction).add(b);
        BigDecimal deductions = gross.multiply(DEDUCTION_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(deductions);

        Payslip p = new Payslip();
        p.setEmployeeId(employeeId);
        p.setMonth(month);
        p.setYear(year);
        p.setBaseSalary(base);
        p.setBonus(b);
        p.setUnpaidLeaveDays(unpaidDays);
        p.setUnpaidLeaveDeducation(unpaidDeduction);
        p.setDeductions(deductions);
        p.setNetSalary(net);
        p.setGeneratedAt(LocalDateTime.now());
        p.setIdempotencyKey(idempotencyKey);

        // 5. Transaction courte
        Payslip saved = tx.execute(status -> repo.save(p));
        return new CreationResult<>(saved, true);
    }
}