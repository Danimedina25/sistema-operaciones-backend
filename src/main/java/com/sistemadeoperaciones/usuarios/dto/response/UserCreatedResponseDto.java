package com.sistemadeoperaciones.usuarios.dto.response;

public class UserCreatedResponseDto {

    private Long id;
    private String nombre;
    private String correo;
    private Boolean activo;
    private String roleName;
    private Boolean debeCambiarPassword;
    private Boolean correoVerificado;
    private String activationUrl;

    public UserCreatedResponseDto() {
    }

    public UserCreatedResponseDto(Long id,
                                  String nombre,
                                  String correo,
                                  Boolean activo,
                                  String roleName,
                                  Boolean debeCambiarPassword,
                                  Boolean correoVerificado,
                                  String activationUrl) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.activo = activo;
        this.roleName = roleName;
        this.debeCambiarPassword = debeCambiarPassword;
        this.correoVerificado = correoVerificado;
        this.activationUrl = activationUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Boolean getDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public void setDebeCambiarPassword(Boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public Boolean getCorreoVerificado() {
        return correoVerificado;
    }

    public void setCorreoVerificado(Boolean correoVerificado) {
        this.correoVerificado = correoVerificado;
    }

    public String getActivationUrl() {
        return activationUrl;
    }

    public void setActivationUrl(String activationUrl) {
        this.activationUrl = activationUrl;
    }
}