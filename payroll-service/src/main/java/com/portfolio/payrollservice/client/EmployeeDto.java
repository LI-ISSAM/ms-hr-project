package com.portfolio.payrollservice.client;

import java.math.BigDecimal;

public record EmployeeDto(Long id, String firstName, String lastName, BigDecimal baseSalary) {
}
