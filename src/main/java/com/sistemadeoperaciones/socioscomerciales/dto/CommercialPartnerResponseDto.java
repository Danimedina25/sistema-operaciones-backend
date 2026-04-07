package com.sistemadeoperaciones.socioscomerciales.dto;

public class CommercialPartnerResponseDto {

    private Long id;
    private String nombre;
    private String correo;
    private String telefono;
    private Boolean activo;
    private Long socioPadreId;
    private String socioPadreNombre;

    public CommercialPartnerResponseDto() {
    }

    public CommercialPartnerResponseDto(Long id, String nombre, String correo, String telefono,
                                        Boolean activo, Long socioPadreId, String socioPadreNombre) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.activo = activo;
        this.socioPadreId = socioPadreId;
        this.socioPadreNombre = socioPadreNombre;
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

    public Long getSocioPadreId() {
        return socioPadreId;
    }

    public String getSocioPadreNombre() {
        return socioPadreNombre;
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

    public void setSocioPadreId(Long socioPadreId) {
        this.socioPadreId = socioPadreId;
    }

    public void setSocioPadreNombre(String socioPadreNombre) {
        this.socioPadreNombre = socioPadreNombre;
    }
}