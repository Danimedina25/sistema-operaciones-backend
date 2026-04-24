package com.sistemadeoperaciones.clientes.repository;

import com.sistemadeoperaciones.clientes.model.Clientes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientesRepository extends JpaRepository<Clientes, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Clientes> findByActivoTrueOrderByNombreAsc();

    List<Clientes> findAllByOrderByCreatedAtDesc();
}