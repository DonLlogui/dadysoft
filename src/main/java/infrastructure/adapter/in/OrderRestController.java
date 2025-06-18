package infrastructure.adapter.in;

import com.comercio.dadysoft.application.port.in.OrderServicePort;
import com.comercio.dadysoft.infrastructure.exception.InvalidOrderStateException;
import com.comercio.dadysoft.infrastructure.exception.ResourceNotFoundException;
import com.comercio.dadysoft.shared.dto.OrderRequest;
import com.comercio.dadysoft.shared.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para la gestión de pedidos.
 * Actúa como un adaptador de entrada, exponiendo los endpoints de la API REST.
 * Delega las operaciones de negocio al OrderServicePort.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor // Para inyección de dependencias a través del constructor
@Slf4j // Para logging
@Tag(name = "Order Management", description = "API para la gestión de pedidos de la plataforma E-commerce")
public class OrderRestController {

    private final OrderServicePort orderServicePort;

    @Operation(summary = "Crea un nuevo pedido",
            description = "Permite a un cliente crear un nuevo pedido con los productos y cantidades especificados.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente",
                            mediaType = "application/json",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Solicitud inválida o stock insuficiente"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
            })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        log.info("Received request to create order for customer ID: {}", orderRequest.getCustomerId());
        try {
            OrderResponse createdOrder = orderServicePort.createOrder(orderRequest);
            log.info("Order created successfully with ID: {}", createdOrder.getId());
            return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
        } catch (InvalidOrderStateException e) {
            log.error("Failed to create order due to invalid state: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Obtiene un pedido por su ID",
            description = "Recupera los detalles de un pedido específico utilizando su identificador único.",
            parameters = @Parameter(name = "id", description = "ID del pedido a buscar", required = true, example = "1"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                            mediaType = "application/json",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
            })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("Received request to get order by ID: {}", id);
        return orderServicePort.getOrderById(id)
                .map(order -> {
                    log.info("Order with ID {} found.", id);
                    return new ResponseEntity<>(order, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    log.warn("Order with ID {} not found.", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    @Operation(summary = "Obtiene pedidos por ID de cliente",
            description = "Recupera una lista de todos los pedidos realizados por un cliente específico.",
            parameters = @Parameter(name = "customerId", description = "ID del cliente", required = true, example = "101"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pedidos encontrados para el cliente",
                            mediaType = "application/json",
                            content = @Content(schema = @Schema(type = "array", implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Cliente o pedidos no encontrados")
            })
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerId(@PathVariable Long customerId) {
        log.info("Received request to get orders by customer ID: {}", customerId);
        List<OrderResponse> orders = orderServicePort.getOrdersByCustomerId(customerId);
        if (orders.isEmpty()) {
            log.warn("No orders found for customer ID: {}", customerId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // O HttpStatus.OK con lista vacía, según la preferencia
        }
        log.info("Found {} orders for customer ID: {}", orders.size(), customerId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @Operation(summary = "Busca pedidos por criterios",
            description = "Permite buscar pedidos por estado, rango de fechas y/o ID de cliente. Todos los parámetros son opcionales.",
            parameters = {
                    @Parameter(name = "status", description = "Estado del pedido (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED, PAID)", example = "SHIPPED"),
                    @Parameter(name = "startDate", description = "Fecha y hora de inicio para la búsqueda (formato yyyy-MM-dd'T'HH:mm:ss)", example = "2024-01-01T00:00:00"),
                    @Parameter(name = "endDate", description = "Fecha y hora de fin para la búsqueda (formato yyyy-MM-dd'T'HH:mm:ss)", example = "2024-12-31T23:59:59"),
                    @Parameter(name = "customerId", description = "ID del cliente", example = "102")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de pedidos que coinciden con los criterios",
                            mediaType = "application/json",
                            content = @Content(schema = @Schema(type = "array", implementation = OrderResponse.class)))
            })
    @GetMapping("/search")
    public ResponseEntity<List<OrderResponse>> searchOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long customerId) {
        log.info("Received search request for orders. Status: {}, StartDate: {}, EndDate: {}, CustomerId: {}",
                status, startDate, endDate, customerId);
        List<OrderResponse> orders = orderServicePort.searchOrders(status, startDate, endDate, customerId);
        log.info("Found {} orders matching the search criteria.", orders.size());
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }


    @Operation(summary = "Cancela un pedido existente",
            description = "Cancela un pedido por su ID, liberando stock y gestionando reembolsos si aplica.",
            parameters = @Parameter(name = "id", description = "ID del pedido a cancelar", required = true, example = "1"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Pedido cancelado exitosamente"),
                    @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
                    @ApiResponse(responseCode = "400", description = "El pedido no puede ser cancelado en su estado actual"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
            })
    @PatchMapping("/{id}/cancel") // Usamos PATCH para operaciones parciales/de estado
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        log.info("Received request to cancel order with ID: {}", id);
        try {
            orderServicePort.cancelOrder(id);
            log.info("Order ID {} cancelled successfully.", id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
        } catch (ResourceNotFoundException e) {
            log.error("Failed to cancel order: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (InvalidOrderStateException e) {
            log.error("Failed to cancel order due to invalid state: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error cancelling order ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Actualiza los ítems de un pedido",
            description = "Modifica los productos y cantidades de un pedido existente. Solo permitido en estados específicos (ej. PENDING, PROCESSING).",
            parameters = @Parameter(name = "orderId", description = "ID del pedido a actualizar", required = true, example = "1"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ítems del pedido actualizados exitosamente",
                            mediaType = "application/json",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
                    @ApiResponse(responseCode = "400", description = "Solicitud inválida o el pedido no puede ser modificado"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
            })
    @PutMapping("/{orderId}/items") // PUT para reemplazar completamente los ítems
    public ResponseEntity<OrderResponse> updateOrderItems(
            @PathVariable Long orderId,
            @Valid @RequestBody List<OrderRequest.OrderItemRequest> itemsRequest) {
        log.info("Received request to update items for order ID: {}", orderId);
        try {
            OrderResponse updatedOrder = orderServicePort.updateOrderItems(orderId, itemsRequest);
            log.info("Items for order ID {} updated successfully.", orderId);
            return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to update order items: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (InvalidOrderStateException e) {
            log.error("Failed to update order items due to invalid state: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error updating items for order ID {}: {}", orderId, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Nota: Los endpoints para confirmación de pago y actualización de estado
    // desde eventos de Kafka no se exponen vía REST, ya que son manejados
    // por un consumidor de Kafka (KafkaOrderStatusConsumer).
}