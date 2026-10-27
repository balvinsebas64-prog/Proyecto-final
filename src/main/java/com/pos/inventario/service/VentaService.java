package com.pos.inventario.service;

import com.pos.inventario.entity.*;
import com.pos.inventario.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class VentaService {
    private final ProductoRepository productos;
    private final VentaRepository ventas;
    private final DetalleVentaRepository detalles;
    private final UsuarioRepository usuarios;

    public VentaService(ProductoRepository productos, VentaRepository ventas,
                        DetalleVentaRepository detalles, UsuarioRepository usuarios) {
        this.productos = productos;
        this.ventas = ventas;
        this.detalles = detalles;
        this.usuarios = usuarios;
    }

    @Transactional
    public Long procesar(Map<Long, Integer> carrito, String metodoPago, String email) {
        if (metodoPago == null || !Set.of("Efectivo", "Tarjeta", "Yape").contains(metodoPago)) {
            throw new IllegalArgumentException("Selecciona un método de pago válido.");
        }
        if (carrito.isEmpty()) throw new IllegalArgumentException("Agrega productos antes de cobrar.");
        Usuario usuario = usuarios.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("El usuario de la venta no está disponible."));
        List<DetalleVenta> lineas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        // Siempre bloquear en el mismo orden para reducir interbloqueos entre cajas.
        for (Long id : new TreeSet<>(carrito.keySet())) {
            Integer cantidad = carrito.get(id);
            if (cantidad == null || cantidad <= 0) throw new IllegalArgumentException("Cantidad inválida.");
            Producto producto = productos.buscarParaVenta(id)
                    .orElseThrow(() -> new IllegalArgumentException("Un producto del carrito ya no existe."));
            if (producto.getStock() == null || producto.getStock() < cantidad) {
                throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre() + ".");
            }
            if (producto.getPrecio() == null || producto.getPrecio().signum() < 0) {
                throw new IllegalArgumentException("Precio inválido para " + producto.getNombre() + ".");
            }
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            total = total.add(detalle.getSubtotal());
            lineas.add(detalle);
        }
        if (total.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("El total supera el importe máximo permitido.");
        }
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
        venta.setUsuario(usuario);
        venta.setMetodoPago(metodoPago);
        venta.setTotal(total);
        ventas.save(venta);
        for (DetalleVenta detalle : lineas) {
            detalle.setVenta(venta);
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() - detalle.getCantidad());
            productos.save(producto);
            detalles.save(detalle);
        }
        return venta.getId();
    }
}
