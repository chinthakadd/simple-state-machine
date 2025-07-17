package com.chinthakad.statemachine.framework;
import java.util.Objects;

public class TransitionKey<S, E> {
    private final S from;
    private final E event;
    private final S to;

    public TransitionKey(S from, E event, S to) {
        this.from = from;
        this.event = event;
        this.to = to;
    }

    public S getFrom() { return from; }
    public E getEvent() { return event; }
    public S getTo() { return to; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransitionKey<?, ?> that = (TransitionKey<?, ?>) o;
        return Objects.equals(from, that.from) &&
               Objects.equals(event, that.event) &&
               Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, event, to);
    }
} 