package com.comercio.dadysoft.domain;

import jakarta.persistence.*; // Importa todas las anotaciones de JPA
import lombok.AllArgsConstructor; // Genera un constructor con todos los argumentos
import lombok.Data; // Genera getters, setters, equals, hashCode y toString
import lombok.NoArgsConstructor; // Genera un constructor sin argumentos

import java.time.LocalDateTime;

@Entity // Marca esta clase como una entidad JPA
@Table(name = "eventos_pedido") // Especifica el nombre de la tabla en la base de datos
@Data // Anotación de Lombok para generar boilerplate code
@NoArgsConstructor // Anotación de Lombok para constructor sin argumentos
@AllArgsConstructor // Anotación de Lombok para constructor con todos los argumentos
public class EventoPedido {

    @Id // Marca el campo como la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura la generación automática de IDs (autoincremento)
    private Long idEvento;

    // Relación Many-to-One con Pedido.
    // Muchos eventos de pedido pueden pertenecer a un solo pedido.
    // @JoinColumn: Especifica la columna de unión en la tabla 'eventos_pedido' que referencia a 'pedidos'.
    @ManyToOne(fetch = FetchType.LAZY) // Lazy loading para cargar el pedido solo cuando sea necesario
    @JoinColumn(name = "id_pedido", nullable = false) // Columna 'id_pedido' en la tabla 'eventos_pedido'
    private Pedido pedido;

    // Fecha y hora en que ocurrió el evento
    private LocalDateTime fechaEvento;

    // Tipo de evento (ej. "CREADO", "PAGADO", "ENVIADO", "CANCELADO", "ESTADO_ACTUALIZADO")
    private String tipoEvento;

    // Descripción adicional del evento
    @Column(length = 500) // Limita la longitud de la descripción
    private String descripcion;

    // --- Métodos adicionales, si son necesarios ---

    /**
     * Constructor conveniente para crear un nuevo evento.
     * @param pedido El pedido al que pertenece el evento.
     * @param tipoEvento El tipo de evento (ej. "CREADO", "PAGADO").
     * @param descripcion Una descripción del evento.
     */
    public EventoPedido(Pedido pedido, String tipoEvento, String descripcion) {
        this.pedido = pedido;
        this.fechaEvento = LocalDateTime.now(); // Se establece automáticamente al crear
        this.tipoEvento = tipoEvento;
        this.descripcion = descripcion;
    }
}