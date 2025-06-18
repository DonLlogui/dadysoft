package application.service;

package com.comercio.dadysoft.application.service;

import com.comercio.dadysoft.application.port.in.OrderServicePort;
import com.comercio.dadysoft.application.port.out.InventoryServicePort;
import com.comercio.dadysoft.application.port.out.OrderEventPublisherPort;
import com.comercio.dadysoft.application.port.out.OrderRepositoryPort;
import com.comercio.dadysoft.application.port.out.PaymentServicePort;
import com.comercio.dadysoft.domain.model.Order;
import com.comercio.dadysoft.domain.model.OrderItem;
import com.comercio.dadysoft.domain.model.OrderStatus;
import com.comercio.dadysoft.domain.model.PaymentStatus;
import com.comercio.dadysoft.infrastructure.exception.InvalidOrderStateException;
import com.comercio.dadysoft.infrastructure.exception.ResourceNotFoundException;
import com.comercio.dadysoft.shared.dto.OrderEvent;
import com.comercio.dadysoft.shared.dto.OrderRequest;
import com.comercio.dadysoft.shared.dto.OrderResponse;
import com.comercio.dadysoft.shared.dto.PaymentResponse;
import com.comercio.dadysoft.shared.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión de pedidos.
 * Contiene la lógica de negocio central y orquesta las interacciones con los puertos de salida.
 */
@Service
@RequiredArgsConstructor // Genera un constructor con todos los campos finales para inyección de dependencias
@Slf4j // Para logging con Lombok
public class OrderServiceImpl implements OrderServicePort {

    private final OrderRepositoryPort orderRepositoryPort;
    private final InventoryServicePort inventoryServicePort;
    private final PaymentServicePort paymentServicePort;
    private final OrderEventPublisherPort orderEventPublisherPort;
    private final OrderMapper orderMapper; // Inyecta el ModelMapper a través de una interfaz Mapper

