package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.retornos.CancelReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.CreateReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.DeliverReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnInstallmentResponseDto;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.exceptions.InvalidReturnAmountException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentAmountExceedsAvailableException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentInvalidStatusException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentNoAuthorizedRecipientsException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentNotCancellableException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentPreparedAmountEvidenceRequiredException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentReceiptRequiredException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentReceiverNotAuthorizedException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentReceiverRequiredException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnRequestAlreadyFullyReturnedException;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio de las parcialidades de retorno. Los repositorios se mockean
 * y llevan un estado en memoria (mapa de parcialidades por solicitud) para que
 * las sumas y el recálculo de estatus reflejen lo que va pasando en la prueba.
 */
@ExtendWith(MockitoExtension.class)
class ReturnInstallmentServiceImplTest {

    @Mock PaymentOperationRepository paymentOperationRepository;
    @Mock OperationReturnPaymentRepository returnPaymentRepository;
    @Mock OperationReturnInstallmentRepository installmentRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock AuthenticatedUserService authenticatedUserService;
    @Mock NotificationService notificationService;
    @Mock ReturnPaymentDtoMapper returnPaymentDtoMapper;

    ReturnAmountCalculator returnAmountCalculator = new ReturnAmountCalculator();

    ReturnInstallmentServiceImpl service;

    // Estado en memoria compartido por los mocks de repos
    final Map<Long, OperationReturnPayment> requests = new HashMap<>();
    final Map<Long, List<OperationReturnInstallment>> installmentsByRequest = new HashMap<>();
    final AtomicLong installmentIdSeq = new AtomicLong(1);

    PaymentOperation operation;
    User jefa;

