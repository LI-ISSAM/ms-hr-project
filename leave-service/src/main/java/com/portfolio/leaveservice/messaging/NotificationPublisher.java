package com.portfolio.leaveservice.messaging;

import com.portfolio.leaveservice.config.RabbitConfig;
import com.portfolio.leaveservice.dto.NotificationEmailDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void envoyer(NotificationEmailDto notification) {
        rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_EMAIL, notification);
    }
}