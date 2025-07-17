package com.chinthakad.statemachine.example;

import com.chinthakad.statemachine.framework.StateMachine;
import com.chinthakad.statemachine.framework.StateMachineBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderStateMachineFactory {
    private static final Logger log = LoggerFactory.getLogger(OrderStateMachineFactory.class);
    public static StateMachine<OrderState, OrderEvent> create() {
        return new StateMachineBuilder<OrderState, OrderEvent>(OrderState.CREATED)
                .transition(OrderState.CREATED, OrderEvent.PAY, OrderState.PAID)
                .transition(OrderState.PAID, OrderEvent.SHIP, OrderState.SHIPPED)
                .transition(OrderState.CREATED, OrderEvent.CANCEL, OrderState.CANCELLED)
                .transition(OrderState.PAID, OrderEvent.CANCEL, OrderState.CANCELLED)
                .transition(OrderState.SHIPPED, OrderEvent.DELIVER, OrderState.DELIVERED)
                .onTransition(OrderState.CREATED, OrderEvent.SHIP, OrderState.SHIPPED, (from, to) -> {
                    log.info("Order shortcut: CREATED -> SHIPPED. Performing both pay and ship logic.");
                    // Simulate both pay and ship logic
                })
                .onTransition(OrderEvent.PAY, (from, to) -> log.info("Order paid: {} -> {}", from, to))
                .onTransition(OrderEvent.SHIP, (from, to) -> log.info("Order shipped: {} -> {}", from, to))
                .onTransition(OrderEvent.CANCEL, (from, to) -> log.info("Order cancelled: {} -> {}", from, to))
                .onTransition(OrderEvent.DELIVER, (from, to) -> log.info("Order delivered: {} -> {}", from, to))
                .onInvalidTransition(msg -> log.error("[OrderStateMachine] {}", msg))
                .build();
    }
} 