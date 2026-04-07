package com.sistemadeoperaciones.socioscomerciales.models;

import com.sistemadeoperaciones.auth.models.User;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commercial_partners")
public class CommercialPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_padre_id")
    private CommercialPartner socioPadre;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User usuario;

    @OneToMany(mappedBy = "socioPadre", cascade = CascadeType.ALL)
    private List<CommercialPartner> hijos = new ArrayList<>();

    public CommercialPartner() {
    }

    public CommercialPartner(Long id, String nombre, String correo, String telefono, Boolean activo, CommercialPartner socioPadre) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.activo = activo;
        this.socioPadre = socioPadre;
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

    public CommercialPartner getSocioPadre() {
        return socioPadre;
    }

    public List<CommercialPartner> getHijos() {
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

    public void setSocioPadre(CommercialPartner socioPadre) {
        this.socioPadre = socioPadre;
    }

    public User getUsuario() {
        return usuario;
    }

    public void setUsuario(User usuario) {
        this.usuario = usuario;
    }

    public void setHijos(List<CommercialPartner> hijos) {
        this.hijos = hijos;
    }
}