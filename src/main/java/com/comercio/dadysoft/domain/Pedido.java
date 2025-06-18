//package domain;
package com.comercio.dadysoft.domain;
import jakarta.persistence.*; // Importa todas las anotaciones de JPA
import lombok.AllArgsConstructor; // Genera un constructor con todos los argumentos
import lombok.Data; // Genera getters, setters, equals, hashCode y toString
import lombok.NoArgsConstructor; // Genera un constructor sin argumentos

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity // Marca esta clase como una entidad JPA
@Table(name = "pedidos") // Especifica el nombre de la tabla en la base de datos
@Data // Anotación de Lombok para generar boilerplate code
@NoArgsConstructor // Anotación de Lombok para constructor sin argumentos
@AllArgsConstructor // Anotación de Lombok para constructor con todos los argumentos
public class Pedido {

    @Id // Marca el campo como la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura la generación automática de IDs (autoincremento)
    private Long idPedido;

    // Aquí ya no hay un objeto Cliente completo. Guardamos solo el ID del cliente
    // porque la información detallada del cliente reside en el Customer Service.
    private Long idCliente;

    // Fecha y hora en que se realizó el pedido
    private LocalDateTime fecha;

    // El estado del pedido, mapeado como String en la base de datos para legibilidad
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    // El total del pedido, usando BigDecimal para precisión con moneda
    private BigDecimal total;


    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles;

    // Relación uno a muchos con EventoPedido (para registrar cambios de estado, etc.)
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoPedido> eventos;

    /**
     * Enumeración para los posibles estados de un pedido.
     */
    public enum EstadoPedido {
        PENDIENTE,        // Recibido, esperando confirmación/pago
        CONFIRMADO,       // Pago confirmado, en proceso de preparación
        EN_PROCESO,       // Preparando el pedido
        ENVIADO,          // El pedido ha sido enviado al cliente
        ENTREGADO,        // El cliente ha recibido el pedido
        CANCELADO,        // El pedido ha sido cancelado
        PAGADO,           // El pago ha sido procesado (puede estar en cualquier estado posterior a PENDIENTE)
        REEMBOLSADO       // El pago ha sido reembolsado (puede aplicar a pedidos cancelados o devueltos)
    }

    // --- Métodos adicionales de dominio (opcionales, puedes moverlos a servicios si lo prefieres) ---
    // Aunque en hexagonal, la lógica de negocio debe estar en el Core (application.service),
    // algunas lógicas pequeñas relacionadas con el estado de la propia entidad pueden estar aquí.

    public void addDetalle(DetallePedido detalle) {
        this.detalles.add(detalle);
        detalle.setPedido(this);
    }

    public void removeDetalle(DetallePedido detalle) {
        this.detalles.remove(detalle);
        detalle.setPedido(null);
    }

    public void addEvento(EventoPedido evento) {
        this.eventos.add(evento);
        evento.setPedido(this);
    }

    /**
     * Verifica si el pedido está en un estado que permite modificaciones.
     * @return true si el pedido se puede modificar, false en caso contrario.
     */
    public boolean isModificable() {
        return this.estado == EstadoPedido.PENDIENTE || this.estado == EstadoPedido.CONFIRMADO;
    }

    /**
     * Actualiza el estado del pedido, con algunas reglas básicas de transición.
     * @param nuevoEstado El nuevo estado del pedido.
     * @throws IllegalStateException Si la transición de estado no es permitida.
     */
    public void cambiarEstado(EstadoPedido nuevoEstado) {
        if (this.estado == EstadoPedido.ENTREGADO && nuevoEstado != EstadoPedido.ENTREGADO) {
            throw new IllegalStateException("No se puede cambiar el estado de un pedido ENTREGADO.");
        }
        if (this.estado == EstadoPedido.CANCELADO && nuevoEstado != EstadoPedido.CANCELADO && nuevoEstado != EstadoPedido.REEMBOLSADO) {
            throw new IllegalStateException("No se puede cambiar el estado de un pedido CANCELADO a menos que sea a REEMBOLSADO.");
        }
        this.estado = nuevoEstado;

    }
}