package com.portfolio.leaveservice.controller;


import com.portfolio.leaveservice.client.UnpaidDaysDto;
import com.portfolio.leaveservice.entity.LeaveRequest;
import com.portfolio.leaveservice.entity.LeaveStatus;
import com.portfolio.leaveservice.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService service;

    @GetMapping
    public List<LeaveRequest> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public LeaveRequest one(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/employee/{employeeId}")
    public List<LeaveRequest> byEmployee(@PathVariable Long employeeId) {
        return service.findByEmployee(employeeId);
    }

    @GetMapping("/status/{status}")
    public List<LeaveRequest> byStatus(@PathVariable LeaveStatus status) {
        return service.findByStatus(status);
    }

    @GetMapping("/employee/{employeeId}/unpaid-days")
    public UnpaidDaysDto unpaidDays(@PathVariable Long employeeId,
                                    @RequestParam int month,
                                    @RequestParam int year
                                    ){
        return new UnpaidDaysDto(employeeId,month,year,
                service.countUnpaidDays(employeeId,month,year)
                );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequest create(@RequestBody LeaveRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}/approve")
    public LeaveRequest approve(@PathVariable Long id) {
        return service.changeStatus(id, LeaveStatus.APPROVED);
    }

    @PutMapping("/{id}/reject")
    public LeaveRequest reject(@PathVariable Long id) {
        return service.changeStatus(id, LeaveStatus.REJECTED);
    }
}