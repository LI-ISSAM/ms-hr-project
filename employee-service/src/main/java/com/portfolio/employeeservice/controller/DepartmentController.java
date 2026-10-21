package com.portfolio.employeeservice.controller;


import com.portfolio.employeeservice.entity.Department;
import com.portfolio.employeeservice.services.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService service;

    @GetMapping
    public List<Department> all() { return service.findAll(); }
    @GetMapping("/{id}") public Department one(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Department create(@RequestBody Department d) { return service.create(d); }

    @PutMapping("/{id}")
    public Department update(@PathVariable Long id, @RequestBody Department d) { return service.update(id, d); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}