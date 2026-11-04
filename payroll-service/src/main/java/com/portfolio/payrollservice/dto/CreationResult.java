package com.portfolio.payrollservice.dto;

public record CreationResult<T>(T value, boolean created) {
}