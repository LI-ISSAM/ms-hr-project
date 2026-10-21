package com.portfolio.employeeservice.repository;

import com.portfolio.employeeservice.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}