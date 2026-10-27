package com.pos.inventario.controller;

import com.pos.inventario.entity.Producto;
import com.pos.inventario.entity.Venta;
import com.pos.inventario.repository.ProductoRepository;
import com.pos.inventario.repository.VentaRepository;
import com.pos.inventario.service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/ventas")
public class VentaController {
    private final ProductoRepository productos;
    private final VentaRepository ventas;
    private final VentaService ventaService;

    public VentaController(ProductoRepository productos, VentaRepository ventas, VentaService ventaService) {
        this.productos = productos;
        this.ventas = ventas;
        this.ventaService = ventaService;
    }

    public static class Carrito implements Serializable {
        private static final long serialVersionUID = 1L;
        final Map<Long, Integer> cantidades = new LinkedHashMap<>();
        String token = UUID.randomUUID().toString();
        String ultimoToken;
        Long ultimaVenta;
    }

    public record Linea(Long id, String nombre, BigDecimal precio, int cantidad, BigDecimal subtotal) {}

    private Carrito carrito(HttpSession session) {
        synchronized (session) {
            Carrito carrito = (Carrito) session.getAttribute("carritoPOS");
            if (carrito == null) {
                carrito = new Carrito();
                session.setAttribute("carritoPOS", carrito);
            }
            return carrito;
        }
    }

    @GetMapping({"", "/nueva"})
    public String nueva(@RequestParam(defaultValue = "") String q, HttpSession session, Model model) {
        Carrito carrito = carrito(session);
        List<Producto> catalogo = productos.findAll();
        String busqueda = q.strip().toLowerCase(Locale.ROOT);
        model.addAttribute("productos", catalogo.stream().filter(p ->
                p.getNombre().toLowerCase(Locale.ROOT).contains(busqueda)
                || (p.getCodigoBarras() != null && p.getCodigoBarras().toLowerCase(Locale.ROOT).contains(busqueda))).toList());
        model.addAttribute("q", q);
        synchronized (carrito) {
            List<Linea> lineas = new ArrayList<>();
            for (Producto producto : catalogo) {
                Integer cantidad = carrito.cantidades.get(producto.getId());
                if (cantidad != null) lineas.add(new Linea(producto.getId(), producto.getNombre(),
                        producto.getPrecio(), cantidad, producto.getPrecio().multiply(BigDecimal.valueOf(cantidad))));
            }
            Set<Long> disponibles = new HashSet<>();
            catalogo.forEach(p -> disponibles.add(p.getId()));
            carrito.cantidades.forEach((id, cantidad) -> {
                if (!disponibles.contains(id)) lineas.add(new Linea(id, "Producto no disponible: quítalo del carrito",
                        BigDecimal.ZERO, cantidad, BigDecimal.ZERO));
            });
            model.addAttribute("lineas", lineas);
            model.addAttribute("total", lineas.stream().map(Linea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
            model.addAttribute("token", carrito.token);
        }
        return "ventas/pos";
    }

    @PostMapping("/carrito/agregar")
    public String agregar(@RequestParam Long productoId, @RequestParam(defaultValue = "1") int cantidad,
                          HttpSession session, RedirectAttributes flash) {
        Carrito carrito = carrito(session);
        synchronized (carrito) {
            Producto producto = productos.findById(productoId).orElse(null);
            long nuevaCantidad = (long) carrito.cantidades.getOrDefault(productoId, 0) + cantidad;
            if (producto == null || cantidad <= 0 || producto.getStock() == null || nuevaCantidad > producto.getStock()) {
                flash.addFlashAttribute("error", "No se pudo agregar: revisa el producto, la cantidad y el stock.");
            } else {
                carrito.cantidades.put(productoId, (int) nuevaCantidad);
                carrito.token = UUID.randomUUID().toString();
            }
        }
        return "redirect:/ventas/nueva";
    }

    @PostMapping("/carrito/quitar")
    public String quitar(@RequestParam Long productoId, HttpSession session) {
        Carrito carrito = carrito(session);
        synchronized (carrito) {
            carrito.cantidades.remove(productoId);
            carrito.token = UUID.randomUUID().toString();
        }
        return "redirect:/ventas/nueva";
    }

    @PostMapping("/procesar")
    public String procesar(@RequestParam String metodoPago, @RequestParam String token,
                           HttpSession session, Principal principal, RedirectAttributes flash) {
        Carrito carrito = carrito(session);
        synchronized (carrito) {
            if (token.equals(carrito.ultimoToken)) {
                flash.addFlashAttribute("ventaExitosa", carrito.ultimaVenta);
                return "redirect:/ventas/nueva";
            }
            if (!token.equals(carrito.token)) {
                flash.addFlashAttribute("error", "El carrito cambió. Revisa el total antes de cobrar.");
                return "redirect:/ventas/nueva";
            }
            try {
                Long id = ventaService.procesar(new LinkedHashMap<>(carrito.cantidades), metodoPago, principal.getName());
                carrito.ultimoToken = token;
                carrito.ultimaVenta = id;
                carrito.cantidades.clear();
                carrito.token = UUID.randomUUID().toString();
                flash.addFlashAttribute("ventaExitosa", id);
                return "redirect:/ventas/nueva";
            } catch (IllegalArgumentException ex) {
                flash.addFlashAttribute("error", ex.getMessage());
                return "redirect:/ventas/nueva";
            } catch (org.springframework.dao.DataAccessException ex) {
                flash.addFlashAttribute("error", "No se pudo completar la venta. Revisa el carrito e inténtalo nuevamente.");
                return "redirect:/ventas/nueva";
            }
        }
    }

    @GetMapping("/exito/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String exito(@PathVariable Long id, Principal principal, Model model) {
        Venta venta = ventas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (venta.getUsuario() == null || !venta.getUsuario().getEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("venta", venta);
        return "ventas/exito";
    }
}
