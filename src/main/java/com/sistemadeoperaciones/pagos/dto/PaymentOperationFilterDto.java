package com.sistemadeoperaciones.pagos.dto;

import com.sistemadeoperaciones.pagos.enums.OperationDateFilter;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

public class PaymentOperationFilterDto {

    private String search;
    private OperationStatus status;
    private Long socioComercialId;
    private OperationDateFilter dateFilter;
    private String activo;

    /**
     * Filtra operaciones que tengan al menos un pago con alguno de estos
     * tipos (ej. todos los "bancarios" = TRANSFERENCIA,DEPOSITO,CHEQUE).
     */
    private List<PaymentType> paymentTypes;

    /**
     * Filtra operaciones que tengan al menos un pago con este estatus.
     */
    private PaymentStatus paymentStatus;

    /**
     * Filtra operaciones cuyo(s) retorno(s) tengan alguno de estos
     * estatus. Usado solo por los listados de retornos solicitados.
     */
    private List<ReturnPaymentStatus> returnStatuses;

    /**
     * Filtra operaciones que tengan al menos un pago hacia esta cuenta
     * destino exacta.
     */
    private Long cuentaDestinoId;

    /**
     * Filtra operaciones que tengan al menos un pago hacia una cuenta de
     * este banco.
     */
    private String banco;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public void setStatus(OperationStatus status) {
        this.status = status;
    }

    public Long getSocioComercialId() {
        return socioComercialId;
    }

    public void setSocioComercialId(Long socioComercialId) {
        this.socioComercialId = socioComercialId;
    }


    public OperationDateFilter getDateFilter() {
        return dateFilter;
    }

    public void setDateFilter(OperationDateFilter dateFilter) {
        this.dateFilter = dateFilter;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getActivo() {
        return activo;
    }

    public void setActivo(String activo) {
        this.activo = activo;
    }

    public List<PaymentType> getPaymentTypes() {
        return paymentTypes;
    }

    public void setPaymentTypes(List<PaymentType> paymentTypes) {
        this.paymentTypes = paymentTypes;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public List<ReturnPaymentStatus> getReturnStatuses() {
        return returnStatuses;
    }

    public void setReturnStatuses(List<ReturnPaymentStatus> returnStatuses) {
        this.returnStatuses = returnStatuses;
    }

    public Long getCuentaDestinoId() {
        return cuentaDestinoId;
    }

    public void setCuentaDestinoId(Long cuentaDestinoId) {
        this.cuentaDestinoId = cuentaDestinoId;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }
}