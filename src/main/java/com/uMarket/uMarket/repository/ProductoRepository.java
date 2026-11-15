package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

	List<Producto> findByUsuario(Usuario usuario);

	List<Producto> findByEstado(String estado);

	/**
	 * Candidatos para la búsqueda en lenguaje natural: publicaciones visibles
	 * (ACTIVO), de la más reciente a la más antigua. El filtrado por relevancia
	 * se hace en memoria en {@code BusquedaNlpService}.
	 */
	List<Producto> findByEstadoOrderByCreatedAtDesc(String estado, Pageable pageable);

	/**
	 * Carga el producto con lock pesimista ({@code SELECT ... FOR UPDATE}).
	 * Lo usa la activación/rechazo del pipeline para que dos workers concurrentes
	 * no se pisen al recalcular el estado (lost update).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Producto p where p.id = :id")
	Optional<Producto> findByIdForUpdate(@Param("id") Long id);

	/**
	 * Sugerencias para una demanda: solo productos ACTIVO de otros usuarios
	 * que coincidan por categoría exacta, por texto parcial en el título o por
	 * el mismo campus (cruce espacial).
	 */
	@Query("""
			SELECT p FROM Producto p
			WHERE p.estado = 'ACTIVO'
			  AND p.usuario.id <> :excluirUsuarioId
			  AND (
			       (:categoria IS NOT NULL AND p.categoria = :categoria)
			       OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :texto, '%'))
			       OR (:campus IS NOT NULL AND LOWER(p.campus) = LOWER(:campus))
			  )
			ORDER BY p.createdAt DESC
			""")
	List<Producto> buscarSugeridosParaDemanda(
			@Param("categoria") String categoria,
			@Param("texto") String texto,
			@Param("campus") String campus,
			@Param("excluirUsuarioId") Long excluirUsuarioId,
			Pageable pageable);

	/**
	 * Migra las filas guardadas con la nomenclatura anterior ('DISPONIBLE') al
	 * estado vigente ('ACTIVO'). Idempotente; la ejecuta {@code LegacyEstadoNormalizer}
	 * al arrancar para no perder publicaciones ya existentes.
	 *
	 * @return número de filas migradas
	 */
	@Modifying
	@Query("update Producto p set p.estado = 'ACTIVO' where p.estado = 'DISPONIBLE'")
	int normalizarEstadoLegacy();

	/**
	 * Descuento atómico de stock. La validación {@code p.stock >= :cantidad} va en el WHERE,
	 * así que la BD decide en una sola sentencia y no hay ventana entre leer y escribir.
	 * {@code version = version + 1} mantiene coherente el bloqueo optimista de {@code @Version}.
	 *
	 * @return 1 si se descontó; 0 si no había stock suficiente (o el producto no existe)
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Producto p
			SET p.stock = p.stock - :cantidad,
			    p.version = p.version + 1
			WHERE p.id = :id AND p.stock >= :cantidad
			""")
	int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

}