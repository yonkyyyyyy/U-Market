package com.uMarket.uMarket.model;

import com.uMarket.uMarket.enums.EstadoModeracion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Imagen de un producto que está siendo revisada por el pipeline asíncrono.
 *
 * <p>Mientras vive aquí, la imagen está en disco local (staging) y NO en
 * Cloudinary. El worker la analiza; si la aprueba, se crea el
 * {@link ArchivoMultimedia} (ya con la URL pública) y esta fila pasa a
 * {@code APROBADO}. Si la rechaza, nunca toca Cloudinary.</p>
 */
@Entity
@Table(name = "imagenes_en_revision")
@Getter
@Setter
@NoArgsConstructor
public class ImagenEnRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    /** Identificador del archivo dentro del {@code StorageService} (staging). */
    @Column(name = "staging_path", nullable = false)
    private String stagingPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_moderacion", nullable = false, length = 20)
    private EstadoModeracion estadoModeracion = EstadoModeracion.PENDIENTE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
