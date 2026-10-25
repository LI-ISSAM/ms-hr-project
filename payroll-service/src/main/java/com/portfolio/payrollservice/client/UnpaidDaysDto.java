package com.portfolio.payrollservice.client;

public record UnpaidDaysDto(Long employeeId, int month , int year,long unpaidDays) {
}
