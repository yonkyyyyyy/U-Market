package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DemandaRepository extends JpaRepository<Demanda, Long> {

	List<Demanda> findByUsuario(Usuario usuario);

	List<Demanda> findByEstadoAndFechaCreacionBefore(String estado, LocalDateTime fechaLimite);

	/** Demandas activas, insumo de la demanda inversa (producto → demandas). */
	List<Demanda> findByEstado(String estado);

	/**
	 * Limita en la base de datos las demandas candidatas a una publicación antes
	 * de calcular su score. Los parámetros opcionales se envían como cadena vacía
	 * para evitar comparaciones de parámetros NULL incompatibles con PostgreSQL.
	 */
	@Query("""
			SELECT d FROM Demanda d JOIN FETCH d.usuario
			WHERE d.estado = 'ACTIVA'
			  AND d.usuario.id <> :excluirUsuarioId
			  AND (
			       (:categoria <> '' AND LOWER(d.categoria) = LOWER(:categoria))
			       OR (:texto <> '' AND (
			              LOWER(d.titulo) LIKE LOWER(CONCAT('%', :texto, '%'))
			              OR LOWER(COALESCE(d.descripcion, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
			       ))
			       OR (:campus <> '' AND LOWER(d.campus) = LOWER(:campus))
			  )
			ORDER BY d.fechaCreacion DESC
			""")
	List<Demanda> buscarCandidatasParaProducto(
			@Param("categoria") String categoria,
			@Param("texto") String texto,
			@Param("campus") String campus,
			@Param("excluirUsuarioId") Long excluirUsuarioId,
			Pageable pageable);

	Page<Demanda> findByEstado(String estado, Pageable pageable);

	Page<Demanda> findByUsuario(Usuario usuario, Pageable pageable);

}