package com.sistemadeoperaciones.pagos.dto.retornos;

public class ReturnDestinationAccountSuggestionDto {

    private String banco;
    private String titular;
    private String cuenta;
    private String clabe;
    private Long usos;

    public ReturnDestinationAccountSuggestionDto(
            String banco,
            String titular,
            String cuenta,
            String clabe,
            Long usos
    ) {
        this.banco = banco;
        this.titular = titular;
        this.cuenta = cuenta;
        this.clabe = clabe;
        this.usos = usos;
    }

    public String getBanco() {
        return banco;
    }

    public String getTitular() {
        return titular;
    }

    public String getCuenta() {
        return cuenta;
    }

    public String getClabe() {
        return clabe;
    }

    public Long getUsos() {
        return usos;
    }
}