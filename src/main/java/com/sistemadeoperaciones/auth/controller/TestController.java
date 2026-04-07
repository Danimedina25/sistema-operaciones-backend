package com.sistemadeoperaciones.auth.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/private")
    public String privado() {
        return "Acceso autorizado";
    }
}