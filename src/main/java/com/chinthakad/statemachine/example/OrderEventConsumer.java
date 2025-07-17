package com.chinthakad.statemachine.example;

import com.chinthakad.statemachine.framework.StateMachine;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class OrderEventConsumer {
    @Incoming("order-events")
    @Blocking
    public void consume(OrderEventMessage message) {
        StateMachine<OrderState, OrderEvent> sm = StateMachineRepository.get(message.orderId);
        if (sm == null) {
            System.err.println("[Kafka] Order not found: " + message.orderId);
            return;
        }
        try {
            OrderEvent event = OrderEvent.valueOf(message.event.toUpperCase());
            boolean success = sm.trigger(event);
            if (!success) {
                System.err.println("[Kafka] Invalid transition for order " + message.orderId + ": " + message.event + ". Caching event.");
                StateMachineRepository.addPendingEvent(message.orderId, message);
            } else {
                System.out.println("[Kafka] Order " + message.orderId + " transitioned to " + sm.getCurrentState());
                sm.processPendingEvents(
                    message.orderId,
                    StateMachineRepository.getPendingEvents(message.orderId),
                    (OrderEventMessage m) -> {
                        try {
                            return OrderEvent.valueOf(m.event.toUpperCase());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    () -> {
                        StateMachineRepository.clearPendingEvents(message.orderId);
                        StateMachineRepository.remove(message.orderId);
                    }
                );
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[Kafka] Invalid event: " + message.event);
        }
    }
} 