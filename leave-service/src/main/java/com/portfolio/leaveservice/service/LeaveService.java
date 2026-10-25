package com.portfolio.leaveservice.service;

import com.portfolio.leaveservice.client.EmployeeClient;
import com.portfolio.leaveservice.entity.LeaveRequest;
import com.portfolio.leaveservice.entity.LeaveStatus;
import com.portfolio.leaveservice.entity.LeaveType;
import com.portfolio.leaveservice.repository.LeaveRequestRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository repo;
    private final EmployeeClient employeeClient;

    public List<LeaveRequest> findAll() {
        return repo.findAll();
    }

    public LeaveRequest findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found: " + id));
    }

    public List<LeaveRequest> findByEmployee(Long employeeId) {
        return repo.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> findByStatus(LeaveStatus status) {
        return repo.findByStatus(status);
    }

    public LeaveRequest create(LeaveRequest req) {
        checkEmployeeExists(req.getEmployeeId());

        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be >= startDate");
        }
        req.setId(null);
        req.setStatus(LeaveStatus.PENDING);
        return repo.save(req);
    }

    public LeaveRequest changeStatus(Long id, LeaveStatus newStatus) {
        LeaveRequest req = findById(id);
        if (req.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already processed");
        }
        req.setStatus(newStatus);
        return repo.save(req);
    }

    public long countUnpaidDays(Long employeeId , int month, int year){
        if(month < 1 || month > 12){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"month must be 1...12");
        }

        YearMonth ym = YearMonth.of(year,month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        return repo.findByEmployeeIdAndTypeAndStatus(employeeId, LeaveType.UNPAID, LeaveStatus.APPROVED)
                .stream()
                .mapToLong(l -> {
                    LocalDate from = l.getStartDate().isAfter(monthStart) ? l.getStartDate() : monthStart;
                    LocalDate to = l.getEndDate().isBefore(monthEnd) ? l.getEndDate() : monthEnd;
                    return from.isAfter(to) ? 0 : ChronoUnit.DAYS.between(from, to) + 1;
                })
                .sum();
    }

    private void checkEmployeeExists(Long employeeId) {
        try {
            employeeClient.getEmployee(employeeId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Employee not found. Feign: " + e.getMessage());
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Employee service unavailable. Feign: " + e.getMessage());
        }
    }
}