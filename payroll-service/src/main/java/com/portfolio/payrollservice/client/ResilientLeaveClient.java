package com.portfolio.payrollservice.client;

import com.portfolio.payrollservice.exception.RemoteServiceUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientLeaveClient {

    private final LeaveClient leaveClient;

    @Retry(name = "leaveService", fallbackMethod = "getUnpaidDaysFallback")
    @CircuitBreaker(name = "leaveService")
    @Bulkhead(name = "leaveService")
    public long getUnpaidDays(Long employeeId, int month, int year) {
        return leaveClient.getUnpaidDays(employeeId, month, year).unpaidDays();
    }

    private long getUnpaidDaysFallback(Long employeeId, int month, int year, Throwable ex) {
        log.warn("leave-service unavailable (employee {}): {}", employeeId, ex.toString());
        // pas de valeur par défaut : une paie fausse est pire qu'un refus
        throw new RemoteServiceUnavailableException("leave-service", ex);
    }
}