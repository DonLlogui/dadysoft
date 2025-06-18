package application.port.out;

import com.comercio.dadysoft.domain.model.Order; // Importa el modelo de dominio Order
import com.comercio.dadysoft.shared.dto.PaymentRequest; // Asumiendo que tendrás un DTO para enviar al servicio de pagos
import com.comercio.dadysoft.shared.dto.PaymentResponse; // Asumiendo que tendrás un DTO para la respuesta del servicio de pagos

import java.math.BigDecimal; // Para manejar el monto del pago

/**
 * Puerto de salida para interactuar con el servicio de pagos externo.
 * Define la interfaz que la lógica de negocio necesita para procesar pagos.
 * Esta interfaz es parte de la capa de aplicación en la arquitectura hexagonal.
 * La implementación concreta de este puerto (ej. un adaptador REST o SDK) estará en la capa de infraestructura.
 */
public interface PaymentServicePort {

    /**
     * Simula o inicia el procesamiento de un pago para un pedido dado.
     * En un entorno real, esto interactuaría con una pasarela de pago.
     *
     * @param orderId   El ID del pedido para el cual se procesa el pago.
     * @param amount    El monto total a pagar.
     * @param paymentMethod El método de pago utilizado (ej. "Credit Card", "PayPal").
     * @return PaymentResponse con el estado de la transacción (ej. PENDING, SUCCESS, FAILED) y una referencia.
     */
    PaymentResponse processPayment(Long orderId, BigDecimal amount, String paymentMethod);

    /**
     * Simula o inicia un reembolso para un pago dado.
     *
     * @param orderId   El ID del pedido relacionado con el pago original.
     * @param transactionReference La referencia de la transacción de pago original.
     * @param amount    El monto a reembolsar.
     * @return PaymentResponse con el estado del reembolso (ej. SUCCESS, FAILED).
     */
    PaymentResponse refundPayment(Long orderId, String transactionReference, BigDecimal amount);

    /**
     * Consulta el estado de una transacción de pago existente.
     *
     * @param transactionReference La referencia única de la transacción.
     * @return PaymentResponse con el estado actual de la transacción.
     */
    PaymentResponse getPaymentStatus(String transactionReference);
}
