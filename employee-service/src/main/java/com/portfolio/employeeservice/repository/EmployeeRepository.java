package com.portfolio.employeeservice.repository;

import com.portfolio.employeeservice.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmail(String email);
    List<Employee> findByDepartmentId(Long departmentId);
    Optional<Employee> findByIdempotencyKey(String idempotencyKey);
}