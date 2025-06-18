package application.port.in;

import com.comercio.dadysoft.domain.model.Order;
import com.comercio.dadysoft.shared.dto.OrderRequest;
import com.comercio.dadysoft.shared.dto.OrderResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada para el servicio de gestión de pedidos.
 * Define las operaciones que la aplicación ofrece a los adaptadores de entrada (ej. controladores REST).
 * Esta interfaz es parte de la capa de aplicación en la arquitectura hexagonal.
 */
public interface OrderServicePort {

    /**
     * Crea un nuevo pedido a partir de una solicitud de pedido.
     * Incluye la lógica de reserva de stock y publicación de eventos.
     *
     * @param orderRequest DTO con los detalles del pedido a crear.
     * @return OrderResponse DTO con los detalles del pedido creado.
     */
    OrderResponse createOrder(OrderRequest orderRequest);

    /**
     * Obtiene los detalles de un pedido específico por su ID.
     *
     * @param id El ID del pedido.
     * @return Un Optional que contiene el OrderResponse si se encuentra el pedido, o vacío si no.
     */
    Optional<OrderResponse> getOrderById(Long id);

    /**
     * Obtiene una lista de todos los pedidos asociados a un cliente específico.
     *
     * @param customerId El ID del cliente.
     * @return Una lista de OrderResponse del cliente.
     */
    List<OrderResponse> getOrdersByCustomerId(Long customerId);

    /**
     * Busca pedidos basándose en varios criterios de filtrado.
     *
     * @param status      Estado del pedido (ej. "PENDING", "SHIPPED"). Puede ser nulo para no filtrar por estado.
     * @param startDate   Fecha y hora de inicio para el rango de la fecha del pedido. Puede ser nulo.
     * @param endDate     Fecha y hora de fin para el rango de la fecha del pedido. Puede ser nulo.
     * @param customerId  ID del cliente. Puede ser nulo para no filtrar por cliente.
     * @return Una lista de OrderResponse que coinciden con los criterios.
     */
    List<OrderResponse> searchOrders(String status, LocalDateTime startDate, LocalDateTime endDate, Long customerId);

    /**
     * Cancela un pedido existente por su ID.
     * Incluye la lógica para liberar stock y publicar eventos de cancelación.
     *
     * @param id El ID del pedido a cancelar.
     * @throws com.ecommerce.ordermanagement.infrastructure.exception.ResourceNotFoundException si el pedido no se encuentra.
     * @throws com.ecommerce.ordermanagement.infrastructure.exception.InvalidOrderStateException si el pedido no puede ser cancelado en su estado actual.
     */
    void cancelOrder(Long id);

    /**
     * Actualiza los ítems de un pedido existente.
     * Requiere que el pedido esté en un estado que permita la modificación (ej. PENDING).
     * Implica la liberación y re-reserva de stock.
     *
     * @param orderId      El ID del pedido cuyos ítems se van a actualizar.
     * @param itemsRequest Lista de OrderItemRequest con los nuevos ítems del pedido.
     * @return OrderResponse del pedido actualizado.
     * @throws com.ecommerce.ordermanagement.infrastructure.exception.ResourceNotFoundException si el pedido no se encuentra.
     * @throws com.ecommerce.ordermanagement.infrastructure.exception.InvalidOrderStateException si el pedido no puede ser modificado o hay stock insuficiente.
     */
    OrderResponse updateOrderItems(Long orderId, List<OrderRequest.OrderItemRequest> itemsRequest);

    /**
     * Procesa la confirmación de un pago, típicamente recibida de un evento de Kafka.
     * Actualiza el estado de pago del pedido y el estado general del pedido.
     *
     * @param orderId             El ID del pedido relacionado con el pago.
     * @param transactionReference La referencia de la transacción de pago.
     * @param status              El estado del pago (ej. "SUCCESS", "FAILED").
     * @throws com.ecommerce.ordermanagement.infrastructure.exception.ResourceNotFoundException si el pedido no se encuentra.
     */
    void processPaymentConfirmation(Long orderId, String transactionReference, String status);

    /**
     * Actualiza el estado de un pedido, típicamente recibida de un evento de Kafka de otro microservicio.
     *
     * @param orderId   El ID del pedido a actualizar.
     * @param newStatus El nuevo estado del pedido como String (ej. "SHIPPED", "DELIVERED").
     * @throws com.ecommerce.ordermanagement.infrastructure.exception.ResourceNotFoundException si el pedido no se encuentra.
     * @throws com.ecommerce.ordermanagement.infrastructure.exception.InvalidOrderStateException si el estado proporcionado no es válido.
     */
    void updateOrderStatusFromEvent(Long orderId, String newStatus);
}
