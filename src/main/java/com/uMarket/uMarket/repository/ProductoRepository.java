package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

	List<Producto> findByUsuario(Usuario usuario);

	List<Producto> findByEstado(String estado);

	/**
	 * Sugerencias para una demanda: solo productos DISPONIBLE de otros usuarios
	 * que coincidan por categoría exacta o por texto parcial en el título.
	 */
	@Query("""
			SELECT p FROM Producto p
			WHERE p.estado = 'DISPONIBLE'
			  AND p.usuario.id <> :excluirUsuarioId
			  AND (
			       (:categoria IS NOT NULL AND p.categoria = :categoria)
			       OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :texto, '%'))
			  )
			ORDER BY p.createdAt DESC
			""")
	List<Producto> buscarSugeridosParaDemanda(
			@Param("categoria") String categoria,
			@Param("texto") String texto,
			@Param("excluirUsuarioId") Long excluirUsuarioId,
			Pageable pageable);

}