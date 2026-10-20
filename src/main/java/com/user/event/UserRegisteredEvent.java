package com.user.event;

import java.time.Instant;

public record UserRegisteredEvent(Long userId, String username, String email, Instant occurredAt) {
}
