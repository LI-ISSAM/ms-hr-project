package com.portfolio.notificationservice.controller;


import com.portfolio.notificationservice.dto.NotificationEmailDto;
import com.portfolio.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private NotificationService notificationService;
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    @PostMapping("/email")
    public ResponseEntity<Void> envoyer(@Valid @RequestBody NotificationEmailDto notification){
        notificationService.envoyerEmail(notification);
        return ResponseEntity.noContent().build();
    }
}
