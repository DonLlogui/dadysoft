package application.port.out;



import java.util.List;
import java.util.Map;

/**
 * Puerto de salida para interactuar con el servicio o módulo de inventario.
 * Define la interfaz que la lógica de negocio de pedidos necesita para gestionar el stock.
 * Esta interfaz es parte de la capa de aplicación en la arquitectura hexagonal.
 * La implementación concreta de este puerto (ej. un adaptador REST a otro microservicio)
 * estará en la capa de infraestructura.
 */
public interface InventoryServicePort {

    /**
     * Verifica la disponibilidad de una lista de productos en el inventario.
     *
     * @param productQuantities Un mapa donde la clave es el ID del producto y el valor es la cantidad deseada.
     * @return true si todos los productos están disponibles en las cantidades especificadas, false en caso contrario.
     */
    boolean checkAvailability(Map<Long, Integer> productQuantities);

    /**
     * Reserva una cantidad de productos en el inventario.
     * Esta operación debería ser idempotente y, en un sistema real, idealmente transaccional con el pedido.
     *
     * @param productQuantities Un mapa donde la clave es el ID del producto y el valor es la cantidad a reservar.
     * @return true si la reserva fue exitosa para todos los productos, false en caso de stock insuficiente o error.
     */
    boolean reserveStock(Map<Long, Integer> productQuantities);

    /**
     * Libera una cantidad de productos del inventario (por ejemplo, cuando un pedido es cancelado).
     *
     * @param productQuantities Un mapa donde la clave es el ID del producto y el valor es la cantidad a liberar.
     * @return true si la liberación fue exitosa para todos los productos, false en caso de error.
     */
    boolean releaseStock(Map<Long, Integer> productQuantities);

    /**
     * Confirma el uso del stock después de que un pedido ha sido exitosamente procesado o enviado.
     * Esto puede implicar una deducción final del inventario o un cambio de estado del stock reservado.
     *
     * @param productQuantities Un mapa donde la clave es el ID del producto y el valor es la cantidad a confirmar.
     * @return true si la confirmación fue exitosa para todos los productos, false en caso de error.
     */
    boolean confirmStockUsage(Map<Long, Integer> productQuantities);
}