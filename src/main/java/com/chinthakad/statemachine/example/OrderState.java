package com.chinthakad.statemachine.example;

public enum OrderState implements com.chinthakad.statemachine.framework.TerminalState {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean isTerminal() {
        return this == CANCELLED || this == DELIVERED;
    }
} 