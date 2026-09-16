package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.dto.BankLedgerRowDto;
import com.sistemadeoperaciones.corte.dto.BankLedgerTotalsDto;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Consulta de movimientos bancarios. Es estrictamente de lectura: no existe ninguna
 * operación para capturar entradas o salidas a mano, porque todo movimiento bancario
 * nace de un pago, de un retorno o de un cheque cobrado en Caja General.
 */
@Service
@Transactional(readOnly = true)
public class BankLedgerService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BankLedgerQuery ledger;

    public BankLedgerService(BankLedgerQuery ledger) {
        this.ledger = ledger;
    }

    public Page<BankLedgerRowDto> search(BankLedgerFilter filter, int page, int size) {
        BankLedgerFilter validated = validate(filter);
        if (page < 0) {
            throw new BadRequestException("Página inválida");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("El tamaño de página debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
        Pageable pageable = PageRequest.of(page, size);
        return ledger.search(validated, pageable);
    }

    public BankLedgerTotalsDto totals(BankLedgerFilter filter) {
        return ledger.totals(validate(filter));
    }

    /** Mismo criterio que el libro de Caja General: rango ordenado, no futuro y de máximo un año. */
    private BankLedgerFilter validate(BankLedgerFilter filter) {
        if (filter == null || filter.desde() == null || filter.hasta() == null) {
            throw new BadRequestException("Indica el rango de fechas a consultar");
        }
        LocalDate desde = filter.desde();
        LocalDate hasta = filter.hasta();
        if (hasta.isBefore(desde)) {
            throw new BadRequestException("La fecha final no puede ser anterior a la inicial");
        }
        if (hasta.isAfter(LocalDate.now())) {
            throw new BadRequestException("No se pueden consultar fechas futuras");
        }
        if (hasta.isAfter(desde.plusYears(1))) {
            throw new BadRequestException("El rango no puede superar un año");
        }
        return filter;
    }
}
