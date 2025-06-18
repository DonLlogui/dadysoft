package application.port.out;

import com.comercio.dadysoft.domain.model.Order; // Importa el modelo de dominio Order
import com.comercio.dadysoft.domain.model.OrderStatus; // Importa el enum OrderStatus

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia de pedidos.
 * Define las operaciones que la aplicación necesita para interactuar con la base de datos de pedidos.
 * Esta interfaz es parte de la capa de aplicación en la arquitectura hexagonal.
 * La implementación concreta de este puerto estará en la capa de infraestructura.
 */
public interface OrderRepositoryPort {

    /**
     * Guarda un nuevo pedido o actualiza uno existente.
     *
     * @param order El objeto de dominio Order a guardar.
     * @return El objeto Order guardado (con ID si es nuevo).
     */
    Order save(Order order);

    /**
     * Busca un pedido por su ID.
     *
     * @param id El ID del pedido.
     * @return Un Optional que contiene el Order si se encuentra, o vacío si no.
     */
    Optional<Order> findById(Long id);

    /**
     * Busca todos los pedidos.
     *
     * @return Una lista de todos los pedidos.
     */
    List<Order> findAll();

    /**
     * Busca pedidos por el ID de un cliente.
     *
     * @param customerId El ID del cliente.
     * @return Una lista de pedidos asociados a ese cliente.
     */
    List<Order> findByCustomerId(Long customerId);

    /**
     * Busca pedidos que coincidan con un estado y/o un rango de fechas y/o un ID de cliente.
     *
     * @param status     El estado del pedido (ej. PENDING, SHIPPED). Puede ser null.
     * @param startDate  La fecha de inicio del rango de fechas del pedido. Puede ser null.
     * @param endDate    La fecha de fin del rango de fechas del pedido. Puede ser null.
     * @param customerId El ID del cliente. Puede ser null.
     * @return Una lista de pedidos que cumplen con los criterios de búsqueda.
     */
    List<Order> findByCriteria(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, Long customerId);

    /**
     * Elimina un pedido por su ID.
     *
     * @param id El ID del pedido a eliminar.
     */
    void deleteById(Long id);
}
