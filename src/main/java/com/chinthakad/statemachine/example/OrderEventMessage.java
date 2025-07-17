package com.chinthakad.statemachine.example;

public class OrderEventMessage {
    public String orderId;
    public String event;

    // Default constructor for deserialization
    public OrderEventMessage() {}

    public OrderEventMessage(String orderId, String event) {
        this.orderId = orderId;
        this.event = event;
    }
} 