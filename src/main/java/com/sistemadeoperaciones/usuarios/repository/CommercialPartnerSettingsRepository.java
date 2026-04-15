package com.sistemadeoperaciones.usuarios.repository;

import com.sistemadeoperaciones.usuarios.model.CommercialPartnerSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommercialPartnerSettingsRepository extends JpaRepository<CommercialPartnerSettings, Long> {

    boolean existsByUsuarioId(Long userId);

    Optional<CommercialPartnerSettings> findByUsuarioId(Long userId);
}