    @Override
    @Transactional // Asegura que todas las operaciones en este método sean parte de una misma transacción
    public OrderResponse createOrder(OrderRequest orderRequest) {
        log.info("Attempting to create order for customer ID: {}", orderRequest.getCustomerId());

        // 1. Mapear DTO a entidad de dominio
        Order newOrder = orderMapper.toDomain(orderRequest);
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setStatus(OrderStatus.PENDING); // Estado inicial
        newOrder.setPaymentStatus(PaymentStatus.PENDING); // Estado inicial de pago
        newOrder.setTotalAmount(BigDecimal.ZERO); // Se calculará al añadir ítems

        // 2. Preparar mapa de productos y cantidades para el servicio de inventario
        Map<Long, Integer> productQuantities = new HashMap<>();
        if (orderRequest.getItems() != null) {
            orderRequest.getItems().forEach(itemRequest ->
                    productQuantities.put(itemRequest.getProductId(), itemRequest.getQuantity())
            );
        }

        // 3. Verificar y reservar stock con el servicio de inventario
        if (!productQuantities.isEmpty()) {
            boolean available = inventoryServicePort.checkAvailability(productQuantities);
            if (!available) {
                log.warn("Insufficient stock for order for customer ID: {}", orderRequest.getCustomerId());
                throw new InvalidOrderStateException("Insufficient stock for one or more products.");
            }
            boolean reserved = inventoryServicePort.reserveStock(productQuantities);
            if (!reserved) {
                log.error("Failed to reserve stock for order for customer ID: {}", orderRequest.getCustomerId());
                throw new RuntimeException("Failed to reserve stock for the order."); // O una excepción más específica
            }
            log.info("Stock reserved successfully for order for customer ID: {}", orderRequest.getCustomerId());
        }

        // 4. Calcular el monto total y añadir ítems al pedido (asumimos que el precio viene del request por ahora,
        //    en un sistema real se consultaría el precio actual del producto).
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (orderRequest.getItems() != null) {
            for (OrderRequest.OrderItemRequest itemReq : orderRequest.getItems()) {
                OrderItem item = OrderItem.builder()
                        .productId(itemReq.getProductId())
                        .quantity(itemReq.getQuantity())
                        .pricePerUnit(itemReq.getPricePerUnit()) // Asegúrate que el precio es el correcto
                        .build();
                newOrder.addItem(item); // Añade el ítem al pedido (el método addItem en Order debe calcular el total)
                totalAmount = totalAmount.add(item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        newOrder.setTotalAmount(totalAmount);


        // 5. Guardar el pedido en la base de datos
        Order savedOrder = orderRepositoryPort.save(newOrder);
        log.info("Order saved successfully with ID: {}", savedOrder.getId());

        // 6. Procesar el pago (simulado/sincrónico inicial)
        // Esto podría ser asíncrono en un sistema real, pero lo hacemos aquí para el flujo básico.
        PaymentResponse paymentResponse = paymentServicePort.processPayment(
                savedOrder.getId(),
                savedOrder.getTotalAmount(),
                orderRequest.getPaymentMethod()
        );

        if ("SUCCESS".equals(paymentResponse.getStatus())) {
            savedOrder.setPaymentStatus(PaymentStatus.PAID);
            savedOrder.setStatus(OrderStatus.PROCESSING); // El pedido pasa a PROCESSING una vez pagado
            log.info("Payment successful for order ID: {}", savedOrder.getId());
            orderEventPublisherPort.publishOrderPaymentSuccessEvent(
                    savedOrder.getId(), paymentResponse.getTransactionReference(), savedOrder.getTotalAmount().doubleValue());
        } else {
            savedOrder.setPaymentStatus(PaymentStatus.FAILED);
            // Si el pago falla, la reserva de stock debe ser liberada
            if (!productQuantities.isEmpty()) {
                inventoryServicePort.releaseStock(productQuantities);
                log.warn("Payment failed for order ID: {}. Stock released.", savedOrder.getId());
            }
            log.error("Payment failed for order ID: {}", savedOrder.getId());
            orderEventPublisherPort.publishOrderPaymentFailedEvent(
                    savedOrder.getId(), paymentResponse.getTransactionReference(), paymentResponse.getMessage());
            savedOrder = orderRepositoryPort.save(savedOrder); // Guardar el estado actualizado
            throw new InvalidOrderStateException("Payment failed: " + paymentResponse.getMessage());
        }

        savedOrder = orderRepositoryPort.save(savedOrder); // Guardar el estado final del pedido (pagado/procesando)

        // 7. Publicar evento de creación de pedido
        orderEventPublisherPort.publishOrderEvent(OrderEvent.builder()
                .orderId(savedOrder.getId())
                .eventType(OrderEvent.EventType.ORDER_CREATED)
                .details("Order created successfully and paid.")
                .timestamp(LocalDateTime.now())
                .customerId(savedOrder.getCustomerId())
                .build());

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);
        return orderRepositoryPort.findById(id)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        log.info("Fetching orders for customer ID: {}", customerId);
        List<Order> orders = orderRepositoryPort.findByCustomerId(customerId);
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> searchOrders(String status, LocalDateTime startDate, LocalDateTime endDate, Long customerId) {
        log.info("Searching orders with status: {}, startDate: {}, endDate: {}, customerId: {}", status, startDate, endDate, customerId);
        OrderStatus orderStatus = (status != null) ? OrderStatus.valueOf(status.toUpperCase()) : null;
        List<Order> orders = orderRepositoryPort.findByCriteria(orderStatus, startDate, endDate, customerId);
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        log.info("Attempting to cancel order with ID: {}", id);
        Order order = orderRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Cannot cancel order ID {} in status: {}", id, order.getStatus());
            throw new InvalidOrderStateException("Order cannot be cancelled in status: " + order.getStatus());
        }

        // Liberar stock para cada ítem del pedido cancelado
        Map<Long, Integer> productQuantitiesToRelease = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));

        if (!productQuantitiesToRelease.isEmpty()) {
            boolean released = inventoryServicePort.releaseStock(productQuantitiesToRelease);
            if (!released) {
                log.error("Failed to release stock for cancelled order ID: {}", id);
                // Considerar una acción de compensación o registrar el error
                throw new RuntimeException("Failed to release stock for cancelled order.");
            }
            log.info("Stock released for cancelled order ID: {}", id);
        }

        // Si el pedido ya estaba pagado, iniciar un reembolso (simulado)
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Order ID {} was paid, initiating refund.", id);
            // Aquí deberías tener una referencia de transacción real
            // Por simplicidad, usaremos una dummy. En un sistema real, PaymentServiceAdapter la devolvería.
            PaymentResponse refundResponse = paymentServicePort.refundPayment(order.getId(), "dummy_transaction_ref_" + id, order.getTotalAmount());
            if ("SUCCESS".equals(refundResponse.getStatus())) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Refund successful for order ID: {}", id);
            } else {
                log.error("Refund failed for order ID: {}", id);
                // Podrías lanzar una excepción o marcar el pedido con un estado de reembolso fallido.
                throw new InvalidOrderStateException("Refund failed for order: " + refundResponse.getMessage());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepositoryPort.save(order);
        log.info("Order ID {} cancelled successfully.", id);

        orderEventPublisherPort.publishOrderCancelledEvent(id, "Customer requested cancellation");
    }

