package com.portfolio.employeeservice.controller;

import com.portfolio.employeeservice.dto.CreationResult;
import com.portfolio.employeeservice.entity.Employee;
import com.portfolio.employeeservice.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService service;

    @GetMapping public List<Employee> all() { return service.findAll(); }
    @GetMapping("/{id}")
    public Employee one(@PathVariable Long id) { return service.findById(id); }

    @GetMapping("/department/{departmentId}")
    public List<Employee> byDepartment(@PathVariable Long departmentId) {
        return service.findByDepartment(departmentId);
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                           @RequestBody Employee e) {
        CreationResult<Employee> result = service.create(e, idempotencyKey);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .header("Idempotent-Replayed", String.valueOf(!result.created()))
                .body(result.value());
    }
    @PutMapping("/{id}")
    public Employee update(@PathVariable Long id, @RequestBody Employee e) { return service.update(id, e); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}