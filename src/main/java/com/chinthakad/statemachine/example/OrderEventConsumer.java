package com.chinthakad.statemachine.example;

import com.chinthakad.statemachine.framework.StateMachine;
import com.chinthakad.statemachine.framework.StateMachineRepository;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final StateMachineRepository<OrderState, OrderEvent, OrderEventMessage> repository = new StateMachineRepository<>();

    @Incoming("order-events")
    @Blocking
    public void consume(OrderEventMessage message) {
        StateMachine<OrderState, OrderEvent> sm = repository.get(message.orderId);
        if (sm == null) {
            log.error("[Kafka] Order not found: {}", message.orderId);
            return;
        }
        try {
            OrderEvent event = OrderEvent.valueOf(message.event.toUpperCase());
            boolean success = sm.trigger(event);
            if (!success) {
                log.error("[Kafka] Invalid transition for order {}: {}. Caching event.", message.orderId, message.event);
                repository.addPendingEvent(message.orderId, message);
            }
            sm.processPendingEvents(
                message.orderId,
                repository.getPendingEvents(message.orderId),
                (OrderEventMessage m) -> {
                    try {
                        return OrderEvent.valueOf(m.event.toUpperCase());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    repository.clearPendingEvents(message.orderId);
                    repository.remove(message.orderId);
                }
            );
        } catch (IllegalArgumentException e) {
            log.error("[Kafka] Invalid event: {}", message.event);
        }
    }

    // Expose repository for other classes (e.g., REST)
    public static StateMachineRepository<OrderState, OrderEvent, OrderEventMessage> getRepository() {
        return repository;
    }
} 