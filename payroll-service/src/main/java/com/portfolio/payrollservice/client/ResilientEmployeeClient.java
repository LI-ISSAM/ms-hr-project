package com.portfolio.payrollservice.client;

import com.portfolio.payrollservice.exception.RemoteServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
@RequiredArgsConstructor
@Slf4j

public class ResilientEmployeeClient {
    private final EmployeeClient employeeClient;
    @Retry(name = "employeeService", fallbackMethod = "findByIdFallback")
    @CircuitBreaker(name = "employeeService")
    @Bulkhead(name = "employeeService")

    public Optional<EmployeeDto> findById(Long id){
        try{
            return Optional.of(employeeClient.getEmployee(id));
        }catch(FeignException.NotFound e){
            return Optional.empty();
        }
    }

    private Optional<EmployeeDto> findByIdFallback(Long id, Throwable ex){
        log.warn("employee-service unavailable (employee{}): {}",id,ex.toString());
        throw new RemoteServiceUnavailableException("employee-service", ex);

    }
}
