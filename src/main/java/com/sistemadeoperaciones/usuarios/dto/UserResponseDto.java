package com.sistemadeoperaciones.usuarios.dto;

public class UserResponseDto {

    private Long id;
    private String nombre;
    private String correo;
    private Boolean activo;
    private Long roleId;
    private String roleName;

    public UserResponseDto() {
    }

    public UserResponseDto(Long id, String nombre, String correo, Boolean activo, Long roleId, String roleName) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.activo = activo;
        this.roleId = roleId;
        this.roleName = roleName;
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

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
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

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}