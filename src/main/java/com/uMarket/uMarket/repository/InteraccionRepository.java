package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.InteraccionEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InteraccionRepository extends JpaRepository<InteraccionEvento, Long> {

	long countByProductoIdAndTipoEvento(Long productoId, com.uMarket.uMarket.enums.TipoEvento tipoEvento);

	@Query("select i.productoId, count(i) from InteraccionEvento i where i.tipoEvento = :tipo group by i.productoId order by count(i) desc")
	List<Object[]> topProductosPorTipo(@Param("tipo") com.uMarket.uMarket.enums.TipoEvento tipo, org.springframework.data.domain.Pageable pageable);
}
