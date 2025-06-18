package com.comercio.dadysoft.domain;

import lombok.AllArgsConstructor; // Genera un constructor con todos los argumentos
import lombok.Data; // Genera getters, setters, equals, hashCode y toString
import lombok.NoArgsConstructor; // Genera un constructor sin argumentos

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para representar la información básica de un producto
 * obtenida del Product Service. Esta clase no es una entidad JPA de este microservicio.
 */
@Data // Anotación de Lombok para generar boilerplate code (getters, setters, etc.)
@NoArgsConstructor // Anotación de Lombok para constructor sin argumentos
@AllArgsConstructor // Anotación de Lombok para constructor con todos los argumentos
public class ProductoInfo {
    private Long id;          // ID del producto en el Product Service
    private String nombre;    // Nombre del producto
    private BigDecimal precio; // Precio actual del producto

}