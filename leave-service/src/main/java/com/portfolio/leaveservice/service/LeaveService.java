package com.portfolio.leaveservice.service;

import com.portfolio.leaveservice.client.EmployeeClient;
import com.portfolio.leaveservice.client.EmployeeDto;
import com.portfolio.leaveservice.dto.NotificationEmailDto;
import com.portfolio.leaveservice.entity.LeaveRequest;
import com.portfolio.leaveservice.entity.LeaveStatus;
import com.portfolio.leaveservice.entity.LeaveType;
import com.portfolio.leaveservice.messaging.NotificationPublisher;
import com.portfolio.leaveservice.repository.LeaveRequestRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRequestRepository repo;
    private final EmployeeClient employeeClient;
    private final NotificationPublisher notificationPublisher;

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

    @Transactional
    public LeaveRequest create(LeaveRequest req) {
        EmployeeDto employee = fetchEmployee(req.getEmployeeId());
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be >= startDate");
        }
        req.setId(null);
        req.setStatus(LeaveStatus.PENDING);
        LeaveRequest saved = repo.save(req);

        publierNotification(new NotificationEmailDto(
                employee.email(),
                "Demande de congé n° " + saved.getId() + " enregistrée",
                "Bonjour " + employee.firstName() + ", votre demande de congé n° " + saved.getId()
                        + " (" + saved.getType() + ") du " + saved.getStartDate()
                        + " au " + saved.getEndDate() + " a été enregistrée. Statut : "
                        + saved.getStatus() + "."));
        return saved;

    }

    public LeaveRequest update(Long id, LeaveRequest r){
        LeaveRequest exist = repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found: " + id));
        exist.setStartDate(r.getStartDate());
        exist.setEndDate(r.getEndDate());
        exist.setType(r.getType());
        exist.setStatus(r.getStatus());
        exist.setReason(r.getReason());

        return repo.save(exist);

    }

    @Transactional
    public LeaveRequest changeStatus(Long id, LeaveStatus newStatus) {
        LeaveRequest req = findById(id);
        if (req.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already processed");
        }
        req.setStatus(newStatus);
        LeaveRequest saved = repo.save(req);

        try {
            EmployeeDto employee = employeeClient.getEmployee(saved.getEmployeeId());
            String decision = (newStatus == LeaveStatus.APPROVED) ? "approuvée" : "refusée";
            publierNotification(new NotificationEmailDto(
                    employee.email(),
                    "Demande de congé n° " + saved.getId() + " " + decision,
                    "Bonjour " + employee.firstName() + ", votre demande de congé n° "
                            + saved.getId() + " a été " + decision + "."));
        } catch (RuntimeException e) {
            log.warn("Notification ignorée pour la demande {} : {}", saved.getId(), e.getMessage());
        }
        return saved;
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



    private EmployeeDto fetchEmployee(Long employeeId){
        try{
            return employeeClient.getEmployee(employeeId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + employeeId);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Employee service unavailable");
        }
    }
    private void publierNotification(NotificationEmailDto email) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notificationPublisher.envoyer(email);
                } catch (AmqpException ex) {
                    log.error("Demande enregistrée, mais publication de la notification en échec", ex);
                }
            }
        });
    }
}