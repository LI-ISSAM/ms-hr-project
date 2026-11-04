package com.portfolio.leaveservice.dto;

public record CreationResult<T>(T value, boolean created) {
}