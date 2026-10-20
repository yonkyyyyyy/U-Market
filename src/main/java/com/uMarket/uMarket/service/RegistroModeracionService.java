package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.MatrizClasificacion;
import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.enums.FuenteModeracion;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.RegistroModeracion;
import com.uMarket.uMarket.repository.RegistroModeracionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Registra y consulta las decisiones de la IA de moderación.
 *
 * <p>Dos responsabilidades:</p>
 * <ol>
 *   <li><b>Escribir los logs</b> desde el pipeline (texto e imagen). Se hace en una
 *       transacción aparte ({@link Propagation#REQUIRES_NEW}) y de forma
 *       <i>best-effort</i>: si el log falla, la moderación y la publicación siguen
 *       su curso. Es imprescindible que sea otra transacción porque, cuando el
 *       texto se rechaza, el pipeline lanza una excepción y su transacción se
 *       revierte: el log debe sobrevivir a esa reversión.</li>
 *   <li><b>Leer</b> los logs y calcular la matriz de clasificación a partir del
 *       feedback humano (quién revisó los logs desde el panel de moderación).</li>
 * </ol>
 */
@Slf4j
@Service
public class RegistroModeracionService {

    /**
     * Rango completo cuando el panel no envía fechas. No se usan
     * {@code LocalDateTime.MIN/MAX} porque quedan fuera del rango de {@code timestamp}
     * de PostgreSQL.
     */
    private static final LocalDateTime DESDE_SIEMPRE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime HASTA_SIEMPRE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final RegistroModeracionRepository registroRepository;
    private final String modeloTexto;
    private final String modeloVision;

    public RegistroModeracionService(RegistroModeracionRepository registroRepository,
                                     @Value("${openai.model:omni-moderation-latest}") String modeloTexto,
                                     @Value("${openai.vision-model:gpt-4o-mini}") String modeloVision) {
        this.registroRepository = registroRepository;
        this.modeloTexto = modeloTexto;
        this.modeloVision = modeloVision;
    }

    /** Log de una decisión de texto (título + descripción). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarTexto(Long productoId, String productoTitulo, Long usuarioId,
                               ModerationResultDTO resultado, String texto, String motivo, long latenciaMs) {
        RegistroModeracion registro = nuevo(TipoModeracion.TEXTO, FuenteModeracion.PUBLICACION,
                productoId, productoTitulo, usuarioId, resultado, motivo, latenciaMs);
        registro.setModelo(modeloTexto);
        registro.setExtracto(extracto(texto));
        registroRepository.save(registro);
    }

    /** Log de una decisión de visión sobre una imagen del producto. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarImagen(Long productoId, String productoTitulo, Long usuarioId,
                                ModerationResultDTO resultado, String motivo, long latenciaMs) {
        RegistroModeracion registro = nuevo(TipoModeracion.IMAGEN, FuenteModeracion.IMAGEN,
                productoId, productoTitulo, usuarioId, resultado, motivo, latenciaMs);
        registro.setModelo(modeloVision);
        registroRepository.save(registro);
    }

    /**
     * Log de una imagen descartada por caducar en staging (la IA nunca respondió).
     * No hay resultado que registrar: el veredicto es {@code FALLIDO}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarTtl(Long productoId, String productoTitulo, Long usuarioId) {
        RegistroModeracion registro = new RegistroModeracion();
        registro.setTipo(TipoModeracion.IMAGEN);
        registro.setFuente(FuenteModeracion.TTL);
        registro.setProductoId(productoId);
        registro.setProductoTitulo(productoTitulo);
        registro.setUsuarioId(usuarioId);
        registro.setVeredicto(VeredictoModeracion.FALLIDO);
        registro.setMotivo("La imagen caducó en staging sin respuesta de la IA");
        registro.setModelo(modeloVision);
        registro.setCreatedAt(LocalDateTime.now());
        registroRepository.save(registro);
    }

    /** Lista los logs para el panel, con filtros opcionales por tipo y veredicto. */
    @Transactional(readOnly = true)
    public Page<RegistroModeracion> listar(TipoModeracion tipo, VeredictoModeracion veredicto, Pageable pageable) {
        if (tipo != null && veredicto != null) {
            return registroRepository.findByTipoAndVeredicto(tipo, veredicto, pageable);
        }
        if (tipo != null) {
            return registroRepository.findByTipo(tipo, pageable);
        }
        if (veredicto != null) {
            return registroRepository.findByVeredicto(veredicto, pageable);
        }
        return registroRepository.findAll(pageable);
    }

    /**
     * Matriz de clasificación en vivo. Solo cuentan los registros revisados por un
     * humano; los {@code FALLIDO} se informan aparte porque no son una decisión.
     *
     * @param desde inicio del rango, o {@code null} para "desde siempre"
     * @param hasta fin del rango, o {@code null} para "hasta ahora"
     */
    @Transactional(readOnly = true)
    public MatrizClasificacion calcularMatriz(LocalDateTime desde, LocalDateTime hasta) {
        // Las fechas se resuelven aquí y nunca se pasa null a la consulta: comparar
        // un parámetro contra NULL en JPQL sólo funciona en H2, PostgreSQL lo rechaza.
        LocalDateTime inicio = desde != null ? desde : DESDE_SIEMPRE;
        LocalDateTime fin = hasta != null ? hasta : HASTA_SIEMPRE;

        long total = 0;
        long fallidos = 0;
        for (Object[] fila : registroRepository.contarPorVeredicto(inicio, fin)) {
            VeredictoModeracion veredicto = (VeredictoModeracion) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            total += cantidad;
            if (veredicto == VeredictoModeracion.FALLIDO) {
                fallidos = cantidad;
            }
        }

        long vp = 0;
        long fp = 0;
        long fn = 0;
        long vn = 0;
        for (Object[] fila : registroRepository.contarRevisadosPorVeredictoYAcierto(inicio, fin)) {
            VeredictoModeracion veredicto = (VeredictoModeracion) fila[0];
            boolean acierto = Boolean.TRUE.equals(fila[1]);
            long cantidad = ((Number) fila[2]).longValue();
            // La clase positiva es "contenido prohibido": RECHAZADO ⇒ positivo.
            if (veredicto == VeredictoModeracion.RECHAZADO) {
                if (acierto) {
                    vp += cantidad;
                } else {
                    fp += cantidad;
                }
            } else if (veredicto == VeredictoModeracion.APROBADO) {
                if (acierto) {
                    vn += cantidad;
                } else {
                    fn += cantidad;
                }
            }
        }

        long medibles = total - fallidos;
        long revisados = vp + fp + fn + vn;
        long pendientes = Math.max(0, medibles - revisados);
        return MatrizClasificacion.de(vp, fp, fn, vn, pendientes, fallidos, total);
    }

    /** Guarda el feedback humano de un log: alimenta la matriz de clasificación. */
    @Transactional
    public RegistroModeracion revisar(Long id, boolean aciertoHumano, String comentario, Long moderadorId) {
        RegistroModeracion registro = registroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de moderación no encontrado con id: " + id));
        registro.setAciertoHumano(aciertoHumano);
        registro.setRevisadoPor(moderadorId);
        registro.setFechaRevision(LocalDateTime.now());
        registro.setComentarioRevision(comentario == null || comentario.isBlank() ? null : comentario.trim());
        return registroRepository.save(registro);
    }

    private RegistroModeracion nuevo(TipoModeracion tipo, FuenteModeracion fuente,
                                     Long productoId, String productoTitulo, Long usuarioId,
                                     ModerationResultDTO resultado, String motivo, long latenciaMs) {
        RegistroModeracion registro = new RegistroModeracion();
        registro.setTipo(tipo);
        registro.setFuente(fuente);
        registro.setProductoId(productoId);
        registro.setProductoTitulo(recortar(productoTitulo, 150));
        registro.setUsuarioId(usuarioId);
        registro.setVeredicto(VeredictoModeracion.desde(resultado.getStatus()));
        registro.setScore(resultado.getConfidenceScore());
        registro.setCategorias(categorias(resultado));
        registro.setMotivo(motivo);
        registro.setLatenciaMs(latenciaMs);
        registro.setCreatedAt(LocalDateTime.now());
        return registro;
    }

    private String categorias(ModerationResultDTO resultado) {
        List<String> detectadas = resultado.getCategoriasDetectadas();
        if (detectadas == null || detectadas.isEmpty()) {
            return null;
        }
        return recortar(String.join(", ", detectadas), 255);
    }

    /**
     * Guarda solo un extracto del texto (no el contenido completo) y con los
     * espacios colapsados, para poder auditar sin almacenar de más.
     */
    private String extracto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return recortar(texto.trim().replaceAll("\\s+", " "), RegistroModeracion.MAX_EXTRACTO);
    }

    private String recortar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
