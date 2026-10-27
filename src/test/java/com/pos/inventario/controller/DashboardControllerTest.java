package com.pos.inventario.controller;

import com.pos.inventario.entity.Venta;
import com.pos.inventario.repository.ProductoRepository;
import com.pos.inventario.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;

class DashboardControllerTest {
    @SuppressWarnings("unchecked")
    private <T> T repositorio(Class<T> tipo, BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo},
                (proxy, method, args) -> handler.apply(method.getName(), args));
    }

    @Test
    void muestraEstadisticasYUsaUnIntervaloDeDiaCompleto() {
        LocalDateTime[] periodo = new LocalDateTime[2];
        VentaRepository ventas = repositorio(VentaRepository.class, (metodo, args) -> {
            return switch (metodo) {
                case "count" -> 20L;
                case "countByFechaGreaterThanEqualAndFechaLessThan" -> {
                    periodo[0] = (LocalDateTime) args[0]; periodo[1] = (LocalDateTime) args[1]; yield 3L;
                }
                case "sumarTotalDelPeriodo" -> {
                    LocalDateTime inicio = (LocalDateTime) args[0];
                    assertEquals(inicio.plusDays(1), args[1]);
                    if (inicio.equals(periodo[0])) yield new BigDecimal("125.50");
                    if (inicio.equals(periodo[0].minusDays(2))) yield new BigDecimal("40.25");
                    yield BigDecimal.ZERO;
                }
                case "sumarTotalVentas" -> new BigDecimal("1800.25");
                default -> throw new AssertionError(metodo);
            };
        });
        ProductoRepository productos = repositorio(ProductoRepository.class, (metodo, args) -> {
            assertEquals("countByStockLessThanEqual", metodo); assertEquals(5, args[0]); return 2L;
        });
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("admin/dashboard", new DashboardController(ventas, productos).admin(model));
        LocalDate fecha = (LocalDate) model.get("fechaResumen");
        assertEquals(fecha.atStartOfDay(), periodo[0]);
        assertEquals(fecha.plusDays(1).atStartOfDay(), periodo[1]);
        assertEquals(3L, model.get("ventasHoy"));
        assertEquals(20L, model.get("totalVentas"));
        assertEquals(new BigDecimal("125.50"), model.get("ingresosHoy"));
        assertEquals(new BigDecimal("1800.25"), model.get("ingresosTotales"));
        assertEquals(2L, model.get("stockCritico"));
        assertEquals(List.of(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("40.25"), BigDecimal.ZERO,
                new BigDecimal("125.50")), model.get("graficoIngresos"));
        assertEquals(5, ((List<?>) model.get("graficoDias")).size());
        assertEquals(fecha.minusDays(4).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                ((List<?>) model.get("graficoFechas")).getFirst());
    }

    @Test
    void historialEntregaVentasOrdenadasSinAlterarSusDatos() {
        Venta venta = new Venta(); venta.setId(10L); venta.setTotal(new BigDecimal("22.50"));
        List<Venta> lista = List.of(venta);
        VentaRepository ventas = repositorio(VentaRepository.class, (metodo, args) -> {
            assertEquals("findAllByOrderByFechaDescIdDesc", metodo); return lista;
        });
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("ventas/historial", new ReporteController(ventas).historial(model));
        assertSame(lista, model.get("ventas"));
    }
}
