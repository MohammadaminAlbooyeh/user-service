package com.user.event;

import com.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    public static final String USER_EVENTS_TOPIC = "user-events";

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public void publishUserRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(user.getId(), user.getUsername(), user.getEmail(), Instant.now());
        try {
            var future = kafkaTemplate.send(USER_EVENTS_TOPIC, String.valueOf(user.getId()), event);
            if (future != null) {
                future.exceptionally(ex -> {
                    log.warn("Failed to publish user.registered event for user {}", user.getId(), ex);
                    return null;
                });
            }
        } catch (Exception ex) {
            log.warn("Failed to publish user.registered event for user {}", user.getId(), ex);
        }
    }
}
