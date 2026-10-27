package com.sistemadeoperaciones.socioscomerciales.service;

import com.sistemadeoperaciones.comisionessocioscomerciales.repository.CommercialPartnerCommissionRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
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
import java.math.BigDecimal;

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
    private final BusinessAuditService businessAuditService;

    public CommercialPartnerServiceImpl(
            CommercialPartnerRepository commercialPartnerRepository,
            AuthenticatedUserService authenticatedUserService,
            PaymentOperationRepository paymentOperationRepository,
            CommercialPartnerCommissionRepository commercialPartnerCommissionRepository,
            DeletionAuditService deletionAuditService,
            BusinessAuditService businessAuditService
    ) {
        this.commercialPartnerRepository = commercialPartnerRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.paymentOperationRepository = paymentOperationRepository;
        this.commercialPartnerCommissionRepository = commercialPartnerCommissionRepository;
        this.deletionAuditService = deletionAuditService;
        this.businessAuditService = businessAuditService;
    }

    @Override
    @Transactional
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
        partner.setPorcentajeComision(BigDecimal.ZERO);
        partner.setCommissionConfigured(false);
        partner.setActivo(
                request.getActivo() != null
                        ? request.getActivo()
                        : true
        );

        CommercialPartner saved =
                commercialPartnerRepository.save(partner);
        audit("COMMERCIAL_PARTNER_CREATE", saved, null, auditSnapshot(saved));

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

        return commercialPartnerRepository
                .findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public CommercialPartnerResponseDto findById(Long id) {
        // Reading a secondary partner is shared across level-1 socios.
        // Mutations still call getPartnerAndValidateAccess to enforce ownership.
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comercial no encontrado con id: " + id));
        return mapToResponse(partner);
    }

    @Override
    @Transactional
    public CommercialPartnerResponseDto update(
            Long id,
            CommercialPartnerRequestDTO request
    ) {

        CommercialPartner partner =
                getPartnerAndValidateAccess(id);
        Map<String, Object> beforeAudit = auditSnapshot(partner);

        partner.setNombre(request.getNombre());
        partner.setCuentaBancaria(request.getCuentaBancaria());
        partner.setBanco(request.getBanco());
        partner.setTitularCuenta(request.getTitularCuenta());
        // Profile edits never change commission; only the dedicated manager action can do so.
        if (request.getActivo() != null) {
            partner.setActivo(request.getActivo());
        }

        CommercialPartner updated =
                commercialPartnerRepository.save(partner);
        audit("COMMERCIAL_PARTNER_UPDATE", updated, beforeAudit, auditSnapshot(updated));

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CommercialPartnerResponseDto configureCommission(Long id, BigDecimal percentage) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comercial no encontrado"));
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0)
            throw new BadRequestException("La comisión debe estar entre 0 y 100 %");
        Map<String, Object> before = auditSnapshot(partner);
        partner.setPorcentajeComision(percentage);
        partner.setCommissionConfigured(true);
        CommercialPartner updated = commercialPartnerRepository.save(partner);
        audit("COMMERCIAL_PARTNER_COMMISSION_CONFIGURE", updated, before, auditSnapshot(updated));
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CommercialPartnerResponseDto activate(Long id) {

        CommercialPartner partner =
                getPartnerAndValidateAccess(id);

        if (partner.getActivo()) {
            throw new BadRequestException(
                    "El socio comercial ya se encuentra activo"
            );
        }

        Map<String, Object> beforeAudit = auditSnapshot(partner);
        partner.setActivo(true);

        CommercialPartner updated =
                commercialPartnerRepository.save(partner);
        audit("COMMERCIAL_PARTNER_ACTIVATE", updated, beforeAudit, auditSnapshot(updated));

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CommercialPartnerResponseDto deactivate(Long id) {

        CommercialPartner partner =
                getPartnerAndValidateAccess(id);

        if (!partner.getActivo()) {
            throw new BadRequestException(
                    "El socio comercial ya se encuentra inactivo"
            );
        }

        Map<String, Object> beforeAudit = auditSnapshot(partner);
        partner.setActivo(false);

        CommercialPartner updated =
                commercialPartnerRepository.save(partner);
        audit("COMMERCIAL_PARTNER_DEACTIVATE", updated, beforeAudit, auditSnapshot(updated));

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Socio comercial no encontrado con id: " + id
                ));
        Map<String, Object> beforeAudit = auditSnapshot(partner);

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
            audit("COMMERCIAL_PARTNER_DELETE", partner, beforeAudit, null);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("No se puede eliminar el socio comercial porque tiene información relacionada");
        }
    }

    private Map<String, Object> auditSnapshot(CommercialPartner partner) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", partner.getNombre());
        snapshot.put("ownerUserId", partner.getSocioComercial() == null ? null : partner.getSocioComercial().getId());
        snapshot.put("bank", partner.getBanco());
        String account = partner.getCuentaBancaria();
        snapshot.put("accountLast4", account == null ? null : account.substring(Math.max(0, account.length() - 4)));
        snapshot.put("commissionPercentage", partner.getPorcentajeComision());
        snapshot.put("commissionConfigured", partner.getCommissionConfigured());
        snapshot.put("active", partner.getActivo());
        return snapshot;
    }

    private void audit(String action, CommercialPartner partner, Map<String, Object> before, Map<String, Object> after) {
        businessAuditService.record(authenticatedUserService.getCurrentUser().getId(), action,
                "COMMERCIAL_PARTNER", partner.getId(), before, after);
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

        boolean canViewAll =
                currentUser.getRoles().stream()
                        .anyMatch(role ->
                                role.getName() == RoleName.ADMIN
                                        || role.getName() == RoleName.GERENTE
                                        || role.getName() == RoleName.DIRECCION
                        );

        if (!canViewAll) {

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

        CommercialPartnerResponseDto response = new CommercialPartnerResponseDto(
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
                        : null,
                partner.getPorcentajeComision()
        );
        response.setCommissionConfigured(partner.getCommissionConfigured());
        return response;
    }
}
