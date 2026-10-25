package com.portfolio.leaveservice.client;

public record UnpaidDaysDto(Long employeeId, int month , int year, long unpaidDays){}