package com.pos.inventario.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import com.pos.inventario.repository.ProductoRepository;
import com.pos.inventario.repository.VentaRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class DashboardController {
    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;

    public DashboardController(VentaRepository ventaRepository, ProductoRepository productoRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
    }

    @GetMapping("/admin/dashboard")
    @Transactional(readOnly = true)
    public String admin(Model model) {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        var inicio = hoy.atStartOfDay();
        var fin = hoy.plusDays(1).atStartOfDay();
        model.addAttribute("fechaResumen", hoy);
        model.addAttribute("ventasHoy", ventaRepository.countByFechaGreaterThanEqualAndFechaLessThan(inicio, fin));
        BigDecimal ingresosHoy = ventaRepository.sumarTotalDelPeriodo(inicio, fin);
        model.addAttribute("ingresosHoy", ingresosHoy);
        model.addAttribute("ingresosTotales", ventaRepository.sumarTotalVentas());
        model.addAttribute("totalVentas", ventaRepository.count());
        model.addAttribute("limiteStock", 5);
        model.addAttribute("stockCritico", productoRepository.countByStockLessThanEqual(5));
        cargarGraficoIngresos(model, hoy, ingresosHoy);
        return "admin/dashboard";
    }

    private void cargarGraficoIngresos(Model model, LocalDate hoy, BigDecimal ingresosHoy) {
        List<String> dias = new ArrayList<>();
        List<String> fechas = new ArrayList<>();
        List<BigDecimal> ingresos = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);
            String dia = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"));
            dias.add(dia.substring(0, 1).toUpperCase(Locale.forLanguageTag("es-PE")) + dia.substring(1));
            fechas.add(fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            BigDecimal total = i == 0 ? ingresosHoy : ventaRepository.sumarTotalDelPeriodo(
                    fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
            ingresos.add(total == null ? BigDecimal.ZERO : total);
        }
        model.addAttribute("graficoDias", dias);
        model.addAttribute("graficoFechas", fechas);
        model.addAttribute("graficoIngresos", ingresos);
    }

    @GetMapping("/cajero/dashboard")
    public String cajero() {
        return "cajero/dashboard";
    }
}
