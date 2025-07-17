package com.chinthakad.statemachine.example;

import com.chinthakad.statemachine.framework.StateMachine;
import com.chinthakad.statemachine.framework.StateMachineBuilder;

public class OrderStateMachineFactory {
    public static StateMachine<OrderState, OrderEvent> create() {
        return new StateMachineBuilder<OrderState, OrderEvent>(OrderState.CREATED)
                .transition(OrderState.CREATED, OrderEvent.PAY, OrderState.PAID)
                .transition(OrderState.PAID, OrderEvent.SHIP, OrderState.SHIPPED)
                .transition(OrderState.CREATED, OrderEvent.CANCEL, OrderState.CANCELLED)
                .transition(OrderState.PAID, OrderEvent.CANCEL, OrderState.CANCELLED)
                .transition(OrderState.SHIPPED, OrderEvent.DELIVER, OrderState.DELIVERED)
                .onTransition(OrderState.CREATED, OrderEvent.SHIP, OrderState.SHIPPED, (from, to) -> {
                    System.out.println("Order shortcut: CREATED -> SHIPPED. Performing both pay and ship logic.");
                    // Simulate both pay and ship logic
                })
                .onTransition(OrderEvent.PAY, (from, to) -> System.out.println("Order paid: " + from + " -> " + to))
                .onTransition(OrderEvent.SHIP, (from, to) -> System.out.println("Order shipped: " + from + " -> " + to))
                .onTransition(OrderEvent.CANCEL, (from, to) -> System.out.println("Order cancelled: " + from + " -> " + to))
                .onTransition(OrderEvent.DELIVER, (from, to) -> System.out.println("Order delivered: " + from + " -> " + to))
                .onInvalidTransition(msg -> System.err.println("[OrderStateMachine] " + msg))
                .build();
    }
} 