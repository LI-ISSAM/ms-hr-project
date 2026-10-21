package com.portfolio.employeeservice.controller;

import com.portfolio.employeeservice.entity.Employee;
import com.portfolio.employeeservice.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@RequestBody Employee e) { return service.create(e); }

    @PutMapping("/{id}")
    public Employee update(@PathVariable Long id, @RequestBody Employee e) { return service.update(id, e); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}