    @Override
    @Transactional
    public OrderResponse updateOrderItems(Long orderId, List<OrderRequest.OrderItemRequest> itemsRequest) {
        log.info("Attempting to update items for order with ID: {}", orderId);
        Order existingOrder = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (existingOrder.getStatus() != OrderStatus.PENDING && existingOrder.getStatus() != OrderStatus.PROCESSING) {
            log.warn("Cannot update items for order ID {} in status: {}", orderId, existingOrder.getStatus());
            throw new InvalidOrderStateException("Order items can only be updated if order is PENDING or PROCESSING.");
        }

        // 1. Liberar stock de ítems antiguos
        Map<Long, Integer> oldProductQuantities = existingOrder.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));
        if (!oldProductQuantities.isEmpty()) {
            inventoryServicePort.releaseStock(oldProductQuantities);
            log.info("Released stock for old items of order ID: {}", orderId);
        }

        // 2. Preparar mapa de nuevos productos y cantidades
        Map<Long, Integer> newProductQuantities = new HashMap<>();
        if (itemsRequest != null) {
            itemsRequest.forEach(itemRequest ->
                    newProductQuantities.put(itemRequest.getProductId(), itemRequest.getQuantity())
            );
        }

        // 3. Verificar y reservar stock para nuevos ítems
        if (!newProductQuantities.isEmpty()) {
            boolean available = inventoryServicePort.checkAvailability(newProductQuantities);
            if (!available) {
                // Si no hay stock, intentar reservar el stock antiguo antes de lanzar excepción
                inventoryServicePort.reserveStock(oldProductQuantities); // Revertir la liberación
                log.warn("Insufficient stock for new items for order ID: {}", orderId);
                throw new InvalidOrderStateException("Insufficient stock for one or more new products.");
            }
            boolean reserved = inventoryServicePort.reserveStock(newProductQuantities);
            if (!reserved) {
                inventoryServicePort.reserveStock(oldProductQuantities); // Revertir la liberación
                log.error("Failed to reserve stock for new items for order ID: {}", orderId);
                throw new RuntimeException("Failed to reserve stock for the updated order.");
            }
            log.info("Stock reserved for new items for order ID: {}", orderId);
        }

        // 4. Actualizar ítems del pedido y recalcular total
        existingOrder.clearItems(); // Elimina ítems antiguos
        BigDecimal newTotalAmount = BigDecimal.ZERO;
        if (itemsRequest != null) {
            for (OrderRequest.OrderItemRequest itemReq : itemsRequest) {
                OrderItem item = OrderItem.builder()
                        .productId(itemReq.getProductId())
                        .quantity(itemReq.getQuantity())
                        .pricePerUnit(itemReq.getPricePerUnit())
                        .build();
                existingOrder.addItem(item);
                newTotalAmount = newTotalAmount.add(item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        existingOrder.setTotalAmount(newTotalAmount);

        Order updatedOrder = orderRepositoryPort.save(existingOrder);
        log.info("Order items updated successfully for order ID: {}", orderId);

        orderEventPublisherPort.publishOrderEvent(OrderEvent.builder()
                .orderId(updatedOrder.getId())
                .eventType(OrderEvent.EventType.ORDER_UPDATED)
                .details("Order items updated.")
                .timestamp(LocalDateTime.now())
                .customerId(updatedOrder.getCustomerId())
                .build());

        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    @Transactional
    public void processPaymentConfirmation(Long orderId, String transactionReference, String status) {
        log.info("Processing payment confirmation for order ID: {} with status: {}", orderId, status);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if ("SUCCESS".equalsIgnoreCase(status)) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.PROCESSING); // Move to processing after successful payment
            log.info("Payment confirmed as SUCCESS for order ID: {}", orderId);
            orderEventPublisherPort.publishOrderPaymentSuccessEvent(orderId, transactionReference, order.getTotalAmount().doubleValue());
        } else if ("FAILED".equalsIgnoreCase(status)) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            // Si el pago falla de forma asíncrona, se debe liberar el stock
            Map<Long, Integer> productQuantitiesToRelease = order.getItems().stream()
                    .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));
            if (!productQuantitiesToRelease.isEmpty()) {
                inventoryServicePort.releaseStock(productQuantitiesToRelease);
                log.warn("Payment failed for order ID: {}. Stock released.", orderId);
            }
            log.error("Payment confirmed as FAILED for order ID: {}", orderId);
            orderEventPublisherPort.publishOrderPaymentFailedEvent(orderId, transactionReference, "Payment processor reported failure.");
        } else {
            log.warn("Unknown payment status received for order ID: {}: {}", orderId, status);
            // Podrías manejar un estado PENDING si el flujo lo requiere.
        }
        orderRepositoryPort.save(order);
    }

    @Override
    @Transactional
    public void updateOrderStatusFromEvent(Long orderId, String newStatus) {
        log.info("Updating order status for ID: {} to: {} from event.", orderId, newStatus);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        try {
            OrderStatus statusEnum = OrderStatus.valueOf(newStatus.toUpperCase());
            // Validaciones de transición de estado, si las hay (ej. no pasar de DELIVERED a PENDING)
            if (statusEnum == OrderStatus.SHIPPED && order.getStatus() != OrderStatus.PROCESSING) {
                log.warn("Order ID {} cannot transition to SHIPPED from status {}", orderId, order.getStatus());
                throw new InvalidOrderStateException("Invalid state transition to SHIPPED from " + order.getStatus());
            }
            if (statusEnum == OrderStatus.DELIVERED && order.getStatus() != OrderStatus.SHIPPED) {
                log.warn("Order ID {} cannot transition to DELIVERED from status {}", orderId, order.getStatus());
                throw new InvalidOrderStateException("Invalid state transition to DELIVERED from " + order.getStatus());
            }

            order.setStatus(statusEnum);
            orderRepositoryPort.save(order);
            log.info("Order ID {} status updated to {}.", orderId, newStatus);

            // Si el pedido llega a SHIPPED o DELIVERED, confirmar el uso del stock
            if (statusEnum == OrderStatus.SHIPPED || statusEnum == OrderStatus.DELIVERED) {
                Map<Long, Integer> productQuantitiesToConfirm = order.getItems().stream()
                        .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));
                if (!productQuantitiesToConfirm.isEmpty()) {
                    boolean confirmed = inventoryServicePort.confirmStockUsage(productQuantitiesToConfirm);
                    if (!confirmed) {
                        log.error("Failed to confirm stock usage for order ID: {} after status update to {}", orderId, newStatus);
                        // Esto podría requerir una intervención manual o un mecanismo de reintento.
                    }
                }
            }

            orderEventPublisherPort.publishOrderStatusUpdatedEvent(orderId, newStatus);

        } catch (IllegalArgumentException e) {
            log.error("Invalid status string received for order ID {}: {}. Error: {}", orderId, newStatus, e.getMessage());
            throw new InvalidOrderStateException("Invalid status provided: " + newStatus);
        }
    }
}