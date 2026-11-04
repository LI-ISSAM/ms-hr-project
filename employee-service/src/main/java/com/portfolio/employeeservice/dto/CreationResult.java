package com.portfolio.employeeservice.dto;

public record CreationResult<T>(T value, boolean created) {
}