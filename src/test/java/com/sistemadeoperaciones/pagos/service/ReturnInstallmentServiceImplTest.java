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
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentReceiptRequiredException;
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

    private OperationReturnPayment request(long id, PaymentType tipo, String monto) {
        OperationReturnPayment r = new OperationReturnPayment();
        r.setId(id);
        r.setOperacion(operation);
        r.setMonto(new BigDecimal(monto));
        r.setTipoPago(tipo);
        r.setEstatus(ReturnPaymentStatus.SOLICITADO);
        requests.put(id, r);
        lenient().when(returnPaymentRepository.findById(id)).thenReturn(Optional.of(r));
        lenient().when(returnPaymentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));
        return r;
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
        DeliverReturnInstallmentRequestDto deliver = new DeliverReturnInstallmentRequestDto();
        deliver.setComprobanteEntregaUrl("https://files/evidencia.jpg");
        service.deliverInstallment(created.getId(), deliver);

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
        DeliverReturnInstallmentRequestDto d2 = new DeliverReturnInstallmentRequestDto();
        d2.setComprobanteEntregaUrl("https://files/e.jpg");
        service.deliverInstallment(s2.getId(), d2);

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
}
