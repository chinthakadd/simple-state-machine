package com.chinthakad.statemachine.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.Set;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateMachine<S, E> {
    private static final Logger log = LoggerFactory.getLogger(StateMachine.class);
    private S currentState;
    private final Map<S, Map<E, S>> transitions = new HashMap<>();
    private final Map<E, BiConsumer<S, S>> callbacks = new HashMap<>();
    private final Map<TransitionKey<S, E>, BiConsumer<S, S>> transitionCallbacks = new HashMap<>();
    private Consumer<String> invalidTransitionHandler = msg -> log.error(msg);

    public StateMachine(S initialState) {
        this.currentState = initialState;
    }

    public void addTransition(S from, E event, S to) {
        transitions.computeIfAbsent(from, k -> new HashMap<>()).put(event, to);
    }

    public void addCallback(E event, BiConsumer<S, S> callback) {
        callbacks.put(event, callback);
    }

    public void addTransitionCallback(S from, E event, S to, BiConsumer<S, S> callback) {
        transitionCallbacks.put(new TransitionKey<>(from, event, to), callback);
    }

    public void setInvalidTransitionHandler(Consumer<String> handler) {
        this.invalidTransitionHandler = handler;
    }

    public synchronized boolean trigger(E event) {
        // Reject transitions if in terminal state (generic)
        if (currentState instanceof TerminalState && ((TerminalState) currentState).isTerminal()) {
            invalidTransitionHandler.accept("No transitions allowed from terminal state: " + currentState);
            return false;
        }
        Map<E, S> stateTransitions = transitions.get(currentState);
        if (stateTransitions != null && stateTransitions.containsKey(event)) {
            S prevState = currentState;
            S nextState = stateTransitions.get(event);
            currentState = nextState;
            // Per-transition callback
            BiConsumer<S, S> transitionCb = transitionCallbacks.get(new TransitionKey<>(prevState, event, nextState));
            if (transitionCb != null) {
                transitionCb.accept(prevState, nextState);
            } else if (callbacks.containsKey(event)) {
                callbacks.get(event).accept(prevState, nextState);
            }
            return true;
        } else {
            invalidTransitionHandler.accept("Invalid transition: " + currentState + " --(" + event + ")-> ?");
            return false;
        }
    }

    /**
     * Process pending events for this state machine.
     * @param id The state machine ID (for cleanup callback)
     * @param pending Queue of pending messages
     * @param eventExtractor Function to extract event from message
     * @param onTerminal Cleanup callback when terminal state is reached
     * @param <M> Message type
     */
    public <M> void processPendingEvents(String id, Queue<M> pending, Function<M, E> eventExtractor, Runnable onTerminal) {
        boolean progress;
        do {
            progress = false;
            Iterator<M> it = pending.iterator();
            while (it.hasNext()) {
                M msg = it.next();
                E event;
                try {
                    event = eventExtractor.apply(msg);
                } catch (Exception e) {
                    it.remove();
                    progress = true;
                    break;
                }
                if (this.trigger(event)) {
                    it.remove();
                    progress = true;
                    if (currentState instanceof TerminalState && ((TerminalState) currentState).isTerminal()) {
                        onTerminal.run();
                        return;
                    }
                    break;
                }
            }
        } while (progress);
        if (currentState instanceof TerminalState && ((TerminalState) currentState).isTerminal()) {
            onTerminal.run();
        }
    }

    public S getCurrentState() {
        return currentState;
    }
} 