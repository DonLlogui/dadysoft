package infrastructure.adapter.out.persistence.repository;

import com.comercio.dadysoft.infrastructure.adapter.out.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz de repositorio JPA para la entidad OrderEntity.
 * Extiende JpaRepository de Spring Data JPA para proporcionar operaciones CRUD básicas
 * y la capacidad de definir consultas personalizadas.
 *
 * Esta es una parte del adaptador de persistencia en la capa de infraestructura.
 * No debe ser accedida directamente por la capa de aplicación, sino a través de OrderRepositoryAdapter.
 */
@Repository // Anotación para indicar que es un componente de repositorio de Spring
public interface JpaOrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * Busca una lista de pedidos por el ID del cliente.
     * Spring Data JPA infiere automáticamente la consulta de este nombre de método.
     *
     * @param customerId El ID del cliente.
     * @return Una lista de OrderEntity asociadas a ese cliente.
     */
    List<OrderEntity> findByCustomerId(Long customerId);

    // Puedes añadir otros métodos de consulta personalizados aquí si fueran necesarios para JpaOrderRepository,
    // por ejemplo:
    // List<OrderEntity> findByStatus(String status);
    // List<OrderEntity> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    // Optional<OrderEntity> findByTransactionReference(String transactionReference);
}