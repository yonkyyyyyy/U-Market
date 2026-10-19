package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import com.uMarket.uMarket.model.RegistroModeracion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Logs de moderación. Además de la auditoría, alimenta la matriz de clasificación
 * en vivo: solo cuentan los registros revisados por un humano
 * ({@code aciertoHumano} no nulo).
 *
 * <p><b>El rango de fechas es obligatorio</b> (nunca {@code null}): comparar un
 * parámetro contra {@code NULL} dentro del JPQL ({@code :desde is null or ...})
 * funciona en H2 pero PostgreSQL lo rechaza con «could not determine data type of
 * parameter». Quien no quiera filtrar debe pasar el rango completo
 * ({@link com.uMarket.uMarket.service.RegistroModeracionService} lo hace).</p>
 */
public interface RegistroModeracionRepository extends JpaRepository<RegistroModeracion, Long> {

    Page<RegistroModeracion> findByTipo(TipoModeracion tipo, Pageable pageable);

    Page<RegistroModeracion> findByVeredicto(VeredictoModeracion veredicto, Pageable pageable);

    Page<RegistroModeracion> findByTipoAndVeredicto(TipoModeracion tipo, VeredictoModeracion veredicto, Pageable pageable);

    /** Todos los registros del rango agrupados por veredicto (para totales y fallidos). */
    @Query("""
            select r.veredicto, count(r)
            from RegistroModeracion r
            where r.createdAt between :desde and :hasta
            group by r.veredicto
            """)
    List<Object[]> contarPorVeredicto(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /**
     * Registros YA revisados por un humano, agrupados por veredicto de la IA y por
     * si el humano lo confirmó. Con esto se arma la matriz de clasificación.
     */
    @Query("""
            select r.veredicto, r.aciertoHumano, count(r)
            from RegistroModeracion r
            where r.aciertoHumano is not null
              and r.createdAt between :desde and :hasta
            group by r.veredicto, r.aciertoHumano
            """)
    List<Object[]> contarRevisadosPorVeredictoYAcierto(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
