package com.sistemadeoperaciones.socioscomerciales.service;

import com.sistemadeoperaciones.comisionessocioscomerciales.repository.CommercialPartnerCommissionRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.EntityHasDependenciesException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerRequestDTO;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerResponseDto;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.socioscomerciales.repository.CommercialPartnerRepository;
import com.sistemadeoperaciones.socioscomerciales.specification.CommercialPartnerSpecification;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommercialPartnerServiceImpl implements CommercialPartnerService {

    private static final List<String> ALLOWED_SORTS = List.of(
            "id",
            "nombre",
            "cuentaBancaria",
            "banco",
            "createdAt"
    );

    private final CommercialPartnerRepository commercialPartnerRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final PaymentOperationRepository paymentOperationRepository;
    private final CommercialPartnerCommissionRepository commercialPartnerCommissionRepository;
    private final DeletionAuditService deletionAuditService;

    public CommercialPartnerServiceImpl(
            CommercialPartnerRepository commercialPartnerRepository,
            AuthenticatedUserService authenticatedUserService,
            PaymentOperationRepository paymentOperationRepository,
            CommercialPartnerCommissionRepository commercialPartnerCommissionRepository,
            DeletionAuditService deletionAuditService
    ) {
        this.commercialPartnerRepository = commercialPartnerRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.paymentOperationRepository = paymentOperationRepository;
        this.commercialPartnerCommissionRepository = commercialPartnerCommissionRepository;
        this.deletionAuditService = deletionAuditService;
    }

    @Override
    public CommercialPartnerResponseDto create(
            CommercialPartnerRequestDTO request
    ) {

        User socioComercial =
                authenticatedUserService.getCurrentUser();

        CommercialPartner partner = new CommercialPartner();

        partner.setNombre(request.getNombre());
        partner.setCuentaBancaria(request.getCuentaBancaria());
        partner.setBanco(request.getBanco());
        partner.setTitularCuenta(request.getTitularCuenta());
        partner.setSocioComercial(socioComercial);
        partner.setActivo(
                request.getActivo() != null
                        ? request.getActivo()
                        : true
        );

        CommercialPartner saved =
                commercialPartnerRepository.save(partner);

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
            String cuentaBancaria
    ) {

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "id";
        }

        if (!ALLOWED_SORTS.contains(sortBy)) {
            sortBy = "id";
        }

        if (sortDir == null || sortDir.isBlank()) {
            sortDir = "asc";
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        User currentUser =
                authenticatedUserService.getCurrentUser();

        boolean isAdminOrManager =
                currentUser.getRoles().stream()
                        .anyMatch(role ->
                                role.getName() == RoleName.ADMIN
                                        || role.getName() == RoleName.GERENTE
                        );

        Specification<CommercialPartner> specification =
                Specification.where(
                                CommercialPartnerSpecification.hasActivo(activo)
                        )
                        .and(
                                CommercialPartnerSpecification.hasNombre(nombre)
                        )
                        .and(
                                CommercialPartnerSpecification.hasCuentaBancaria(cuentaBancaria)
                        );

        if (!isAdminOrManager) {
            specification = specification.and(
                    CommercialPartnerSpecification.hasSocioComercialId(
                            currentUser.getId()
                    )
            );
        }

        return commercialPartnerRepository
                .findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public CommercialPartnerResponseDto findById(Long id) {

        CommercialPartner partner =
                getPartnerAndValidateAccess(id);

        return mapToResponse(partner);
    }

    @Override
    public CommercialPartnerResponseDto update(
            Long id,
            CommercialPartnerRequestDTO request
    ) {

        CommercialPartner partner =
                getPartnerAndValidateAccess(id);

        partner.setNombre(request.getNombre());
        partner.setCuentaBancaria(request.getCuentaBancaria());
        partner.setBanco(request.getBanco());
        partner.setTitularCuenta(request.getTitularCuenta());
        if (request.getActivo() != null) {
            partner.setActivo(request.getActivo());
        }

        CommercialPartner updated =
                commercialPartnerRepository.save(partner);

        return mapToResponse(updated);
    }

    @Override
    public CommercialPartnerResponseDto activate(Long id) {

        CommercialPartner partner =
                getPartnerAndValidateAccess(id);

        if (partner.getActivo()) {
            throw new BadRequestException(
                    "El socio comercial ya se encuentra activo"
            );
        }

        partner.setActivo(true);

        CommercialPartner updated =
                commercialPartnerRepository.save(partner);

        return mapToResponse(updated);
    }

    @Override
    public CommercialPartnerResponseDto deactivate(Long id) {

        CommercialPartner partner =
                getPartnerAndValidateAccess(id);

        if (!partner.getActivo()) {
            throw new BadRequestException(
                    "El socio comercial ya se encuentra inactivo"
            );
        }

        partner.setActivo(false);

        CommercialPartner updated =
                commercialPartnerRepository.save(partner);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Socio comercial no encontrado con id: " + id
                ));

        // A diferencia de deactivate/update, no se valida ownership aquí:
        // solo ADMIN/DIRECCION llegan a este método (ver @PreAuthorize del controller).
        Map<String, Long> dependencies = new LinkedHashMap<>();

        long nivel2 = paymentOperationRepository.countBySocioComercialNivel2Id(id);
        if (nivel2 > 0) dependencies.put("operacionesNivel2", nivel2);

        long nivel3 = paymentOperationRepository.countBySocioComercialNivel3Id(id);
        if (nivel3 > 0) dependencies.put("operacionesNivel3", nivel3);

        long comisiones = commercialPartnerCommissionRepository.countByCommercialPartnerId(id);
        if (comisiones > 0) dependencies.put("comisiones", comisiones);

        if (!dependencies.isEmpty()) {
            throw new EntityHasDependenciesException(
                    "No se puede eliminar el socio comercial porque tiene operaciones o comisiones asociadas",
                    dependencies
            );
        }

        User currentUser = authenticatedUserService.getCurrentUser();
        deletionAuditService.record("COMMERCIAL_PARTNER", partner.getId(), partner.getNombre(), currentUser);

        try {
            commercialPartnerRepository.delete(partner);
            commercialPartnerRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("No se puede eliminar el socio comercial porque tiene información relacionada");
        }
    }

    private CommercialPartner getPartnerAndValidateAccess(
            Long id
    ) {

        CommercialPartner partner =
                commercialPartnerRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Socio comercial no encontrado con id: " + id
                                )
                        );

        User currentUser =
                authenticatedUserService.getCurrentUser();

        boolean isAdminOrManager =
                currentUser.getRoles().stream()
                        .anyMatch(role ->
                                role.getName() == RoleName.ADMIN
                                        || role.getName() == RoleName.GERENTE
                        );

        if (!isAdminOrManager) {

            Long ownerId =
                    partner.getSocioComercial() != null
                            ? partner.getSocioComercial().getId()
                            : null;

            if (!currentUser.getId().equals(ownerId)) {
                throw new BadRequestException(
                        "No tiene permisos para acceder a este socio comercial"
                );
            }
        }

        return partner;
    }

    private CommercialPartnerResponseDto mapToResponse(
            CommercialPartner partner
    ) {

        return new CommercialPartnerResponseDto(
                partner.getId(),
                partner.getNombre(),
                partner.getCuentaBancaria(),
                partner.getBanco(),
                partner.getTitularCuenta(),
                partner.getActivo(),
                partner.getSocioComercial() != null
                        ? partner.getSocioComercial().getId()
                        : null,
                partner.getSocioComercial() != null
                        ? partner.getSocioComercial().getNombre()
                        : null
        );
    }
}