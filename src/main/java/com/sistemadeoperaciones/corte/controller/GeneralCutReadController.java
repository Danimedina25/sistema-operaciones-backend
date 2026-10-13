package com.sistemadeoperaciones.corte.controller;

import com.sistemadeoperaciones.corte.service.GeneralCutReadService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/general-cut")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
public class GeneralCutReadController {
    private final GeneralCutReadService service;

    public GeneralCutReadController(GeneralCutReadService service) { this.service = service; }

    @GetMapping
    public ApiResponse<GeneralCutReadService.GeneralCut> read(@RequestParam LocalDate startDate,
                                                               @RequestParam LocalDate endDate,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "30") int size) {
        return new ApiResponse<>(true, "Corte general", service.read(startDate, endDate, page, size), null);
    }
}
