package com.pos.inventario.controller;

import com.pos.inventario.repository.VentaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class ReporteController {
    private final VentaRepository ventaRepository;

    public ReporteController(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    @GetMapping({"/ventas/historial", "/reportes"})
    @Transactional(readOnly = true)
    public String historial(Model model) {
        model.addAttribute("ventas", ventaRepository.findAllByOrderByFechaDescIdDesc());
        return "ventas/historial";
    }
}
