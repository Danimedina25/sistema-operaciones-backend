package com.sistemadeoperaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SistemaDeOperacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                SistemaDeOperacionesApplication.class,
                args
        );
    }
}