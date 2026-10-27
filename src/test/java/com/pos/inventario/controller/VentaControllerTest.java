package com.pos.inventario.controller;

import com.pos.inventario.entity.Producto;
import com.pos.inventario.repository.ProductoRepository;
import com.pos.inventario.service.VentaService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VentaControllerTest {
    private static class Cobro extends VentaService {
        int llamadas;
        Map<Long, Integer> recibido;
        boolean fallar;
        Cobro() { super(null, null, null, null); }
        @Override public Long procesar(Map<Long, Integer> carrito, String pago, String email) {
            llamadas++;
            if (fallar) throw new IllegalArgumentException("Stock insuficiente");
            recibido = carrito;
            return 77L;
        }
    }

    private VentaController controlador(Cobro cobro) {
        Producto p = new Producto(); p.setId(1L); p.setStock(5);
        ProductoRepository repo = (ProductoRepository) Proxy.newProxyInstance(
                ProductoRepository.class.getClassLoader(), new Class<?>[]{ProductoRepository.class},
                (proxy, method, args) -> Optional.of(p));
        return new VentaController(repo, null, cobro);
    }

    private VentaController.Carrito carrito(MockHttpSession session) {
        return (VentaController.Carrito) session.getAttribute("carritoPOS");
    }

    @Test
    void acumulaCantidadesYRechazaExcederStock() {
        VentaController controller = controlador(new Cobro());
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        controller.agregar(1L, 2, session, flash);
        controller.agregar(1L, 2, session, flash);
        assertEquals(4, carrito(session).cantidades.get(1L));
        controller.agregar(1L, 2, session, flash);
        assertEquals(4, carrito(session).cantidades.get(1L));
        assertTrue(flash.getFlashAttributes().containsKey("error"));
    }

    @Test
    void dobleEnvioCobraUnaSolaVezYVaciaCarrito() {
        Cobro cobro = new Cobro(); VentaController controller = controlador(cobro);
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        controller.agregar(1L, 2, session, flash);
        String token = carrito(session).token;
        assertEquals("redirect:/ventas/nueva", controller.procesar("Yape", token, session, () -> "caja@pos.test", flash));
        assertEquals(77L, flash.getFlashAttributes().get("ventaExitosa"));
        assertEquals("redirect:/ventas/nueva", controller.procesar("Yape", token, session, () -> "caja@pos.test", flash));
        assertEquals(1, cobro.llamadas);
        assertEquals(Map.of(1L, 2), cobro.recibido);
        assertTrue(carrito(session).cantidades.isEmpty());
    }

    @Test
    void rechazaCobroDeUnaPestanaConCarritoDesactualizado() {
        Cobro cobro = new Cobro(); VentaController controller = controlador(cobro);
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        controller.agregar(1L, 1, session, flash);
        String anterior = carrito(session).token;
        controller.agregar(1L, 1, session, flash);
        assertEquals("redirect:/ventas/nueva", controller.procesar("Efectivo", anterior, session, () -> "caja@pos.test", flash));
        assertEquals(0, cobro.llamadas);
        assertEquals(2, carrito(session).cantidades.get(1L));
    }

    @Test
    void conservaCarritoCuandoFallaElCobro() {
        Cobro cobro = new Cobro(); cobro.fallar = true;
        VentaController controller = controlador(cobro);
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        controller.agregar(1L, 1, session, flash);
        String token = carrito(session).token;
        assertEquals("redirect:/ventas/nueva", controller.procesar("Tarjeta", token, session, () -> "caja@pos.test", flash));
        assertEquals(Map.of(1L, 1), carrito(session).cantidades);
        assertEquals(token, carrito(session).token);
        assertEquals("Stock insuficiente", flash.getFlashAttributes().get("error"));
        assertFalse(flash.getFlashAttributes().containsKey("ventaExitosa"));
    }
}
