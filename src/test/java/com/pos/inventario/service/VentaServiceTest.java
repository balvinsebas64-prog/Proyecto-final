package com.pos.inventario.service;

import com.pos.inventario.entity.*;
import com.pos.inventario.repository.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;

class VentaServiceTest {
    @SuppressWarnings("unchecked")
    private static <T> T repositorio(Class<T> tipo, BiFunction<String, Object[], Object> llamadas) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo},
                (proxy, method, args) -> llamadas.apply(method.getName(), args));
    }

    private static class Caja {
        final Map<Long, Producto> productos = new HashMap<>();
        final List<Long> bloqueos = new ArrayList<>();
        final List<DetalleVenta> detalles = new ArrayList<>();
        Venta venta;
        int stockGuardado;
        final VentaService servicio;

        Caja() {
            Usuario usuario = new Usuario();
            usuario.setEmail("caja@pos.test");
            ProductoRepository pr = repositorio(ProductoRepository.class, (metodo, args) -> {
                if (metodo.equals("buscarParaVenta")) {
                    bloqueos.add((Long) args[0]);
                    return Optional.ofNullable(productos.get(args[0]));
                }
                if (metodo.equals("save")) { stockGuardado++; return args[0]; }
                throw new AssertionError(metodo);
            });
            VentaRepository vr = repositorio(VentaRepository.class, (metodo, args) -> {
                venta = (Venta) args[0]; venta.setId(42L); return venta;
            });
            DetalleVentaRepository dr = repositorio(DetalleVentaRepository.class, (metodo, args) -> {
                detalles.add((DetalleVenta) args[0]); return args[0];
            });
            UsuarioRepository ur = repositorio(UsuarioRepository.class, (metodo, args) -> Optional.of(usuario));
            servicio = new VentaService(pr, vr, dr, ur);
        }

        Producto agregar(long id, String precio, int stock) {
            Producto p = new Producto();
            p.setId(id); p.setNombre("Producto " + id); p.setPrecio(new BigDecimal(precio)); p.setStock(stock);
            productos.put(id, p);
            return p;
        }
    }

    @Test
    void calculaImportesExactosDescuentaStockYAsociaDetalles() {
        Caja caja = new Caja();
        Producto p = caja.agregar(1L, "12.35", 10);
        assertEquals(42L, caja.servicio.procesar(Map.of(1L, 3), "Yape", "caja@pos.test"));
        assertEquals(new BigDecimal("37.05"), caja.venta.getTotal());
        assertEquals(7, p.getStock());
        assertEquals("Yape", caja.venta.getMetodoPago());
        assertEquals("caja@pos.test", caja.venta.getUsuario().getEmail());
        assertEquals(1, caja.detalles.size());
        assertSame(caja.venta, caja.detalles.getFirst().getVenta());
        assertEquals(3, caja.detalles.getFirst().getCantidad());
        assertEquals(caja.venta.getTotal(), caja.detalles.getFirst().getSubtotal());
    }

    @Test
    void validaTodoElStockAntesDeModificarProductos() {
        Caja caja = new Caja();
        Producto primero = caja.agregar(1L, "5.00", 5);
        caja.agregar(2L, "8.00", 0);
        assertThrows(IllegalArgumentException.class, () -> caja.servicio.procesar(Map.of(1L, 2, 2L, 1), "Efectivo", "caja@pos.test"));
        assertEquals(5, primero.getStock());
        assertNull(caja.venta);
        assertEquals(0, caja.stockGuardado);
        assertTrue(caja.detalles.isEmpty());
    }

    @Test
    void rechazaCarritoVacioPagoInvalidoCantidadInvalidaYProductoEliminado() {
        Caja caja = new Caja();
        caja.agregar(1L, "5.00", 5);
        assertThrows(IllegalArgumentException.class, () -> caja.servicio.procesar(Map.of(), "Efectivo", "caja@pos.test"));
        assertThrows(IllegalArgumentException.class, () -> caja.servicio.procesar(Map.of(1L, 1), "Otro", "caja@pos.test"));
        assertThrows(IllegalArgumentException.class, () -> caja.servicio.procesar(Map.of(1L, -1), "Tarjeta", "caja@pos.test"));
        assertThrows(IllegalArgumentException.class, () -> caja.servicio.procesar(Map.of(999L, 1), "Tarjeta", "caja@pos.test"));
        assertNull(caja.venta);
    }

    @Test
    void bloqueaProductosEnOrdenYRechazaTotalesFueraDeRango() {
        Caja caja = new Caja();
        caja.agregar(1L, "99999999.99", 10);
        caja.agregar(2L, "1.00", 10);
        assertThrows(IllegalArgumentException.class, () -> caja.servicio.procesar(Map.of(2L, 1, 1L, 1), "Tarjeta", "caja@pos.test"));
        assertEquals(List.of(1L, 2L), caja.bloqueos);
        assertNull(caja.venta);
        assertEquals(0, caja.stockGuardado);
    }
}
