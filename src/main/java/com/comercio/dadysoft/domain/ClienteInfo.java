package com.comercio.dadysoft.domain;

import lombok.AllArgsConstructor; // Genera un constructor con todos los argumentos
import lombok.Data; // Genera getters, setters, equals, hashCode y toString
import lombok.NoArgsConstructor; // Genera un constructor sin argumentos

/**
 * DTO (Data Transfer Object) para representar la información básica de un cliente
 * obtenida del Customer Service. Esta clase no es una entidad JPA de este microservicio.
 */
@Data // Anotación de Lombok para generar boilerplate code (getters, setters, etc.)
@NoArgsConstructor // Anotación de Lombok para constructor sin argumentos
@AllArgsConstructor // Anotación de Lombok para constructor con todos los argumentos
public class ClienteInfo {
    private Long id;          // ID del cliente en el Customer Service
    private String nombre;    // Nombre del cliente
    private String correo;    // Correo electrónico del cliente

}