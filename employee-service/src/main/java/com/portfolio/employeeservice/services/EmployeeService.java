package com.portfolio.employeeservice.services;


import com.portfolio.employeeservice.entity.Department;
import com.portfolio.employeeservice.entity.Employee;
import com.portfolio.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository repo;
    private final DepartmentService departmentService;

    public List<Employee> findAll() { return repo.findAll(); }

    public Employee findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + id));
    }

    public Employee create(Employee e) {
        if (repo.existsByEmail(e.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
        e.setId(null);
        attachDepartment(e);
        return repo.save(e);
    }

    public Employee update(Long id, Employee e) {
        Employee existing = findById(id);
        existing.setFirstName(e.getFirstName());
        existing.setLastName(e.getLastName());
        existing.setEmail(e.getEmail());
        existing.setPosition(e.getPosition());
        existing.setBaseSalary(e.getBaseSalary());
        existing.setHireDate(e.getHireDate());
        existing.setDepartment(e.getDepartment());
        attachDepartment(existing);
        return repo.save(existing);
    }

    public void delete(Long id) { repo.delete(findById(id)); }

    public List<Employee> findByDepartment(Long departmentId) {
        return repo.findByDepartmentId(departmentId);
    }

    private void attachDepartment(Employee e) {
        if (e.getDepartment() != null && e.getDepartment().getId() != null) {
            Department d = departmentService.findById(e.getDepartment().getId());
            e.setDepartment(d);
        }
    }
}