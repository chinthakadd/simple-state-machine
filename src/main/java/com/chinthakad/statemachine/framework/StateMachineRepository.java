package com.chinthakad.statemachine.framework;

import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class StateMachineRepository<S, E, M> {
    private final Map<String, StateMachine<S, E>> machines = new ConcurrentHashMap<>();
    private final Map<String, Queue<M>> pendingEvents = new ConcurrentHashMap<>();

    public void save(String id, StateMachine<S, E> sm) {
        machines.put(id, sm);
    }

    public StateMachine<S, E> get(String id) {
        return machines.get(id);
    }

    public boolean exists(String id) {
        return machines.containsKey(id);
    }

    public void addPendingEvent(String id, M message) {
        pendingEvents.computeIfAbsent(id, k -> new LinkedList<>()).add(message);
    }

    public Queue<M> getPendingEvents(String id) {
        return pendingEvents.getOrDefault(id, new LinkedList<>());
    }

    public void clearPendingEvents(String id) {
        pendingEvents.remove(id);
    }

    public void remove(String id) {
        machines.remove(id);
        pendingEvents.remove(id);
    }
} 