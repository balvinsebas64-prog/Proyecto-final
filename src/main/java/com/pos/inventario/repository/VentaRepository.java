package com.pos.inventario.repository;

import com.pos.inventario.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    long countByFechaGreaterThanEqualAndFechaLessThan(LocalDateTime inicio, LocalDateTime fin);

    @Query("select coalesce(sum(v.total), 0) from Venta v where v.fecha >= :inicio and v.fecha < :fin")
    BigDecimal sumarTotalDelPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("select coalesce(sum(v.total), 0) from Venta v")
    BigDecimal sumarTotalVentas();

    @EntityGraph(attributePaths = "usuario")
    List<Venta> findAllByOrderByFechaDescIdDesc();
}
