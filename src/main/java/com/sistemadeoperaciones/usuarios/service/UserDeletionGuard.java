package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.clientes.repository.ClientesRepository;
import com.sistemadeoperaciones.comisionessocioscomerciales.repository.CommercialPartnerCommissionRepository;
import com.sistemadeoperaciones.corte.repository.DailyCashCutRepository;
import com.sistemadeoperaciones.notifications.repository.NotificationRepository;
import com.sistemadeoperaciones.notifications.repository.UserNotificationRepository;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.EntityHasDependenciesException;
import com.sistemadeoperaciones.socioscomerciales.repository.CommercialPartnerRepository;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.CommercialPartnerSettingsRepository;
import com.sistemadeoperaciones.usuarios.repository.UserActivationTokenRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class UserDeletionGuard {

    private final UserRepository userRepository;
    private final CommercialPartnerSettingsRepository commercialPartnerSettingsRepository;
    private final UserActivationTokenRepository userActivationTokenRepository;
    private final CommercialPartnerRepository commercialPartnerRepository;
    private final ClientesRepository clientesRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final PaymentOperationRepository paymentOperationRepository;
    private final OperationPaymentRepository operationPaymentRepository;
    private final OperationReturnPaymentRepository operationReturnPaymentRepository;
    private final DailyCashCutRepository dailyCashCutRepository;
    private final CommercialPartnerCommissionRepository commercialPartnerCommissionRepository;

    public UserDeletionGuard(
            UserRepository userRepository,
            CommercialPartnerSettingsRepository commercialPartnerSettingsRepository,
            UserActivationTokenRepository userActivationTokenRepository,
            CommercialPartnerRepository commercialPartnerRepository,
            ClientesRepository clientesRepository,
            NotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository,
            PaymentOperationRepository paymentOperationRepository,
            OperationPaymentRepository operationPaymentRepository,
            OperationReturnPaymentRepository operationReturnPaymentRepository,
            DailyCashCutRepository dailyCashCutRepository,
            CommercialPartnerCommissionRepository commercialPartnerCommissionRepository
    ) {
        this.userRepository = userRepository;
        this.commercialPartnerSettingsRepository = commercialPartnerSettingsRepository;
        this.userActivationTokenRepository = userActivationTokenRepository;
        this.commercialPartnerRepository = commercialPartnerRepository;
        this.clientesRepository = clientesRepository;
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.paymentOperationRepository = paymentOperationRepository;
        this.operationPaymentRepository = operationPaymentRepository;
        this.operationReturnPaymentRepository = operationReturnPaymentRepository;
        this.dailyCashCutRepository = dailyCashCutRepository;
        this.commercialPartnerCommissionRepository = commercialPartnerCommissionRepository;
    }

    public void assertCanDelete(User currentUser, User target) {
        if (currentUser.getId().equals(target.getId())) {
            throw new BadRequestException("No puede eliminar su propio usuario");
        }

        boolean isAdmin = target.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean isDireccion = target.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.DIRECCION);

        if (isAdmin && Boolean.TRUE.equals(target.getActivo())
                && userRepository.countByRoles_NameAndActivoTrue(RoleName.ADMIN) <= 1) {
            throw new BadRequestException("No se puede eliminar al último administrador activo del sistema");
        }

        if (isDireccion && Boolean.TRUE.equals(target.getActivo())
                && userRepository.countByRoles_NameAndActivoTrue(RoleName.DIRECCION) <= 1) {
            throw new BadRequestException("No se puede eliminar al último usuario con rol Dirección activo del sistema");
        }

        Long id = target.getId();
        Map<String, Long> dependencies = new LinkedHashMap<>();

        if (commercialPartnerSettingsRepository.existsByUsuarioId(id)) {
            dependencies.put("configuracionSocioComercial", 1L);
        }
        putIfPositive(dependencies, "tokensActivacion", userActivationTokenRepository.countByUserId(id));
        putIfPositive(dependencies, "sociosComercialesPropios", commercialPartnerRepository.countBySocioComercialId(id));
        putIfPositive(dependencies, "clientesAsignados", clientesRepository.countByUserId(id));
        putIfPositive(dependencies, "notificacionesCreadas", notificationRepository.countByCreatedById(id));
        putIfPositive(dependencies, "notificacionesRecibidas", userNotificationRepository.countByUsuarioId(id));
        putIfPositive(dependencies, "operacionesComoSocioComercial", paymentOperationRepository.countBySocioComercialId(id));
        putIfPositive(dependencies, "pagosRegistrados", operationPaymentRepository.countByRegistradoPorId(id));
        putIfPositive(dependencies, "pagosValidados", operationPaymentRepository.countByValidadoPorId(id));
        putIfPositive(dependencies, "retornosSolicitados", operationReturnPaymentRepository.countBySolicitadoPorId(id));
        putIfPositive(dependencies, "retornosPagados", operationReturnPaymentRepository.countByPagadoPorId(id));
        putIfPositive(dependencies, "cortesDiariosGenerados", dailyCashCutRepository.countByGeneradoPorId(id));
        putIfPositive(dependencies, "comisionesComoBeneficiario", commercialPartnerCommissionRepository.countByUserId(id));

        if (!dependencies.isEmpty()) {
            throw new EntityHasDependenciesException(
                    "No se puede eliminar el usuario porque tiene información relacionada en el sistema",
                    dependencies
            );
        }
    }

    private void putIfPositive(Map<String, Long> map, String key, long value) {
        if (value > 0) {
            map.put(key, value);
        }
    }
}
