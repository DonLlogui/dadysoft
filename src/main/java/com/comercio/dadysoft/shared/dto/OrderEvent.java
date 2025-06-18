package com.comercio.dadysoft.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    public enum EventType {
        ORDER_CREATED,
        ORDER_UPDATED,
        ORDER_CANCELLED,
        ORDER_PAYMENT_SUCCESS,
        ORDER_PAYMENT_FAILED,
        ORDER_SHIPPED,
        ORDER_DELIVERED
        // Agrega más tipos de eventos según sea necesario
    }

    private Long orderId;
    private EventType eventType;
    private String details; // Mensaje o descripción del evento
    private LocalDateTime timestamp;
    private Long customerId; // Útil para algunos eventos
    // Puedes añadir más campos relevantes para el evento, ej. para un ORDER_CREATED:
    // private OrderResponse orderData;
}
