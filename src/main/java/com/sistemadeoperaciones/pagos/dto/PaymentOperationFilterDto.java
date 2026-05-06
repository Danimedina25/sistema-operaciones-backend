package com.sistemadeoperaciones.pagos.dto;

import com.sistemadeoperaciones.pagos.enums.OperationDateFilter;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PaymentOperationFilterDto {

    private String search;
    private OperationStatus status;
    private Long socioComercialId;
    private OperationDateFilter dateFilter;

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
}