package com.comercio.dadysoft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Clase principal de la aplicación Spring Boot para el microservicio de gestión de pedidos.
 * Anotaciones:
 * - @SpringBootApplication: Conveniencia que añade @Configuration, @EnableAutoConfiguration y @ComponentScan.
 * - @ComponentScan: Escanea paquetes para componentes Spring. Asegura que Spring encuentre tus beans.
 * Se usa para especificar explícitamente los paquetes a escanear si tus componentes no están directamente
 * debajo del paquete de la clase principal o si necesitas incluir otros módulos.
 * - @EntityScan: Escanea entidades JPA para el mapeo.
 * - @EnableJpaRepositories: Habilita el soporte de repositorios JPA.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.comercio.dadysoft") // Escanea todos los componentes dentro de este paquete base
@EntityScan(basePackages = "com.comercio.dadysoft.infrastructure.adapter.out.persistence.entity") // Escanea tus entidades JPA
@EnableJpaRepositories(basePackages = "com.comercio.dadysoft.infrastructure.adapter.out.persistence.repository") // Habilita los repositorios JPA
public class OrderManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderManagementApplication.class, args);
    }
}