package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.MatrizClasificacion;
import com.uMarket.uMarket.dto.RevisionModeracionRequest;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import com.uMarket.uMarket.model.RegistroModeracion;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.RegistroModeracionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Panel de moderación de la IA: logs de cada decisión del pipeline y la matriz de
 * clasificación calculada con el feedback humano.
 *
 * <p>Está pensado para el equipo de moderación (rol {@code ADMINISTRADOR}), igual
 * que {@code /api/admin/moderacion/demandas}, pero separado en
 * {@code /api/admin/moderacion/ia} para no mezclar el bloqueo de demandas con la
 * moderación automática de contenido.</p>
 *
 * <p>Se aceptan ambos nombres de rol ({@code ADMINISTRADOR} es el documentado en
 * {@code docker/init.sql}; {@code ADMIN} lo usa {@code ModeracionController}) para
 * que el panel funcione con el usuario de moderación existente.</p>
 */
@RestController
@RequestMapping("/api/admin/moderacion/ia")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN')")
public class ModeracionIaController {

    private final RegistroModeracionService registroModeracionService;

    public ModeracionIaController(RegistroModeracionService registroModeracionService) {
        this.registroModeracionService = registroModeracionService;
    }

    /**
     * Logs de moderación paginados, del más reciente al más antiguo.
     *
     * @param tipo      filtra por {@code TEXTO} o {@code IMAGEN}
     * @param veredicto filtra por {@code APROBADO}, {@code RECHAZADO} o {@code FALLIDO}
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<RegistroModeracion>> listarLogs(
            @RequestParam(required = false) TipoModeracion tipo,
            @RequestParam(required = false) VeredictoModeracion veredicto,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(registroModeracionService.listar(tipo, veredicto, pageable));
    }

    /**
     * Matriz de clasificación en vivo (VP/FP/FN/VN + exactitud, precisión, recall y F1)
     * sobre los logs ya revisados por un humano. El rango de fechas es opcional.
     */
    @GetMapping("/metricas")
    public ResponseEntity<MatrizClasificacion> obtenerMatriz(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(registroModeracionService.calcularMatriz(desde, hasta));
    }

    /**
     * Registra el feedback humano sobre un log: con esto entra a la matriz de
     * clasificación ({@code aciertoHumano=true} confirma a la IA;
     * {@code false} la corrige).
     */
    @PatchMapping("/logs/{id}")
    public ResponseEntity<RegistroModeracion> revisarLog(
            @PathVariable Long id,
            @Valid @RequestBody RevisionModeracionRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        RegistroModeracion revisado = registroModeracionService.revisar(
                id, request.aciertoHumano(), request.comentario(), principal.getUsuario().getId());
        return ResponseEntity.ok(revisado);
    }
}
