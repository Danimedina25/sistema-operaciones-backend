package com.sistemadeoperaciones.cheques;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.corte.repository.DailyCashCutRepository;
import com.sistemadeoperaciones.corte.enums.DailyCashCutStatus;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.repository.*;
import com.sistemadeoperaciones.pagos.service.PaymentOperationServiceImpl;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.*;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service @lombok.RequiredArgsConstructor
public class ChequeService {
    private final OperationPaymentRepository payments;
    private final PaymentOperationRepository operations;
    private final ChequeAuditRepository audits;
    private final CashGeneralService cash;
    private final CashGeneralRegisterRepository register;
    private final DailyCashCutRepository cuts;
    private final com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository bankCuts;
    private final PaymentOperationServiceImpl paymentService;
    private final AuthenticatedUserService auth;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;
    @PersistenceContext private EntityManager em;
    public record Changed(Long paymentId, String action) {}
    private static final Set<String> STATES = Set.of("POR_COBRAR", "DEPOSITADO", "COBRADO", "DEVUELTO", "CANCELADO");
    private boolean has(User user, RoleName... roles) {
        return user.getRoles().stream().anyMatch(r -> Arrays.asList(roles).contains(r.getName()));
    }
    private User authorizeRead() {
        User user = auth.getCurrentUser();
        if (!has(user, RoleName.ADMIN, RoleName.JEFA_CUENTAS, RoleName.AUXILIAR_CUENTAS, RoleName.JEFA_CAJAS, RoleName.GERENTE, RoleName.DIRECCION))
            throw new AccessDeniedException("No tienes acceso a cheques");
        return user;
    }
    private void authorizeAction(User user, ChequeAction action) {
        boolean allowed = action == ChequeAction.COBRAR_EFECTIVO
                ? has(user, RoleName.ADMIN, RoleName.JEFA_CAJAS)
                : has(user, RoleName.ADMIN, RoleName.JEFA_CUENTAS, RoleName.AUXILIAR_CUENTAS);
        if (!allowed) throw new AccessDeniedException("No tienes permiso para esta acción del cheque");
    }
    private OperationPayment requireCheque(Long id) {
        OperationPayment payment = payments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cheque no encontrado"));
        if (payment.getTipoPago() != PaymentType.CHEQUE) throw new ResourceNotFoundException("Cheque no encontrado");
        return payment;
    }
    @Transactional(readOnly=true)
    public ChequeView byPayment(Long id) { authorizeRead(); return view(requireCheque(id), audits.findByPagoIdOrderByIdAsc(id)); }

