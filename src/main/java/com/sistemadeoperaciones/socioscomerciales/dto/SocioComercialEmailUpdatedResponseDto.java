package com.sistemadeoperaciones.socioscomerciales.dto;

public class SocioComercialEmailUpdatedResponseDto {

    private Long id;
    private String nombre;
    private String correo;
    private Boolean activo;
    private Boolean correoVerificado;
    private Boolean debeCambiarPassword;
    private String activationUrl;
    private String message;

    public SocioComercialEmailUpdatedResponseDto(Long id, String nombre, String correo,
                                                 Boolean activo, Boolean correoVerificado,
                                                 Boolean debeCambiarPassword,
                                                 String activationUrl, String message) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.activo = activo;
        this.correoVerificado = correoVerificado;
        this.debeCambiarPassword = debeCambiarPassword;
        this.activationUrl = activationUrl;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public Boolean getActivo() { return activo; }
    public Boolean getCorreoVerificado() { return correoVerificado; }
    public Boolean getDebeCambiarPassword() { return debeCambiarPassword; }
    public String getActivationUrl() { return activationUrl; }
    public String getMessage() { return message; }
}