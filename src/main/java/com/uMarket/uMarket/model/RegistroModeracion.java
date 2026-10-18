package com.uMarket.uMarket.model;

import com.uMarket.uMarket.enums.FuenteModeracion;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Una decisión de moderación (texto o imagen) guardada como log auditable.
 *
 * <p>Es la fuente de dos cosas a la vez:</p>
 * <ul>
 *   <li>los <b>logs</b> que consume el panel de moderación
 *       ({@code GET /api/admin/moderacion/ia/logs});</li>
 *   <li>la <b>matriz de clasificación</b> en vivo
 *       ({@code GET /api/admin/moderacion/ia/metricas}): cuando un moderador
 *       marca {@link #aciertoHumano}, el registro pasa a contar como
 *       verdadero/falso positivo/negativo.</li>
 * </ul>
 *
 * <p>Se guardan ids sueltos (no relaciones) a propósito: el log debe sobrevivir
 * al borrado del producto o del usuario, y no debe arrastrar cargas perezosas al
 * serializarse en el panel. El título se guarda como copia (snapshot) para poder
 * mostrar el log sin joins.</p>
 */
@Entity
@Table(name = "moderacion_logs")
@Getter
@Setter
@NoArgsConstructor
public class RegistroModeracion {

    /** Cuántos caracteres del texto analizado se guardan (privacidad: no el texto completo). */
    public static final int MAX_EXTRACTO = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoModeracion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FuenteModeracion fuente;

    /** Producto afectado. Puede ser {@code null} si el texto se rechazó antes de crearlo. */
    @Column(name = "producto_id")
    private Long productoId;

    /** Copia del título en el momento del análisis (sobrevive al borrado del producto). */
    @Column(name = "producto_titulo", length = 150)
    private String productoTitulo;

    /** Dueño del contenido moderado. */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VeredictoModeracion veredicto;

    /** Confianza que devolvió la IA (0..1). */
    @Column(nullable = false)
    private double score;

    /** Categorías detectadas, separadas por coma (vacío si no hubo). */
    @Column(length = 255)
    private String categorias;

    /** Por qué se rechazó (o por qué falló) en lenguaje legible. */
    @Column(columnDefinition = "TEXT")
    private String motivo;

    /** Modelo de IA que tomó la decisión (trazabilidad). */
    @Column(length = 60)
    private String modelo;

    /** Cuánto tardó el análisis (ms), para vigilar latencias del pipeline. */
    @Column(name = "latencia_ms")
    private long latenciaMs;

    /** Extracto truncado del texto revisado; {@code null} en moderación de imagen. */
    @Column(length = MAX_EXTRACTO)
    private String extracto;

    /**
     * Revisión humana: {@code null} = pendiente; {@code true} = el humano confirma
     * el veredicto de la IA; {@code false} = el humano lo corrige. Con esto se
     * derivan los verdaderos/falsos positivos y negativos de la matriz.
     */
    @Column(name = "acierto_humano")
    private Boolean aciertoHumano;

    @Column(name = "revisado_por")
    private Long revisadoPor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "comentario_revision", columnDefinition = "TEXT")
    private String comentarioRevision;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
