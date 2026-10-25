package com.portfolio.payrollservice.service;

import com.portfolio.payrollservice.client.EmployeeClient;
import com.portfolio.payrollservice.client.EmployeeDto;
import com.portfolio.payrollservice.client.LeaveClient;
import com.portfolio.payrollservice.entity.Payslip;
import com.portfolio.payrollservice.repository.PayslipRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final BigDecimal DEDUCTION_RATE = new BigDecimal("0.10");
    private final EmployeeClient employeeClient;

    private final PayslipRepository repo;
    private final LeaveClient leaveClient;

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

    public Payslip generate(Long employeeId, int month, int year, BigDecimal bonus) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be 1..12");
        }
        if (repo.existsByEmployeeIdAndMonthAndYear(employeeId, month, year)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payslip already generated");
        }

        EmployeeDto emp = fetchEmployee(employeeId); //appel Feign de employee
        long unpaidDays = fetchUnpaidDays(employeeId, month, year); // appel Feign de Leave

        BigDecimal base = emp.baseSalary();
        BigDecimal b = (bonus == null) ? BigDecimal.ZERO : bonus;
        int daysInMonth = YearMonth.of(year,month).lengthOfMonth();
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
        return repo.save(p);
    }

    private EmployeeDto fetchEmployee(Long employeeId) {
        try {
            return employeeClient.getEmployee(employeeId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + employeeId);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Employee service unavailable");
        }
    }

    private long fetchUnpaidDays(Long employeeId, int month, int year){
        try{
            return leaveClient.getUnpaidDays(employeeId,month,year).unpaidDays();
        }catch(FeignException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Leave service unvailable");
        }
    }
}