package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.corte.dto.BankAccountBalanceDetailResponseDto;
import com.sistemadeoperaciones.corte.dto.BankAccountBalanceResponseDto;
import com.sistemadeoperaciones.corte.dto.BankGroupBalanceResponseDto;
import com.sistemadeoperaciones.corte.dto.BankLedgerBuckets;
import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.model.BankAccountDailyCut;
import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.cuentasbancarias.service.BankNames;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankAccountDailyCutServiceImpl implements BankAccountDailyCutService {
    @org.springframework.beans.factory.annotation.Autowired
    private com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository chequePeriodLock;


    private final BankAccountRepository bankAccountRepository;

    private final BankAccountDailyCutRepository bankAccountDailyCutRepository;

    /**
     * Única fuente de los importes. Antes este servicio sumaba los repositorios de pagos y
     * retornos por su cuenta, con la fórmula duplicada en dos métodos; ahora el saldo en vivo,
     * el corte registrado y el historial de movimientos comparten la misma definición.
     */
    private final BankLedgerQuery ledger;

    public BankAccountDailyCutServiceImpl(
            BankAccountRepository bankAccountRepository,
            BankAccountDailyCutRepository bankAccountDailyCutRepository,
            BankLedgerQuery ledger
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankAccountDailyCutRepository = bankAccountDailyCutRepository;
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountBalanceDetailResponseDto calculateBalance(
            Long bankAccountId,
            LocalDate fecha
    ) {

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha es obligatoria"
            );
        }

        BankAccount account =
                bankAccountRepository.findById(bankAccountId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Cuenta bancaria no encontrada"
                                )
                        );

        /*
         * FECHA HISTÓRICA: manda el corte registrado. No se recalcula para que el histórico
         * siga siendo reproducible.
         */
        if (fecha.isBefore(LocalDate.now())) {

            var cut = bankAccountDailyCutRepository.findByBankAccountIdAndFecha(bankAccountId, fecha);
            if (cut.isPresent()) return mapToDetailResponse(cut.get());
            // Una cuenta todavía inexistente aporta cero al saldo inicial de un periodo anterior.
            if (fecha.isBefore(account.getCreatedAt().toLocalDate())) {
                return emptyBalance(account, fecha);
            }
            throw new ResourceNotFoundException("No existe corte registrado para la fecha " + fecha);
        }

        /*
         * HOY = TIEMPO REAL
         */
        return calculateBalanceLive(
                account,
                fecha
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountBalanceResponseDto> calculateBalances(LocalDate fecha) {

        final LocalDate fechaConsulta =
                fecha == null
                        ? LocalDate.now()
                        : fecha;

        return bankAccountRepository.findByActivoTrue()
                .stream()
                .map(account -> {

                    BankAccountBalanceDetailResponseDto detail =
                            calculateBalance(
                                    account.getId(),
                                    fechaConsulta
                            );

                    BankAccountBalanceResponseDto dto =
                            new BankAccountBalanceResponseDto();

                    dto.setBankAccountId(detail.getBankAccountId());
                    dto.setBanco(detail.getBanco());
                    dto.setTitular(detail.getTitular());
                    dto.setNumeroCuenta(detail.getNumeroCuenta());
                    dto.setClabe(detail.getClabe());
                    dto.setFecha(detail.getFecha());
                    dto.setSaldoFinal(detail.getSaldoFinal());

                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public List<BankGroupBalanceResponseDto> calculateBalancesGrouped(LocalDate fecha) {

        final LocalDate fechaConsulta =
                fecha == null
                        ? LocalDate.now()
                        : fecha;

        if (fechaConsulta.isBefore(LocalDate.now())) {
            registerDailyCut(fechaConsulta);
        }

        List<BankAccountBalanceResponseDto> cuentas =
                calculateBalances(fechaConsulta);

        // "SCOTIABANK PFAE" y "SCOTIA NOMINA" son el mismo banco: se agrupa con la misma regla
        // que Caja General usa para el "Retiro N de M".
        BankNames catalog = BankNames.of(
                bankAccountRepository.findAll().stream().map(BankAccount::getBanco).toList()
        );

        return cuentas.stream()
                .collect(
                        Collectors.groupingBy(
                                cuenta -> catalog.display(cuenta.getBanco()),
                                java.util.LinkedHashMap::new,
                                Collectors.toList()
                        )
                )
                .entrySet()
                .stream()
                .map(entry -> {

                    List<BankAccountBalanceResponseDto> cuentasBanco =
                            entry.getValue();

                    BigDecimal saldoTotal =
                            cuentasBanco.stream()
                                    .map(BankAccountBalanceResponseDto::getSaldoFinal)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BankGroupBalanceResponseDto dto =
                            new BankGroupBalanceResponseDto();

                    dto.setBanco(entry.getKey());
                    dto.setSaldoTotalBanco(saldoTotal);
                    dto.setTotalCuentas(cuentasBanco.size());
                    dto.setCuentas(cuentasBanco);

                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public void registerDailyCut(LocalDate fecha) {
        if (chequePeriodLock != null) { chequePeriodLock.ensureRegister(); chequePeriodLock.lockRegister(); }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha es obligatoria"
            );
        }

        // findAll y no findByActivoTrue: una cuenta desactivada con saldo debe seguir
        // generando corte, o su cadena de saldo inicial se rompe y deja de ser consultable.
        for (BankAccount account : bankAccountRepository.findAll()) {

            boolean existeCorte =
                    bankAccountDailyCutRepository
                            .existsByBankAccountIdAndFecha(
                                    account.getId(),
                                    fecha
                            );

            if (existeCorte) {
                continue;
            }

            persistCut(account, fecha);
        }
    }

    /**
     * Rehace los cortes ya registrados de una cuenta desde una fecha en adelante.
     *
     * <p>Lo necesita la eliminación administrativa de un corte de Caja General: si ese día
     * contenía cheques cobrados, sus salidas bancarias desaparecen y los cortes que ya las
     * habían contabilizado quedarían con una salida sin origen. Se borran y se regeneran en
     * orden ascendente para que la cadena {@code saldoInicial = saldoFinal del corte anterior}
     * vuelva a cuadrar. No se inventan cortes de fechas que nunca lo tuvieron.
     *
     * @return cuántos cortes se regeneraron
     */
    @Override
    @Transactional
    public int recalculateFrom(Long bankAccountId, LocalDate fecha) {
        if (chequePeriodLock != null) { chequePeriodLock.ensureRegister(); chequePeriodLock.lockRegister(); }

        if (bankAccountId == null || fecha == null) {
            throw new IllegalArgumentException(
                    "La cuenta y la fecha son obligatorias"
            );
        }

        BankAccount account =
                bankAccountRepository.findById(bankAccountId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Cuenta bancaria no encontrada"
                                )
                        );

        List<BankAccountDailyCut> afectados =
                bankAccountDailyCutRepository
                        .findByBankAccountIdAndFechaGreaterThanEqualOrderByFechaAsc(
                                bankAccountId,
                                fecha
                        );

        if (afectados.isEmpty()) {
            return 0;
        }

        List<LocalDate> fechas =
                afectados.stream()
                        .map(BankAccountDailyCut::getFecha)
                        .collect(Collectors.toCollection(ArrayList::new));

        // Primero se borran todos: si se regenerara uno a uno, el saldo inicial seguiría
        // leyendo la fila vieja del día anterior.
        bankAccountDailyCutRepository.deleteAll(afectados);
        bankAccountDailyCutRepository.flush();

        for (LocalDate dia : fechas) {
            persistCut(account, dia);
        }

        return fechas.size();
    }

    @Override
    @Transactional
    public int recalculateAllFrom(LocalDate fecha) {
        if (fecha == null || !fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha debe ser anterior a hoy");
        }

        // findAll: una cuenta desactivada también conserva su cadena de cortes.
        int total = 0;
        for (BankAccount account : bankAccountRepository.findAll()) {
            total += recalculateFrom(account.getId(), fecha);
        }
        return total;
    }

    private void persistCut(BankAccount account, LocalDate fecha) {

        BankAccountBalanceDetailResponseDto calculado =
                calculateBalanceLive(account, fecha);

        BankAccountDailyCut cut = new BankAccountDailyCut();

        cut.setBankAccount(account);
        cut.setFecha(fecha);
        cut.setSaldoInicial(calculado.getSaldoInicial());
        cut.setEntradasTransferencia(calculado.getEntradasTransferencia());
        cut.setEntradasDeposito(calculado.getEntradasDeposito());
        cut.setEntradasCheque(calculado.getEntradasCheque());
        cut.setSalidasRetornos(calculado.getSalidasRetornos());
        cut.setSalidasCajaGeneral(calculado.getSalidasCajaGeneral());
        cut.setSalidasComisiones(calculado.getSalidasComisiones());
        cut.setEntradasTransferenciaInterna(calculado.getEntradasTransferenciaInterna());
        cut.setSalidasTransferenciaInterna(calculado.getSalidasTransferenciaInterna());
        cut.setTotalEntradas(calculado.getTotalEntradas());
        cut.setTotalSalidas(calculado.getTotalSalidas());
        cut.setSaldoFinal(calculado.getSaldoFinal());

        bankAccountDailyCutRepository.save(cut);
    }

    /**
     * Saldo de una cuenta en una fecha, calculado desde los movimientos reales.
     * La ventana del día es semiabierta: antes terminaba en 23:59:59 y perdía cualquier
     * movimiento dentro de ese último segundo.
     */
    private BankAccountBalanceDetailResponseDto calculateBalanceLive(
            BankAccount account,
            LocalDate fecha
    ) {

        BigDecimal saldoInicial = obtenerSaldoInicialCuenta(account, fecha);
        BankLedgerBuckets buckets = ledger.bucketsForAccount(account.getId(), fecha);
        return mapLiveBalance(account, fecha, saldoInicial, buckets);
    }

    private BankAccountBalanceDetailResponseDto emptyBalance(BankAccount account, LocalDate fecha) {
        BigDecimal zero = BigDecimal.ZERO;
        return mapLiveBalance(account, fecha, zero,
                new BankLedgerBuckets(zero, zero, zero, zero, zero, zero, zero, zero));
    }

    private BankAccountBalanceDetailResponseDto mapLiveBalance(
            BankAccount account, LocalDate fecha, BigDecimal saldoInicial, BankLedgerBuckets buckets) {
        BigDecimal totalEntradas = buckets.totalEntradas();

        BigDecimal totalSalidas = buckets.totalSalidas();

        BigDecimal saldoFinal =
                saldoInicial
                        .add(totalEntradas)
                        .subtract(totalSalidas);

        BankAccountBalanceDetailResponseDto dto =
                new BankAccountBalanceDetailResponseDto();

        dto.setBankAccountId(account.getId());
        dto.setBanco(account.getBanco());
        dto.setTitular(account.getTitular());
        dto.setNumeroCuenta(account.getNumeroCuenta());
        dto.setClabe(account.getClabe());
        dto.setFecha(fecha);
        dto.setSaldoInicial(saldoInicial);
        dto.setEntradasTransferencia(buckets.entradasTransferencia());
        dto.setEntradasDeposito(buckets.entradasDeposito());
        dto.setEntradasCheque(buckets.entradasCheque());
        dto.setTotalEntradas(totalEntradas);
        dto.setSalidasRetornos(buckets.salidasRetornos());
        dto.setSalidasCajaGeneral(buckets.salidasCajaGeneral());
        dto.setSalidasComisiones(buckets.salidasComisiones());
        dto.setEntradasTransferenciaInterna(buckets.entradasTransferenciaInterna());
        dto.setSalidasTransferenciaInterna(buckets.salidasTransferenciaInterna());
        dto.setTotalSalidas(totalSalidas);
        dto.setSaldoFinal(saldoFinal);

        return dto;
    }

    private BankAccountBalanceDetailResponseDto mapToDetailResponse(
            BankAccountDailyCut cut
    ) {

        BankAccountBalanceDetailResponseDto dto =
                new BankAccountBalanceDetailResponseDto();

        dto.setBankAccountId(cut.getBankAccount().getId());
        dto.setBanco(cut.getBankAccount().getBanco());
        dto.setTitular(cut.getBankAccount().getTitular());
        dto.setNumeroCuenta(cut.getBankAccount().getNumeroCuenta());
        dto.setClabe(cut.getBankAccount().getClabe());
        dto.setFecha(cut.getFecha());
        dto.setSaldoInicial(cut.getSaldoInicial());
        dto.setEntradasTransferencia(cut.getEntradasTransferencia());
        dto.setEntradasDeposito(cut.getEntradasDeposito());
        dto.setEntradasCheque(cut.getEntradasCheque());
        dto.setTotalEntradas(cut.getTotalEntradas());
        dto.setSalidasRetornos(cut.getSalidasRetornos());
        dto.setSalidasCajaGeneral(cut.getSalidasCajaGeneral());
        dto.setSalidasComisiones(cut.getSalidasComisiones());
        dto.setEntradasTransferenciaInterna(cut.getEntradasTransferenciaInterna());
        dto.setSalidasTransferenciaInterna(cut.getSalidasTransferenciaInterna());
        dto.setTotalSalidas(cut.getTotalSalidas());
        dto.setSaldoFinal(cut.getSaldoFinal());

        return dto;
    }

    private BigDecimal obtenerSaldoInicialCuenta(BankAccount account, LocalDate fecha) {
        var previous = bankAccountDailyCutRepository
                .findTopByBankAccountIdAndFechaBeforeOrderByFechaDesc(account.getId(), fecha);
        BigDecimal initial = previous.map(BankAccountDailyCut::getSaldoFinal).orElse(BigDecimal.ZERO);
        LocalDate gapStart = previous.map(cut -> cut.getFecha().plusDays(1))
                .orElse(account.getCreatedAt().toLocalDate());
        LocalDate gapEnd = fecha.minusDays(1);
        if (gapStart.isAfter(gapEnd)) return initial;

        // Conserva el último corte y agrega solo movimientos de los días aún sin corte.
        BigDecimal uncutChange = ledger.totals(new BankLedgerFilter(
                gapStart, gapEnd, account.getId(), null, null, null)).variacionNeta();
        return initial.add(uncutChange);
    }
}
