package com.sistemadeoperaciones.cajageneral.model;
import jakarta.persistence.*;
@Entity
@Table(name = "cash_general_register")
public class CashGeneralRegister {
    @Id private Long id;
    public Long getId() { return id; }
}
