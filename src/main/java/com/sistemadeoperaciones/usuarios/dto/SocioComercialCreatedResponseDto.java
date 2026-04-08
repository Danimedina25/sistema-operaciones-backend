package com.sistemadeoperaciones.usuarios.dto;

public class SocioComercialCreatedResponseDto {

    private Long id;
    private String nombre;
    private String correo;
    private Boolean activo;
    private String roleName;
    private String activationUrl;
    private Boolean debeCambiarPassword;
    private Boolean correoVerificado;

    public SocioComercialCreatedResponseDto() {
    }

    public SocioComercialCreatedResponseDto(Long id, String nombre, String correo, Boolean activo,
                                            String roleName, String activationUrl,
                                            Boolean debeCambiarPassword, Boolean correoVerificado) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.activo = activo;
        this.roleName = roleName;
        this.activationUrl = activationUrl;
        this.debeCambiarPassword = debeCambiarPassword;
        this.correoVerificado = correoVerificado;
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

    public Boolean getActivo() {
        return activo;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getActivationUrl() {
        return activationUrl;
    }

    public Boolean getDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public Boolean getCorreoVerificado() {
        return correoVerificado;
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

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setActivationUrl(String activationUrl) {
        this.activationUrl = activationUrl;
    }

    public void setDebeCambiarPassword(Boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public void setCorreoVerificado(Boolean correoVerificado) {
        this.correoVerificado = correoVerificado;
    }
}