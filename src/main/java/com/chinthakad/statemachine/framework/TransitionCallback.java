package com.chinthakad.statemachine.framework;

@FunctionalInterface
public interface TransitionCallback<S> {
    void onTransition(S from, S to);
} 