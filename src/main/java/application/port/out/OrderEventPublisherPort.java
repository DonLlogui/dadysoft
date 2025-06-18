package application.port.out;

import com.comercio.dadysoft.shared.dto.OrderEvent; // Importa el DTO del evento de pedido

/**
 * Puerto de salida para la publicación de eventos de pedidos.
 * Define las operaciones que la aplicación necesita para enviar eventos
 * relacionados con los pedidos a un sistema de mensajería (ej. Kafka).
 * Esta interfaz es parte de la capa de aplicación en la arquitectura hexagonal.
 * La implementación concreta (ej. un productor Kafka) estará en la capa de infraestructura.
 */
public interface OrderEventPublisherPort {

    /**
     * Publica un evento de pedido en un tema de mensajería específico.
     * Este método se utiliza para notificar a otros servicios o componentes
     * sobre cambios importantes en el ciclo de vida de un pedido.
     *
     * @param event El objeto OrderEvent que contiene los detalles del evento a publicar.
     */
    void publishOrderEvent(OrderEvent event);

    /**
     * Publica un evento de cancelación de pedido.
     *
     * @param orderId El ID del pedido que fue cancelado.
     * @param reason La razón de la cancelación.
     */
    void publishOrderCancelledEvent(Long orderId, String reason);

    /**
     * Publica un evento de actualización de estado de pedido.
     *
     * @param orderId El ID del pedido cuyo estado ha cambiado.
     * @param newStatus El nuevo estado del pedido.
     */
    void publishOrderStatusUpdatedEvent(Long orderId, String newStatus);

    /**
     * Publica un evento de pago exitoso de un pedido.
     *
     * @param orderId El ID del pedido.
     * @param transactionReference La referencia de la transacción de pago.
     * @param amount El monto del pago.
     */
    void publishOrderPaymentSuccessEvent(Long orderId, String transactionReference, Double amount);

    /**
     * Publica un evento de fallo de pago de un pedido.
     *
     * @param orderId El ID del pedido.
     * @param transactionReference La referencia de la transacción de pago.
     * @param reason La razón del fallo.
     */
    void publishOrderPaymentFailedEvent(Long orderId, String transactionReference, String reason);
}