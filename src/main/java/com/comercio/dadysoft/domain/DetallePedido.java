package com.comercio.dadysoft.domain; // Asegúrate de que este sea el paquete correcto

import jakarta.persistence.*; // Importa todas las anotaciones de JPA
import lombok.AllArgsConstructor; // Genera un constructor con todos los argumentos
import lombok.Data; // Genera getters, setters, equals, hashCode y toString
import lombok.NoArgsConstructor; // Genera un constructor sin argumentos

import java.math.BigDecimal;

@Entity // Marca esta clase como una entidad JPA
@Table(name = "detalle_pedido") // Especifica el nombre de la tabla en la base de datos
@Data // Anotación de Lombok para generar boilerplate code
@NoArgsConstructor // Anotación de Lombok para constructor sin argumentos
@AllArgsConstructor // Anotación de Lombok para constructor con todos los argumentos
public class DetallePedido {

    @Id // Marca el campo como la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura la generación automática de IDs (autoincremento)
    private Long idDetalle;

    // Relación Many-to-One con Pedido.
    // Muchos detalles de pedido pueden pertenecer a un solo pedido.
    // @JoinColumn: Especifica la columna de unión en la tabla 'detalle_pedido' que referencia a 'pedidos'.
    @ManyToOne(fetch = FetchType.LAZY) // Lazy loading para cargar el pedido solo cuando sea necesario
    @JoinColumn(name = "id_pedido", nullable = false) // Columna 'id_pedido' en la tabla 'detalle_pedido'
    private Pedido pedido;

    // Aquí ya no hay un objeto Producto completo. Guardamos solo el ID del producto
    // porque la información detallada del producto reside en el Product Service.
    private Long idProducto;

    // Cantidad del producto en este detalle del pedido
    private int cantidad;

    // Precio unitario del producto en el momento de la compra.
    // Es crucial almacenar el precio unitario aquí para un registro histórico,
    // ya que el precio del producto podría cambiar en el Product Service con el tiempo.
    private BigDecimal precioUnitario;

    // --- Métodos adicionales, si son necesarios ---

    /**
     * Calcula el subtotal para este detalle de pedido.
     * @return El subtotal (precioUnitario * cantidad).
     */
    public BigDecimal getSubtotal() {
        if (precioUnitario == null || cantidad < 0) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(new BigDecimal(cantidad));
    }
}