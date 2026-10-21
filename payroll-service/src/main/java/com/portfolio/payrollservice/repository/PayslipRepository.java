package com.portfolio.payrollservice.repository;

import com.portfolio.payrollservice.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByEmployeeId(Long employeeId);
    boolean existsByEmployeeIdAndMonthAndYear(Long employeeId, int month, int year);
}