package com.portfolio.leaveservice.service;

import com.portfolio.leaveservice.client.EmployeeDto;
import com.portfolio.leaveservice.client.ResilientEmployeeClient;
import com.portfolio.leaveservice.dto.CreationResult;
import com.portfolio.leaveservice.dto.NotificationEmailDto;
import com.portfolio.leaveservice.entity.LeaveRequest;
import com.portfolio.leaveservice.entity.LeaveStatus;
import com.portfolio.leaveservice.entity.LeaveType;
import com.portfolio.leaveservice.messaging.NotificationPublisher;
import com.portfolio.leaveservice.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRequestRepository repo;
    private final ResilientEmployeeClient employeeClient;
    private final NotificationPublisher notificationPublisher;
    private final TransactionTemplate tx;

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

    public CreationResult<LeaveRequest> create(LeaveRequest req, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }

        // 1. Rejeu : même clé, on renvoie la demande déjà créée (aucun email, aucun appel distant)
        Optional<LeaveRequest> existing = repo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new CreationResult<>(existing.get(), false);
        }

        // 2. Validations locales
        if (req.getEmployeeId() == null || req.getStartDate() == null
                || req.getEndDate() == null || req.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "employeeId, startDate, endDate and type are required");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be >= startDate");
        }

        // 3. Appel distant protégé (retry + circuit breaker), HORS transaction
        EmployeeDto employee = employeeClient.findById(req.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee not found: " + req.getEmployeeId()));

        // 4. Transaction courte : enregistrement, puis publication après commit
        LeaveRequest saved = tx.execute(status -> {
            req.setId(null);
            req.setStatus(LeaveStatus.PENDING);
            req.setIdempotencyKey(idempotencyKey);
            LeaveRequest s = repo.save(req);
            publierNotification(creationEmail(s, employee));
            return s;
        });
        return new CreationResult<>(saved, true);
    }

    public LeaveRequest changeStatus(Long id, LeaveStatus newStatus) {
        LeaveRequest current = findById(id);
        if (current.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already processed");
        }

        // Meilleur effort, hors transaction : si Employee est indisponible, la décision est quand même prise
        Optional<EmployeeDto> employee = Optional.empty();
        try {
            employee = employeeClient.findById(current.getEmployeeId());
        } catch (RuntimeException e) {
            log.warn("Notification skipped for leave {}: {}", id, e.getMessage());
        }
        final Optional<EmployeeDto> recipient = employee;

        return tx.execute(status -> {
            LeaveRequest req = findById(id);
            if (req.getStatus() != LeaveStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already processed");
            }
            req.setStatus(newStatus);
            LeaveRequest saved = repo.save(req);

            String decision = (newStatus == LeaveStatus.APPROVED) ? "approuvée" : "refusée";
            recipient.ifPresent(e -> publierNotification(decisionEmail(saved, e, decision)));
            return saved;
        });
    }

    private NotificationEmailDto creationEmail(LeaveRequest l, EmployeeDto e) {
        return new NotificationEmailDto(
                e.email(),
                "Demande de congé n° " + l.getId() + " enregistrée",
                "Bonjour " + e.firstName() + ", votre demande de congé n° " + l.getId()
                        + " (" + l.getType() + ") du " + l.getStartDate() + " au " + l.getEndDate()
                        + " a été enregistrée. Statut : " + l.getStatus() + ".");
    }

    private NotificationEmailDto decisionEmail(LeaveRequest l, EmployeeDto e, String decision) {
        return new NotificationEmailDto(
                e.email(),
                "Demande de congé n° " + l.getId() + " " + decision,
                "Bonjour " + e.firstName() + ", votre demande de congé n° " + l.getId()
                        + " a été " + decision + ".");
    }

    // Publication RabbitMQ seulement si la transaction est validée
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

}