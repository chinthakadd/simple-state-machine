package com.chinthakad.statemachine.framework;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StateMachineBuilder<S, E> {
    private final StateMachine<S, E> stateMachine;

    public StateMachineBuilder(S initialState) {
        this.stateMachine = new StateMachine<>(initialState);
    }

    public StateMachineBuilder<S, E> transition(S from, E event, S to) {
        stateMachine.addTransition(from, event, to);
        return this;
    }

    public StateMachineBuilder<S, E> onTransition(E event, BiConsumer<S, S> callback) {
        stateMachine.addCallback(event, callback);
        return this;
    }

    public StateMachineBuilder<S, E> onTransition(S from, E event, S to, BiConsumer<S, S> callback) {
        stateMachine.addTransitionCallback(from, event, to, callback);
        return this;
    }

    public StateMachineBuilder<S, E> onInvalidTransition(Consumer<String> handler) {
        stateMachine.setInvalidTransitionHandler(handler);
        return this;
    }

    public StateMachine<S, E> build() {
        return stateMachine;
    }
} 