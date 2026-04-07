package com.sistemadeoperaciones.socioscomerciales.dto;

import java.util.ArrayList;
import java.util.List;

public class CommercialPartnerNetworkDto {

    private Long id;
    private String nombre;
    private String correo;
    private String telefono;
    private Boolean activo;
    private List<CommercialPartnerNetworkDto> hijos = new ArrayList<>();

    public CommercialPartnerNetworkDto() {
    }

    public CommercialPartnerNetworkDto(Long id, String nombre, String correo, String telefono, Boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public Boolean getActivo() {
        return activo;
    }

    public List<CommercialPartnerNetworkDto> getHijos() {
        return hijos;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setHijos(List<CommercialPartnerNetworkDto> hijos) {
        this.hijos = hijos;
    }
}