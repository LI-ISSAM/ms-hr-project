package com.portfolio.notificationservice.messaging;

import com.portfolio.notificationservice.config.RabbitConfig;
import com.portfolio.notificationservice.dto.NotificationEmailDto;
import com.portfolio.notificationservice.service.NotificationService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;
    private final Validator validator;

    public NotificationConsumer(NotificationService notificationService, Validator validator) {
        this.notificationService = notificationService;
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_EMAIL)
    public void recevoir(NotificationEmailDto notification) {
        log.info("Message reçu pour {} : {}", notification.destinataire(), notification.objet());

        var erreurs = validator.validate(notification);
        if (!erreurs.isEmpty()) {
            throw new ConstraintViolationException(erreurs);
        }
        notificationService.envoyerEmail(notification);
        log.info("Email envoyé à {}", notification.destinataire());
    }
}