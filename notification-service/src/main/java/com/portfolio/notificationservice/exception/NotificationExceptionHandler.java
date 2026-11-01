package com.portfolio.notificationservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(MailException.class)
    public ResponseEntity<String> erreurSmtp(MailException ex) {
        return ResponseEntity.status(503)
                .body("Impossible de transmettre l'email au serveur SMTP");
    }
}