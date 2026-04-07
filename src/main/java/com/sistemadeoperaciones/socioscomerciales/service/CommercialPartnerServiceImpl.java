package com.sistemadeoperaciones.socioscomerciales.service;

import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerNetworkDto;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerRequestDto;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerResponseDto;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.socioscomerciales.repository.CommercialPartnerRepository;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.sistemadeoperaciones.socioscomerciales.specification.CommercialPartnerSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Service
public class CommercialPartnerServiceImpl implements CommercialPartnerService {

    private final CommercialPartnerRepository commercialPartnerRepository;

    public CommercialPartnerServiceImpl(CommercialPartnerRepository commercialPartnerRepository) {
        this.commercialPartnerRepository = commercialPartnerRepository;
    }

    @Override
    public CommercialPartnerResponseDto create(CommercialPartnerRequestDto request) {
        if (commercialPartnerRepository.existsByCorreo(request.getCorreo())) {
            throw new BadRequestException("Ya existe un socio comercial con ese correo");
        }

        CommercialPartner socioPadre = null;
        if (request.getSocioPadreId() != null) {
            socioPadre = commercialPartnerRepository.findById(request.getSocioPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Socio padre no encontrado con id: " + request.getSocioPadreId()));
        }

        CommercialPartner partner = new CommercialPartner();
        partner.setNombre(request.getNombre());
        partner.setCorreo(request.getCorreo());
        partner.setTelefono(request.getTelefono());
        partner.setActivo(request.getActivo() != null ? request.getActivo() : true);
        partner.setSocioPadre(socioPadre);

        CommercialPartner saved = commercialPartnerRepository.save(partner);
        return mapToResponse(saved);
    }

    @Override
    public Page<CommercialPartnerResponseDto> findAll(
            int page,
            int size,
            String sortBy,
            String sortDir,
            Boolean activo,
            String nombre,
            String correo,
            Long socioPadreId
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<CommercialPartner> specification =
                Specification.where(CommercialPartnerSpecification.hasActivo(activo))
                        .and(CommercialPartnerSpecification.hasNombre(nombre))
                        .and(CommercialPartnerSpecification.hasCorreo(correo))
                        .and(CommercialPartnerSpecification.hasSocioPadreId(socioPadreId));

        return commercialPartnerRepository.findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public CommercialPartnerResponseDto findById(Long id) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comercial no encontrado con id: " + id));

        return mapToResponse(partner);
    }

    @Override
    public CommercialPartnerResponseDto update(Long id, CommercialPartnerRequestDto request) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comercial no encontrado con id: " + id));

        if (commercialPartnerRepository.existsByCorreoAndIdNot(request.getCorreo(), id)) {
            throw new BadRequestException("Ya existe otro socio comercial con ese correo");
        }

        CommercialPartner socioPadre = null;
        if (request.getSocioPadreId() != null) {
            if (request.getSocioPadreId().equals(id)) {
                throw new BadRequestException("Un socio comercial no puede ser su propio padre");
            }

            socioPadre = commercialPartnerRepository.findById(request.getSocioPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Socio padre no encontrado con id: " + request.getSocioPadreId()));

            validateHierarchyCycle(partner, socioPadre);
        }

        partner.setNombre(request.getNombre());
        partner.setCorreo(request.getCorreo());
        partner.setTelefono(request.getTelefono());
        partner.setActivo(request.getActivo() != null ? request.getActivo() : partner.getActivo());
        partner.setSocioPadre(socioPadre);

        CommercialPartner updated = commercialPartnerRepository.save(partner);
        return mapToResponse(updated);
    }

    @Override
    public CommercialPartnerNetworkDto getNetworkById(Long id) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comercial no encontrado con id: " + id));

        return mapToNetwork(partner);
    }

    @Override
    public CommercialPartnerResponseDto deactivate(Long id) {
        CommercialPartner commercialPartner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comerial no encontrado con id: " + id));

        if (!commercialPartner.getActivo()) {
            throw new BadRequestException("El socio comercial ya se encuentra inactivo");
        }

        commercialPartner.setActivo(false);

        CommercialPartner updated = commercialPartnerRepository.save(commercialPartner);
        return mapToResponse(updated);
    }

    @Override
    public CommercialPartnerResponseDto activate(Long id) {
        CommercialPartner commercialPartner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comerial no encontrado con id: " + id));

        if (commercialPartner.getActivo()) {
            throw new BadRequestException("El socio comercial ya se encuentra activo");
        }

        commercialPartner.setActivo(true);

        CommercialPartner updated = commercialPartnerRepository.save(commercialPartner);
        return mapToResponse(updated);
    }

    private CommercialPartnerResponseDto mapToResponse(CommercialPartner partner) {
        Long socioPadreId = null;
        String socioPadreNombre = null;

        if (partner.getSocioPadre() != null) {
            socioPadreId = partner.getSocioPadre().getId();
            socioPadreNombre = partner.getSocioPadre().getNombre();
        }

        return new CommercialPartnerResponseDto(
                partner.getId(),
                partner.getNombre(),
                partner.getCorreo(),
                partner.getTelefono(),
                partner.getActivo(),
                socioPadreId,
                socioPadreNombre
        );
    }

    private CommercialPartnerNetworkDto mapToNetwork(CommercialPartner partner) {
        CommercialPartnerNetworkDto dto = new CommercialPartnerNetworkDto(
                partner.getId(),
                partner.getNombre(),
                partner.getCorreo(),
                partner.getTelefono(),
                partner.getActivo()
        );

        List<CommercialPartnerNetworkDto> hijosDto = partner.getHijos()
                .stream()
                .map(this::mapToNetwork)
                .toList();

        dto.setHijos(hijosDto);
        return dto;
    }

    private void validateHierarchyCycle(CommercialPartner currentPartner, CommercialPartner proposedParent) {
        CommercialPartner current = proposedParent;

        while (current != null) {
            if (current.getId().equals(currentPartner.getId())) {
                throw new BadRequestException("No se puede asignar ese socio padre porque genera un ciclo jerárquico en la red");
            }
            current = current.getSocioPadre();
        }
    }
}