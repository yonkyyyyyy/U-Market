package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.enums.EstadoModeracion;
import com.uMarket.uMarket.model.ImagenEnRevision;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImagenEnRevisionRepository extends JpaRepository<ImagenEnRevision, Long> {

    List<ImagenEnRevision> findByProductoId(Long productoId);

    /**
     * Carga la imagen con lock pesimista ({@code SELECT ... FOR UPDATE}).
     * Serializa a dos workers que intenten procesar la MISMA imagen (mensaje
     * duplicado de RabbitMQ) para que no la suban dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ImagenEnRevision i where i.id = :id")
    Optional<ImagenEnRevision> findByIdForUpdate(@Param("id") Long id);

    /** Imágenes que siguen en revisión y llevan demasiado tiempo (para limpieza). */
    List<ImagenEnRevision> findByEstadoModeracionAndCreatedAtBefore(EstadoModeracion estado, LocalDateTime limite);
}
