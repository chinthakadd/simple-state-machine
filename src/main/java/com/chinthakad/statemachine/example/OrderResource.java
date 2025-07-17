package com.chinthakad.statemachine.example;

import com.chinthakad.statemachine.framework.StateMachine;
import com.chinthakad.statemachine.framework.StateMachineRepository;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/order")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @POST
    public Response createOrder() {
        String id = java.util.UUID.randomUUID().toString();
        StateMachine<OrderState, OrderEvent> sm = OrderStateMachineFactory.create();
        OrderEventConsumer.getRepository().save(id, sm);
        return Response.ok(Map.of("id", id)).build();
    }

    @POST
    @Path("{id}/trigger/{event}")
    public Response triggerEvent(@PathParam("id") String id, @PathParam("event") String eventStr) {
        StateMachine<OrderState, OrderEvent> sm = OrderEventConsumer.getRepository().get(id);
        if (sm == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Order not found")).build();
        }
        OrderEvent event;
        try {
            event = OrderEvent.valueOf(eventStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Invalid event")).build();
        }
        boolean success = sm.trigger(event);
        if (!success) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Invalid transition", "state", sm.getCurrentState())).build();
        }
        return Response.ok(Map.of("state", sm.getCurrentState())).build();
    }

    @GET
    @Path("{id}")
    public Response getState(@PathParam("id") String id) {
        StateMachine<OrderState, OrderEvent> sm = OrderEventConsumer.getRepository().get(id);
        if (sm == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Order not found")).build();
        }
        return Response.ok(Map.of("state", sm.getCurrentState())).build();
    }
} 