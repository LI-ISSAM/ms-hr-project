package com.portfolio.payrollservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="leave-service")
public interface LeaveClient {
    @GetMapping("/api/leaves/employee/{employeeId}/unpaid-days")
    UnpaidDaysDto getUnpaidDays(@PathVariable("employeeId") Long employeeId,
                                @RequestParam("month") int month,
                                @RequestParam("year") int year

    );
}
