package com.chinthakad.statemachine.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.chinthakad.statemachine.framework.StateMachine;

import java.util.Queue;
import java.util.LinkedList;

public class StateMachineRepository {
    private static final Map<String, StateMachine<OrderState, OrderEvent>> orders = new ConcurrentHashMap<>();
    private static final Map<String, Queue<OrderEventMessage>> pendingEvents = new ConcurrentHashMap<>();

    public static void save(String id, StateMachine<OrderState, OrderEvent> sm) {
        orders.put(id, sm);
    }

    public static StateMachine<OrderState, OrderEvent> get(String id) {
        return orders.get(id);
    }

    public static boolean exists(String id) {
        return orders.containsKey(id);
    }

    public static void addPendingEvent(String id, OrderEventMessage message) {
        pendingEvents.computeIfAbsent(id, k -> new LinkedList<>()).add(message);
    }

    public static Queue<OrderEventMessage> getPendingEvents(String id) {
        return pendingEvents.getOrDefault(id, new LinkedList<>());
    }

    public static void clearPendingEvents(String id) {
        pendingEvents.remove(id);
    }

    public static void remove(String id) {
        orders.remove(id);
    }
} 