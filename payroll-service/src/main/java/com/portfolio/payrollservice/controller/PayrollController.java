package com.portfolio.payrollservice.controller;

import com.portfolio.payrollservice.entity.Payslip;
import com.portfolio.payrollservice.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payrolls")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService service;

    @GetMapping
    public List<Payslip> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Payslip one(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/employee/{employeeId}")
    public List<Payslip> byEmployee(@PathVariable Long employeeId) {
        return service.findByEmployee(employeeId);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public Payslip generate(@RequestParam Long employeeId,
                            @RequestParam int month,
                            @RequestParam int year,
                            @RequestParam(required = false) BigDecimal bonus) {
        return service.generate(employeeId, month, year, bonus);
    }
}