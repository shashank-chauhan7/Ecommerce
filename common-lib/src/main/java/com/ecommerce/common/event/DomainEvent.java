package com.ecommerce.common.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent<T>(
        String eventId,
        String eventType,
        String aggregateId,
        Instant timestamp,
        T payload,
        Map<String, String> metadata
) implements Serializable {

    public static <T> DomainEvent<T> create(String eventType, String aggregateId, T payload) {
        return new DomainEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                aggregateId,
                Instant.now(),
                payload,
                Map.of()
        );
    }

    public static <T> DomainEvent<T> create(String eventType, String aggregateId, T payload,
                                             Map<String, String> metadata) {
        return new DomainEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                aggregateId,
                Instant.now(),
                payload,
                metadata
        );
    }
}
