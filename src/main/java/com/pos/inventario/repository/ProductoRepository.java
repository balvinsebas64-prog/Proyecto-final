package com.pos.inventario.repository;

import com.pos.inventario.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    long countByStockLessThanEqual(Integer limite);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.id = :id")
    java.util.Optional<Producto> buscarParaVenta(@org.springframework.data.repository.query.Param("id") Long id);
    @Override
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findAll();
}