    @Transactional
    public ChequeView act(Long id, ChequeCommand command) {
        User user = authorizeRead();
        if (command.accion() == null || command.requestId() == null) throw new BusinessException("Acción e identificador obligatorios");
        authorizeAction(user, command.accion());
        OperationPayment p = requireCheque(id);
        operations.findByIdForUpdate(p.getOperacion().getId()).orElseThrow();
        em.refresh(p.getOperacion(), LockModeType.PESSIMISTIC_WRITE);
        em.refresh(p, LockModeType.PESSIMISTIC_WRITE);
        // Serialize idempotency lookup/insertion as well: different missing UUIDs can
        // otherwise hold overlapping MySQL gap locks while waiting for the cash mutex.
        register.ensureRegister(); register.lockRegister();
        String hash = fingerprint(command);
        // Current locking read avoids a stale repeatable-read snapshot on MySQL after waiting.
        List<ChequeAudit> prior = em.createQuery("select a from ChequeAudit a where a.requestId=:key", ChequeAudit.class)
                .setParameter("key", command.requestId().toString()).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (!prior.isEmpty()) {
            ChequeAudit old = prior.get(0);
            if (!old.getPagoId().equals(id) || !hash.equals(old.getCommandHash())) throw new ConflictException("La clave ya fue usada para otra solicitud");
            try { return mapper.readValue(old.getResultado(), ChequeView.class); }
            catch (Exception e) { throw new IllegalStateException("No se pudo recuperar la respuesta del cheque", e); }
        }
        if (p.getChequeEstado() == null) throw new ConflictException("El cheque histórico requiere conciliación");
        if (command.version() == null || command.version() != p.getChequeVersion()) throw new ConflictException("El cheque cambió; actualiza la consulta");
        if (!Boolean.TRUE.equals(p.getOperacion().getActivo())) throw new ConflictException("La operación está inactiva");
        if (p.getEstatus() != PaymentStatus.PENDIENTE_VALIDACION && p.getEstatus() != PaymentStatus.EN_PROCESO)
            throw new ConflictException("El pago no está pendiente");
        validateTransition(p.getChequeEstado(), command.accion());
        if (command.fecha() == null || command.fecha().isAfter(LocalDate.now()) || command.fecha().isBefore(p.getCreatedAt().toLocalDate()))
            throw new BusinessException("Fecha efectiva inválida para el cheque");
        if (p.getChequeFechaDeposito() != null && command.fecha().isBefore(p.getChequeFechaDeposito().toLocalDate()))
            throw new BusinessException("La fecha no puede preceder al depósito");
        // Shared with daily-cut registration: absence of a closed row is also protected.
        boolean financial = command.accion() == ChequeAction.COBRAR_BANCO || command.accion() == ChequeAction.COBRAR_EFECTIVO;
        if (financial && cuts.findFirstByFechaGreaterThanEqualAndEstatusOrderByFechaAsc(command.fecha(), DailyCashCutStatus.CERRADO).isPresent())
            throw new ConflictException("El periodo está cerrado");
        boolean bank = command.accion() == ChequeAction.DEPOSITAR || command.accion() == ChequeAction.COBRAR_BANCO;
        boolean cashAction = command.accion() == ChequeAction.COBRAR_EFECTIVO;
        if (!bank && command.cuentaDestinoId() != null) throw new BusinessException("Esta acción no admite cuenta bancaria");
        if (!cashAction && (command.diaCajaId() != null || command.denominaciones() != null)) throw new BusinessException("Esta acción no admite datos de caja");
        if (bank || cashAction) validateProof(command.comprobanteUrl(), p.getOperacion().getId());
        else if (command.motivo() == null || command.motivo().isBlank() || command.motivo().length() > 500) throw new BusinessException("Indica el motivo");
        String previous = snapshot(p);
        if (bank) {
            BankAccount account = command.cuentaDestinoId() == null ? null : em.find(BankAccount.class, command.cuentaDestinoId(), LockModeType.PESSIMISTIC_WRITE);
            if (account != null) em.refresh(account, LockModeType.PESSIMISTIC_WRITE);
            if (account == null || !Boolean.TRUE.equals(account.getActivo())) throw new BusinessException("Selecciona una cuenta bancaria activa");
            if (financial && bankCuts.findFirstByBankAccountIdAndFechaGreaterThanEqualOrderByFechaAsc(account.getId(), command.fecha()).isPresent())
                throw new ConflictException("La cuenta tiene cortes cerrados que incluyen esta fecha");
            p.setCuentaDestino(account);
        }
        switch (command.accion()) {
            case DEPOSITAR -> { p.setChequeEstado("DEPOSITADO"); p.setChequeFechaDeposito(command.fecha().atStartOfDay()); }
            case COBRAR_BANCO, COBRAR_EFECTIVO -> {
                p.setChequeEstado("COBRADO"); p.setChequeDestinoCobro(cashAction ? "EFECTIVO" : "CUENTA_BANCARIA");
                p.setChequeFechaCobro(command.fecha().atStartOfDay()); p.setEstatus(PaymentStatus.VALIDADA);
                p.setFechaValidacion(LocalDateTime.now()); p.setValidadoPor(user); p.setComprobanteValidacionUrl(command.comprobanteUrl());
                if (cashAction) {
                    p.setCuentaDestino(null);
                    cash.recordChequePayment(p, command.denominaciones(), command.diaCajaId(), command.fecha());
                }
            }
            case DEVOLVER, CANCELAR -> { p.setChequeEstado(command.accion() == ChequeAction.DEVOLVER ? "DEVUELTO" : "CANCELADO"); p.setEstatus(PaymentStatus.RECHAZADA); p.setObservaciones(command.motivo().trim()); }
        }
        p.setEnProcesoPor(null); p.setFechaEnProceso(null); p.setChequeVersion(p.getChequeVersion() + 1);
        payments.saveAndFlush(p);
        paymentService.recalculateAfterCheque(p);
        ChequeAudit audit = new ChequeAudit(); audit.setPagoId(id); audit.setRequestId(command.requestId().toString());
        audit.setCommandHash(hash); audit.setAccion(command.accion().name()); audit.setFecha(command.fecha().atStartOfDay());
        audit.setRegistradoEn(LocalDateTime.now()); audit.setUsuarioId(user.getId()); audit.setUsuarioNombre(user.getNombre());
        audit.setMotivo(command.motivo()); audit.setComprobanteUrl(command.comprobanteUrl());
        audit.setDetalle(previous + " -> " + snapshot(p)); audits.saveAndFlush(audit);
        ChequeView result = view(p, audits.findByPagoIdOrderByIdAsc(id));
        try { audit.setResultado(mapper.writeValueAsString(result)); }
        catch (Exception e) { throw new IllegalStateException(e); }
        audits.saveAndFlush(audit);
        events.publishEvent(new Changed(id, command.accion().name()));
        return result;
    }
    static void validateTransition(String state, ChequeAction action) {
        boolean initial = "POR_COBRAR".equals(state), deposited = "DEPOSITADO".equals(state);
        if (!(initial || deposited && (action == ChequeAction.COBRAR_BANCO || action == ChequeAction.DEVOLVER)))
            throw new ConflictException("Transición de cheque no permitida");
    }
    public static void validateProof(String proof, Long operationId) {
        try {
            URI uri = URI.create(proof);
            String path = URLDecoder.decode(uri.getRawPath(), StandardCharsets.UTF_8);
            if (proof.length() > 500 || !"https".equals(uri.getScheme()) || !"firebasestorage.googleapis.com".equals(uri.getHost())
                    || !path.contains("/o/comprobantes/operaciones/" + operationId + "/") || path.contains(".."))
                throw new IllegalArgumentException();
        } catch (Exception e) { throw new BusinessException("El comprobante debe pertenecer a esta operación en el almacenamiento autorizado"); }
    }
    private String fingerprint(ChequeCommand c) {
        try {
            // Record property order is stable; denomination map order must not affect replay.
            var canonical = mapper.copy().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.writeValueAsBytes(c)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    static String snapshot(OperationPayment p) {
        return "estado=" + p.getChequeEstado() + ", monto=" + p.getMonto() + ", cuenta=" + (p.getCuentaDestino() == null ? null : p.getCuentaDestino().getId())
                + ", numero=" + p.getNumeroCheque() + ", banco=" + p.getBancoEmisor() + ", emisor=" + p.getEmisor() + ", beneficiario=" + p.getBeneficiario();
    }
    private ChequeView view(OperationPayment p, List<ChequeAudit> history) {
        BankAccount b = p.getCuentaDestino();
        return new ChequeView(p.getId(), p.getId(), p.getOperacion().getId(), p.getOperacion().getCliente().getNombre(),
                p.getNumeroCheque(), p.getBancoEmisor(), p.getEmisor(), p.getBeneficiario(), p.getMonto(), "MXN", p.getChequeEstado(), p.getChequeEstado() == null,
                p.getChequeVersion(), p.getCreatedAt(), b == null ? null : b.getId(), b == null ? null : b.getTitular()+" — "+b.getBanco()+" — "+b.getNumeroCuenta(),
                p.getChequeDestinoCobro(), p.getComprobanteUrl(), history.stream().map(a -> new ChequeView.History(a.getId(), a.getAccion(), a.getFecha(), a.getUsuarioNombre(), a.getMotivo(), a.getComprobanteUrl())).toList());
    }
    @Transactional(readOnly=true)
    public ChequeView.Page list(String states, String search, String customer, Long operationId, String bank, LocalDate from, LocalDate to, int page) {
        authorizeRead();
        if (page < 0 || from != null && to != null && from.isAfter(to)) throw new BusinessException("Filtros inválidos");
        Set<String> selected = states == null || states.isBlank() ? Set.of() : new HashSet<>(Arrays.asList(states.split(",")));
        if (!STATES.containsAll(selected)) throw new BusinessException("Estado de cheque inválido");
        Specification<OperationPayment> base = (r, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>(); predicates.add(cb.equal(r.get("tipoPago"), PaymentType.CHEQUE));
            if (operationId != null) predicates.add(cb.equal(r.get("operacion").get("id"), operationId));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(r.get("createdAt"), from.atStartOfDay()));
            if (to != null) predicates.add(cb.lessThan(r.get("createdAt"), to.plusDays(1).atStartOfDay()));
            if (customer != null && !customer.isBlank()) predicates.add(cb.like(cb.lower(r.get("operacion").get("cliente").get("nombre")), pattern(customer), '\\'));
            if (bank != null && !bank.isBlank()) predicates.add(cb.like(cb.lower(r.get("bancoEmisor")), pattern(bank), '\\'));
            if (search != null && !search.isBlank()) predicates.add(cb.or(
                cb.like(cb.lower(r.get("numeroCheque")), pattern(search), '\\'), cb.like(cb.lower(r.get("bancoEmisor")), pattern(search), '\\'),
                cb.like(cb.lower(r.get("emisor")), pattern(search), '\\'), cb.like(cb.lower(r.get("beneficiario")), pattern(search), '\\')));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Specification<OperationPayment> filtered = selected.isEmpty() ? base : base.and((r,q,cb) -> r.get("chequeEstado").in(selected));
        Page<OperationPayment> data = payments.findAll(filtered, PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        Map<Long,List<ChequeAudit>> histories = new HashMap<>();
        if (!data.isEmpty()) audits.findByPagoIdInOrderByIdAsc(data.getContent().stream().map(OperationPayment::getId).toList())
                .forEach(a -> histories.computeIfAbsent(a.getPagoId(), ignored -> new ArrayList<>()).add(a));
        CriteriaBuilder cb = em.getCriteriaBuilder(); CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<OperationPayment> r = query.from(OperationPayment.class);
        query.multiselect(r.get("chequeEstado"), cb.sum(r.<BigDecimal>get("monto"))).where(base.toPredicate(r, query, cb)).groupBy(r.get("chequeEstado"));
        Map<String,BigDecimal> totals = new HashMap<>();
        em.createQuery(query).getResultList().forEach(row -> totals.put((String)row[0], (BigDecimal)row[1]));
        return new ChequeView.Page(data.getContent().stream().map(p -> view(p, histories.getOrDefault(p.getId(), List.of()))).toList(), data.getTotalPages(), data.getTotalElements(),
                List.of(new ChequeView.Total("MXN", totals.getOrDefault("POR_COBRAR", BigDecimal.ZERO), totals.getOrDefault("DEPOSITADO", BigDecimal.ZERO), totals.getOrDefault("COBRADO", BigDecimal.ZERO))));
    }
    private static String pattern(String value) { return "%"+value.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")+"%"; }
}
