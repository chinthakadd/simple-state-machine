package com.chinthakad.statemachine.framework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class StateMachineTest {
    enum SimpleState implements TerminalState {
        INIT, RUNNING, DONE, FAILED;
        public boolean isTerminal() { return this == DONE || this == FAILED; }
    }
    enum SimpleEvent { START, FINISH, ERROR }

    StateMachine<SimpleState, SimpleEvent> sm;

    @BeforeEach
    void setup() {
        sm = new StateMachineBuilder<SimpleState, SimpleEvent>(SimpleState.INIT)
                .transition(SimpleState.INIT, SimpleEvent.START, SimpleState.RUNNING)
                .transition(SimpleState.RUNNING, SimpleEvent.FINISH, SimpleState.DONE)
                .transition(SimpleState.RUNNING, SimpleEvent.ERROR, SimpleState.FAILED)
                .onTransition(SimpleEvent.START, (from, to) -> {})
                .onTransition(SimpleEvent.FINISH, (from, to) -> {})
                .onTransition(SimpleEvent.ERROR, (from, to) -> {})
                .build();
    }

    @Test
    void testNormalTransition() {
        assertEquals(SimpleState.INIT, sm.getCurrentState());
        assertTrue(sm.trigger(SimpleEvent.START));
        assertEquals(SimpleState.RUNNING, sm.getCurrentState());
        assertTrue(sm.trigger(SimpleEvent.FINISH));
        assertEquals(SimpleState.DONE, sm.getCurrentState());
    }

    @Test
    void testInvalidTransition() {
        assertFalse(sm.trigger(SimpleEvent.FINISH)); // Can't finish from INIT
        assertEquals(SimpleState.INIT, sm.getCurrentState());
    }

    @Test
    void testPerTransitionCallback() {
        AtomicBoolean called = new AtomicBoolean(false);
        sm = new StateMachineBuilder<SimpleState, SimpleEvent>(SimpleState.INIT)
                .transition(SimpleState.INIT, SimpleEvent.START, SimpleState.RUNNING)
                .onTransition(SimpleState.INIT, SimpleEvent.START, SimpleState.RUNNING, (from, to) -> called.set(true))
                .build();
        sm.trigger(SimpleEvent.START);
        assertTrue(called.get());
    }

    @Test
    void testTerminalStatePreventsFurtherTransitions() {
        sm.trigger(SimpleEvent.START);
        sm.trigger(SimpleEvent.FINISH);
        assertEquals(SimpleState.DONE, sm.getCurrentState());
        assertFalse(sm.trigger(SimpleEvent.ERROR));
        assertEquals(SimpleState.DONE, sm.getCurrentState());
    }

    @Test
    void testProcessPendingEvents() {
        Queue<SimpleEvent> pending = new LinkedList<>();
        pending.add(SimpleEvent.FINISH); // invalid at INIT
        pending.add(SimpleEvent.START);  // valid at INIT
        AtomicReference<SimpleState> lastState = new AtomicReference<>(null);
        sm = new StateMachineBuilder<SimpleState, SimpleEvent>(SimpleState.INIT)
                .transition(SimpleState.INIT, SimpleEvent.START, SimpleState.RUNNING)
                .transition(SimpleState.RUNNING, SimpleEvent.FINISH, SimpleState.DONE)
                .onTransition(SimpleEvent.FINISH, (from, to) -> lastState.set(to))
                .build();
        sm.processPendingEvents("id", pending, e -> e, () -> {});
        assertEquals(SimpleState.DONE, sm.getCurrentState());
        assertEquals(SimpleState.DONE, lastState.get());
        assertTrue(pending.isEmpty());
    }
} 