package com.portfolio.employeeservice.services;

import com.portfolio.employeeservice.entity.Department;
import com.portfolio.employeeservice.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository repo;

    public List<Department> findAll() { return repo.findAll(); }

    public Department findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found: " + id));
    }

    public Department create(Department d) { d.setId(null); return repo.save(d); }

    public Department update(Long id, Department d) {
        Department existing = findById(id);
        existing.setName(d.getName());
        existing.setLocation(d.getLocation());
        return repo.save(existing);
    }

    public void delete(Long id) { repo.delete(findById(id)); }
}