    @BeforeEach
    void setUp() {
        service = new ReturnInstallmentServiceImpl(
                paymentOperationRepository,
                returnPaymentRepository,
                installmentRepository,
                bankAccountRepository,
                authenticatedUserService,
                notificationService,
                returnAmountCalculator,
                returnPaymentDtoMapper
        );

        jefa = user(10L, "Jefa Cuentas");
        lenient().when(authenticatedUserService.getCurrentUser()).thenReturn(jefa);

        operation = new PaymentOperation();
        operation.setId(1500L);
        operation.setMontoValidado(new BigDecimal("100000.00"));
        operation.setPorcentajeComisionSocio(BigDecimal.ZERO);
        operation.setPorcentajeComisionSocioNivel2(BigDecimal.ZERO);
        operation.setPorcentajeComisionSocioNivel3(BigDecimal.ZERO);
        operation.setPorcentajeComisionOficina(BigDecimal.ZERO);
        operation.setEstatus(OperationStatus.RETORNO_TOTAL_SOLICITADO);
        operation.setSocioComercial(user(99L, "Socio"));

        lenient().when(paymentOperationRepository.findByIdForUpdate(1500L))
                .thenReturn(Optional.of(operation));
        lenient().when(paymentOperationRepository.save(any())).thenAnswer(a -> a.getArgument(0));
        lenient().when(returnPaymentRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        // Repos de parcialidades → estado en memoria
        lenient().when(installmentRepository.save(any())).thenAnswer(a -> {
            OperationReturnInstallment i = a.getArgument(0);
            if (i.getId() == null) {
                i.setId(installmentIdSeq.getAndIncrement());
                installmentsByRequest
                        .computeIfAbsent(i.getSolicitud().getId(), k -> new java.util.ArrayList<>())
                        .add(i);
            }
            return i;
        });
        lenient().when(installmentRepository.findById(anyLong())).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return installmentsByRequest.values().stream()
                    .flatMap(List::stream)
                    .filter(i -> id.equals(i.getId()))
                    .findFirst();
        });
        lenient().when(installmentRepository.findBySolicitudIdOrderByCreatedAtAsc(anyLong()))
                .thenAnswer(a -> installmentsByRequest.getOrDefault(a.getArgument(0), List.of()));
        lenient().when(installmentRepository.sumCompletedBySolicitud(anyLong()))
                .thenAnswer(a -> sumBySolicitud(a.getArgument(0), ReturnInstallmentStatus.COMPLETADA));
        lenient().when(installmentRepository.sumInFlightBySolicitud(anyLong()))
                .thenAnswer(a -> sumBySolicitud(a.getArgument(0),
                        ReturnInstallmentStatus.PROGRAMADA, ReturnInstallmentStatus.ENTREGADA));
        lenient().when(installmentRepository.sumCompletedByOperation(anyLong()))
                .thenAnswer(a -> installmentsByRequest.values().stream()
                        .flatMap(List::stream)
                        .filter(i -> i.getEstatus() == ReturnInstallmentStatus.COMPLETADA)
                        .map(OperationReturnInstallment::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        lenient().when(installmentRepository.countBySolicitudIdAndEstatusIn(anyLong(), any()))
                .thenAnswer(a -> {
                    Long reqId = a.getArgument(0);
                    List<ReturnInstallmentStatus> statuses = a.getArgument(1);
                    return installmentsByRequest.getOrDefault(reqId, List.of()).stream()
                            .filter(i -> statuses.contains(i.getEstatus()))
                            .count();
                });
        lenient().when(installmentRepository.countBySolicitudIdAndEstatusNot(anyLong(), any()))
                .thenAnswer(a -> {
                    Long reqId = a.getArgument(0);
                    ReturnInstallmentStatus excluded = a.getArgument(1);
                    return installmentsByRequest.getOrDefault(reqId, List.of()).stream()
                            .filter(i -> i.getEstatus() != excluded)
                            .count();
                });
        lenient().when(returnPaymentRepository.sumRequestedAmountByOperationId(anyLong()))
                .thenAnswer(a -> requests.values().stream()
                        .map(OperationReturnPayment::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        lenient().when(bankAccountRepository.findById(anyLong()))
                .thenReturn(Optional.of(bankAccount(5L)));
    }

    // ------------------------------------------------------------------

    private BigDecimal sumBySolicitud(Long reqId, ReturnInstallmentStatus... statuses) {
        List<ReturnInstallmentStatus> allowed = List.of(statuses);
        return installmentsByRequest.getOrDefault(reqId, List.of()).stream()
                .filter(i -> allowed.contains(i.getEstatus()))
                .map(OperationReturnInstallment::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Nombre canónico de la persona autorizada por defecto en cada solicitud. */
    private static final String AUTORIZADO_CANONICO = "María Gómez Díaz";

    private OperationReturnPayment request(long id, PaymentType tipo, String monto) {
        OperationReturnPayment r = new OperationReturnPayment();
        r.setId(id);
        r.setOperacion(operation);
        r.setMonto(new BigDecimal(monto));
        r.setTipoPago(tipo);
        r.setEstatus(ReturnPaymentStatus.SOLICITADO);
        r.setAutorizadoParaRecibirEfectivo1(AUTORIZADO_CANONICO);
        requests.put(id, r);
        lenient().when(returnPaymentRepository.findById(id)).thenReturn(Optional.of(r));
        lenient().when(returnPaymentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));
        return r;
    }

    private DeliverReturnInstallmentRequestDto deliver(String personaQueRecibioEfectivo) {
        DeliverReturnInstallmentRequestDto dto = new DeliverReturnInstallmentRequestDto();
        dto.setComprobanteEntregaUrl("https://files/evidencia-entrega.jpg");
        dto.setPersonaQueRecibioEfectivo(personaQueRecibioEfectivo);
        return dto;
    }

    private User user(long id, String nombre) {
        User u = new User();
        u.setId(id);
        u.setNombre(nombre);
        return u;
    }

    private BankAccount bankAccount(long id) {
        BankAccount b = new BankAccount();
        b.setId(id);
        b.setBanco("BBVA");
        return b;
    }

    private CreateReturnInstallmentRequestDto transfer(String monto) {
        CreateReturnInstallmentRequestDto dto = new CreateReturnInstallmentRequestDto();
        dto.setMonto(new BigDecimal(monto));
        dto.setCuentaOrigenId(5L);
        dto.setComprobanteUrl("https://files/comprobante.pdf");
        return dto;
    }

    private CreateReturnInstallmentRequestDto cash(String monto) {
        CreateReturnInstallmentRequestDto dto = new CreateReturnInstallmentRequestDto();
        dto.setMonto(new BigDecimal(monto));
        dto.setFechaHoraRecoleccion(LocalDateTime.now().plusDays(1));
        dto.setEvidenciaImportePreparadoUrl("https://files/evidencia-importe.jpg");
        return dto;
    }

    // ================================================================
    // Reglas
    // ================================================================

    @Test
    void createsInstallmentBelowBalance() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");

        ReturnInstallmentResponseDto dto = service.createInstallment(1L, transfer("15000"));

        assertThat(dto.getMonto()).isEqualByComparingTo("15000.00");
        assertThat(dto.getEstatus()).isEqualTo(ReturnInstallmentStatus.COMPLETADA);
        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.PARCIALMENTE_RETORNADO);
    }

    @Test
    void multipleInstallmentsCompleteARequest() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");

        service.createInstallment(1L, transfer("15000"));
        service.createInstallment(1L, transfer("20000"));
        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.PARCIALMENTE_RETORNADO);

        service.createInstallment(1L, transfer("5000"));
        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.RETORNADO);
    }

    @Test
    void rejectsInstallmentAboveBalance() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");
        service.createInstallment(1L, transfer("35000"));

        assertThatThrownBy(() -> service.createInstallment(1L, transfer("10000")))
                .isInstanceOf(ReturnInstallmentAmountExceedsAvailableException.class);
    }

    @Test
    void rejectsZeroOrNegativeAmount() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");

        assertThatThrownBy(() -> service.createInstallment(1L, transfer("0")))
                .isInstanceOf(InvalidReturnAmountException.class);
        assertThatThrownBy(() -> service.createInstallment(1L, transfer("-5")))
                .isInstanceOf(InvalidReturnAmountException.class);
    }

    @Test
    void rejectsNewInstallmentAfterRequestFullyReturned() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");
        service.createInstallment(1L, transfer("40000"));
        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.RETORNADO);

        assertThatThrownBy(() -> service.createInstallment(1L, transfer("1")))
                .isInstanceOf(ReturnRequestAlreadyFullyReturnedException.class);
    }

    @Test
    void transferInstallmentRequiresReceipt() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");
        CreateReturnInstallmentRequestDto dto = transfer("10000");
        dto.setComprobanteUrl("  ");

        assertThatThrownBy(() -> service.createInstallment(1L, dto))
                .isInstanceOf(ReturnInstallmentReceiptRequiredException.class);
    }

    @Test
    void cashInstallmentRequiresPreparedAmountEvidence() {
        request(1L, PaymentType.EFECTIVO, "25000");
        CreateReturnInstallmentRequestDto dto = cash("10000");
        dto.setEvidenciaImportePreparadoUrl("  ");

        assertThatThrownBy(() -> service.createInstallment(1L, dto))
                .isInstanceOf(ReturnInstallmentPreparedAmountEvidenceRequiredException.class);
    }

    @Test
    void cashInstallmentStoresPreparedAmountEvidence() {
        request(1L, PaymentType.EFECTIVO, "25000");

        ReturnInstallmentResponseDto created = service.createInstallment(1L, cash("10000"));

        assertThat(created.getEvidenciaImportePreparadoUrl())
                .isEqualTo("https://files/evidencia-importe.jpg");
    }

    @Test
    void twoRequestsSameMethodAreIndependent() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");
        request(2L, PaymentType.TRANSFERENCIA, "10000");

        service.createInstallment(1L, transfer("40000"));
        service.createInstallment(2L, transfer("10000"));

        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.RETORNADO);
        assertThat(requests.get(2L).getEstatus()).isEqualTo(ReturnPaymentStatus.RETORNADO);
    }

    @Test
    void scheduledCashInstallmentDoesNotCountAsReturned() {
        request(1L, PaymentType.EFECTIVO, "25000");

        service.createInstallment(1L, cash("10000"));

        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.EN_RECOLECCION);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("0");
    }

    @Test
    void cashFlowCompletesOnlyAfterConfirmAndDeliver() {
        request(1L, PaymentType.EFECTIVO, "25000");
        ReturnInstallmentResponseDto created = service.createInstallment(1L, cash("10000"));

        // socio confirma
        when(authenticatedUserService.getCurrentUser()).thenReturn(operation.getSocioComercial());
        service.confirmInstallment(created.getId());
        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.ENTREGADO);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("0");

        // jefa cierra
        when(authenticatedUserService.getCurrentUser()).thenReturn(jefa);
        service.deliverInstallment(created.getId(), deliver(AUTORIZADO_CANONICO));

        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.PARCIALMENTE_RETORNADO);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("10000");
    }

    @Test
    void cancellingLastActiveInstallmentReturnsRequestToSolicitado() {
        request(1L, PaymentType.EFECTIVO, "25000");
        ReturnInstallmentResponseDto created = service.createInstallment(1L, cash("10000"));
        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.EN_RECOLECCION);

        CancelReturnInstallmentRequestDto cancel = new CancelReturnInstallmentRequestDto();
        cancel.setMotivo("El socio pidió reprogramar");
        service.cancelInstallment(created.getId(), cancel);

        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.SOLICITADO);
    }

    @Test
    void concurrentInstallmentsThatIndividuallyFitButTogetherExceedBalance() {
        // Escenario de sobrepago: dos parcialidades de $15,000 sobre un saldo de
        // $25,000. Individualmente caben; juntas no. Con la guarda dentro del
        // lock, la segunda debe fallar (el estado en memoria simula el commit
        // de la primera antes de evaluar la segunda).
        request(1L, PaymentType.TRANSFERENCIA, "25000");

        service.createInstallment(1L, transfer("15000"));

        assertThatThrownBy(() -> service.createInstallment(1L, transfer("15000")))
                .isInstanceOf(ReturnInstallmentAmountExceedsAvailableException.class);

        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("15000");
    }

    // ================================================================
    // Estatus de operación + ejemplo obligatorio OP-1500
    // ================================================================

    @Test
    void operationStatusReflectsAggregateProgress_OP1500() {
        // amountToReturn = montoValidado (100000) - 0% comisiones = 100000
        OperationReturnPayment sol1 = request(1L, PaymentType.TRANSFERENCIA, "40000");
        OperationReturnPayment sol2 = request(2L, PaymentType.EFECTIVO, "25000");
        request(3L, PaymentType.RETIRO_SIN_TARJETA, "15000");
        OperationReturnPayment sol4 = request(4L, PaymentType.TRANSFERENCIA, "10000");
        OperationReturnPayment sol5 = request(5L, PaymentType.EFECTIVO, "10000");

        // SOL-001: 15k + 20k + 5k = 40k → RETORNADO
        service.createInstallment(1L, transfer("15000"));
        service.createInstallment(1L, transfer("20000"));
        service.createInstallment(1L, transfer("5000"));

        // SOL-002: 1 parcialidad de 10k, ciclo completo → retornado 10k
        ReturnInstallmentResponseDto s2 = service.createInstallment(2L, cash("10000"));
        when(authenticatedUserService.getCurrentUser()).thenReturn(operation.getSocioComercial());
        service.confirmInstallment(s2.getId());
        when(authenticatedUserService.getCurrentUser()).thenReturn(jefa);
        service.deliverInstallment(s2.getId(), deliver(AUTORIZADO_CANONICO));

        // SOL-004: 1 parcialidad única de 10k → RETORNADO
        service.createInstallment(4L, transfer("10000"));

        // SOL-005: 10k programada, NO confirmada → no suma
        service.createInstallment(5L, cash("10000"));

        BigDecimal totalCompletado = installmentRepository.sumCompletedByOperation(1500L);
        assertThat(totalCompletado).isEqualByComparingTo("60000");

        assertThat(sol1.getEstatus()).isEqualTo(ReturnPaymentStatus.RETORNADO);
        assertThat(sol2.getEstatus()).isEqualTo(ReturnPaymentStatus.PARCIALMENTE_RETORNADO);
        assertThat(sol4.getEstatus()).isEqualTo(ReturnPaymentStatus.RETORNADO);
        assertThat(sol5.getEstatus()).isEqualTo(ReturnPaymentStatus.EN_RECOLECCION);
        assertThat(operation.getEstatus()).isEqualTo(OperationStatus.RETORNO_PARCIAL_ENTREGADO);
    }

    @Test
    void notifiesSocioWhenInstallmentCompleted() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");
        service.createInstallment(1L, transfer("15000"));

        verify(notificationService).createForUser(
                eq(operation.getSocioComercial().getId()),
                any(), any(),
                eq(NotificationType.RETURN_INSTALLMENT_COMPLETED),
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void doesNotNotifyCompletionForScheduledCash() {
        request(1L, PaymentType.EFECTIVO, "25000");
        service.createInstallment(1L, cash("10000"));

        verify(notificationService, never()).createForUser(
                anyLong(), any(), any(),
                eq(NotificationType.RETURN_INSTALLMENT_COMPLETED),
                any(), any(), any(), any(), any()
        );
    }

    // ================================================================
    // Cierre de entrega: persona autorizada que recibió + evidencia
    // ================================================================

    private CreateReturnInstallmentRequestDto cashRst(String monto) {
        CreateReturnInstallmentRequestDto dto = cash(monto);
        dto.setCuentaOrigenId(5L);
        dto.setCodigoRetiroSinTarjeta("RST-0001");
        return dto;
    }

    /** Crea una parcialidad de efectivo/RST y la deja en ENTREGADA (socio confirmó). */
    private Long installmentReadyToDeliver(long reqId, PaymentType tipo, String monto) {
        CreateReturnInstallmentRequestDto create =
                tipo == PaymentType.RETIRO_SIN_TARJETA ? cashRst(monto) : cash(monto);
        ReturnInstallmentResponseDto created = service.createInstallment(reqId, create);

        when(authenticatedUserService.getCurrentUser()).thenReturn(operation.getSocioComercial());
        service.confirmInstallment(created.getId());
        // El cierre valida receptor/evidencia antes de leer el usuario; para los
        // casos de rechazo este stub no llega a usarse.
        lenient().when(authenticatedUserService.getCurrentUser()).thenReturn(jefa);
        return created.getId();
    }

    private OperationReturnInstallment storedInstallment(Long id) {
        return installmentsByRequest.values().stream()
                .flatMap(List::stream)
                .filter(i -> id.equals(i.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void closesCashInstallmentWithAuthorizedReceiver() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");

        ReturnInstallmentResponseDto dto = service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));

        assertThat(dto.getEstatus()).isEqualTo(ReturnInstallmentStatus.COMPLETADA);
        assertThat(dto.getPersonaQueRecibioEfectivo()).isEqualTo(AUTORIZADO_CANONICO);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("10000");
    }

    @Test
    void closesWithdrawalWithoutCardWithAuthorizedReceiver() {
        request(1L, PaymentType.RETIRO_SIN_TARJETA, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.RETIRO_SIN_TARJETA, "10000");

        ReturnInstallmentResponseDto dto = service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));

        assertThat(dto.getEstatus()).isEqualTo(ReturnInstallmentStatus.COMPLETADA);
        assertThat(dto.getPersonaQueRecibioEfectivo()).isEqualTo(AUTORIZADO_CANONICO);
    }

    @Test
    void storesCanonicalAuthorizedNameIgnoringCaseSpacesAndAccents() {
        OperationReturnPayment r = request(1L, PaymentType.EFECTIVO, "25000");
        r.setAutorizadoParaRecibirEfectivo1("María Gómez Díaz");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");

        ReturnInstallmentResponseDto dto =
                service.deliverInstallment(id, deliver("  MARIA   gomez  diaz "));

        assertThat(dto.getPersonaQueRecibioEfectivo()).isEqualTo("María Gómez Díaz");
    }

    @Test
    void rejectsBlankReceiver() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");

        assertThatThrownBy(() -> service.deliverInstallment(id, deliver("   ")))
                .isInstanceOf(ReturnInstallmentReceiverRequiredException.class);

        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("0");
    }

    @Test
    void rejectsReceiverNotAmongAuthorized() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");

        assertThatThrownBy(() -> service.deliverInstallment(id, deliver("Pedro Pérez")))
                .isInstanceOf(ReturnInstallmentReceiverNotAuthorizedException.class);

        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
    }

    @Test
    void rejectsCloseWhenRequestHasNoAuthorizedPeople() {
        OperationReturnPayment r = request(1L, PaymentType.EFECTIVO, "25000");
        r.setAutorizadoParaRecibirEfectivo1("   ");
        r.setAutorizadoParaRecibirEfectivo2(null);
        r.setAutorizadoParaRecibirEfectivo3(null);
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");

        assertThatThrownBy(() -> service.deliverInstallment(id, deliver("María Gómez Díaz")))
                .isInstanceOf(ReturnInstallmentNoAuthorizedRecipientsException.class);

        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
    }

    @Test
    void rejectsBlankDeliveryProof() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");

        DeliverReturnInstallmentRequestDto req = deliver(AUTORIZADO_CANONICO);
        req.setComprobanteEntregaUrl("   ");

        assertThatThrownBy(() -> service.deliverInstallment(id, req))
                .isInstanceOf(ReturnInstallmentReceiptRequiredException.class);

        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
    }

    @Test
    void jefaCanCloseBeforeSocioConfirmation() {
        request(1L, PaymentType.EFECTIVO, "25000");
        ReturnInstallmentResponseDto created = service.createInstallment(1L, cash("10000"));
        // PROGRAMADA — el socio no ha confirmado. La jefa cierra igual.

        ReturnInstallmentResponseDto dto =
                service.deliverInstallment(created.getId(), deliver(AUTORIZADO_CANONICO));

        assertThat(dto.getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
        assertThat(dto.isCerradoPorJefa()).isTrue();
        assertThat(dto.isConfirmadoPorSocio()).isFalse();
        assertThat(dto.getPersonaQueRecibioEfectivo()).isEqualTo(AUTORIZADO_CANONICO);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("0");
    }

    @Test
    void rejectsCloseForNonCashMethod() {
        request(1L, PaymentType.TRANSFERENCIA, "40000");
        ReturnInstallmentResponseDto created = service.createInstallment(1L, transfer("10000"));

        assertThatThrownBy(() -> service.deliverInstallment(created.getId(), deliver(AUTORIZADO_CANONICO)))
                .isInstanceOf(ReturnInstallmentInvalidStatusException.class);
    }

    @Test
    void failedValidationDoesNotChangeStatusOrTotals() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");

        assertThatThrownBy(() -> service.deliverInstallment(id, deliver("No autorizada")))
                .isInstanceOf(ReturnInstallmentReceiverNotAuthorizedException.class);

        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
        assertThat(storedInstallment(id).getPersonaQueRecibioEfectivo()).isNull();
        assertThat(requests.get(1L).getEstatus()).isEqualTo(ReturnPaymentStatus.ENTREGADO);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("0");
    }

    @Test
    void exposesReceiverInHistory() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");
        service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));

        ReturnInstallmentResponseDto fromHistory = service.findInstallmentsByRequest(1L).stream()
                .filter(i -> id.equals(i.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(fromHistory.getPersonaQueRecibioEfectivo()).isEqualTo(AUTORIZADO_CANONICO);
        assertThat(fromHistory.getEntregadoPorNombre()).isEqualTo(jefa.getNombre());
    }

    @Test
    void historicalInstallmentWithNullReceiverStillReadable() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");
        // Parcialidad histórica: nunca se registró la persona receptora.

        ReturnInstallmentResponseDto fromHistory = service.findInstallmentsByRequest(1L).stream()
                .filter(i -> id.equals(i.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(fromHistory.getPersonaQueRecibioEfectivo()).isNull();
    }

    @Test
    void deliverTransitionIsIdempotent() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = installmentReadyToDeliver(1L, PaymentType.EFECTIVO, "10000");

        service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));

        assertThatThrownBy(() -> service.deliverInstallment(id, deliver("Pedro Pérez")))
                .isInstanceOf(ReturnInstallmentInvalidStatusException.class);

        assertThat(storedInstallment(id).getPersonaQueRecibioEfectivo()).isEqualTo(AUTORIZADO_CANONICO);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("10000");
    }

    // ================================================================
    // Doble confirmación independiente (socio / jefa de cajas)
    // ================================================================

    private void confirmAsSocio(Long installmentId) {
        when(authenticatedUserService.getCurrentUser()).thenReturn(operation.getSocioComercial());
        service.confirmInstallment(installmentId);
        lenient().when(authenticatedUserService.getCurrentUser()).thenReturn(jefa);
    }

    private Long scheduledCashInstallment(String monto) {
        return service.createInstallment(1L, cash(monto)).getId();
    }

    @Test
    void socioConfirmationAloneLeavesItInEntregadaAndNotCounted() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");

        confirmAsSocio(id);

        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
        assertThat(storedInstallment(id).getConfirmadoPor()).isEqualTo(operation.getSocioComercial());
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("0");
    }

    @Test
    void completesWhenJefaClosesAfterSocioConfirmed() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");

        confirmAsSocio(id);
        ReturnInstallmentResponseDto dto = service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));

        assertThat(dto.getEstatus()).isEqualTo(ReturnInstallmentStatus.COMPLETADA);
        assertThat(dto.isConfirmadoPorSocio()).isTrue();
        assertThat(dto.isCerradoPorJefa()).isTrue();
        assertThat(storedInstallment(id).getFechaRealizacion()).isNotNull();
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("10000");
    }

    @Test
    void completesWhenSocioConfirmsAfterJefaClosed() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");

        // jefa primero
        service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));
        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("0");

        // socio después
        confirmAsSocio(id);

        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.COMPLETADA);
        assertThat(storedInstallment(id).getFechaRealizacion()).isNotNull();
        assertThat(installmentRepository.sumCompletedBySolicitud(1L)).isEqualByComparingTo("10000");
    }

    @Test
    void socioCannotConfirmTwice() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");
        confirmAsSocio(id);

        // Se rechaza en el chequeo de idempotencia, antes de leer el usuario.
        assertThatThrownBy(() -> service.confirmInstallment(id))
                .isInstanceOf(ReturnInstallmentInvalidStatusException.class);
    }

    @Test
    void jefaCannotCloseTwice() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");
        service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));

        assertThatThrownBy(() -> service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO)))
                .isInstanceOf(ReturnInstallmentInvalidStatusException.class);
        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.ENTREGADA);
    }

    @Test
    void neitherPartyCanActOnACompletedInstallment() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");
        confirmAsSocio(id);
        service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));

        assertThatThrownBy(() -> service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO)))
                .isInstanceOf(ReturnInstallmentInvalidStatusException.class);
        assertThatThrownBy(() -> service.confirmInstallment(id))
                .isInstanceOf(ReturnInstallmentInvalidStatusException.class);
    }

    @Test
    void jefaCloseFromProgramadaStillRequiresProofAndAuthorizedPerson() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");

        DeliverReturnInstallmentRequestDto sinFoto = deliver(AUTORIZADO_CANONICO);
        sinFoto.setComprobanteEntregaUrl("  ");
        assertThatThrownBy(() -> service.deliverInstallment(id, sinFoto))
                .isInstanceOf(ReturnInstallmentReceiptRequiredException.class);

        assertThatThrownBy(() -> service.deliverInstallment(id, deliver("Persona No Autorizada")))
                .isInstanceOf(ReturnInstallmentReceiverNotAuthorizedException.class);

        assertThat(storedInstallment(id).getEstatus()).isEqualTo(ReturnInstallmentStatus.PROGRAMADA);
    }

    @Test
    void cannotCancelOnceThereIsOneMark() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");
        confirmAsSocio(id);

        CancelReturnInstallmentRequestDto cancel = new CancelReturnInstallmentRequestDto();
        cancel.setMotivo("El socio pidió reprogramar");
        assertThatThrownBy(() -> service.cancelInstallment(id, cancel))
                .isInstanceOf(ReturnInstallmentNotCancellableException.class);
    }

    @Test
    void jefaClosingFirstNotifiesSocioToConfirm() {
        request(1L, PaymentType.EFECTIVO, "25000");
        Long id = scheduledCashInstallment("10000");

        service.deliverInstallment(id, deliver(AUTORIZADO_CANONICO));

        verify(notificationService).createForUser(
                eq(operation.getSocioComercial().getId()),
                any(), any(),
                eq(NotificationType.RETURN_INSTALLMENT_DELIVERED),
                any(), any(), any(), any(), any()
        );
    }
}
