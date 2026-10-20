package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.InferenciaTextoRequest;
import com.uMarket.uMarket.dto.VeredictoIaResponse;
import com.uMarket.uMarket.service.InferenciaIaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API INTERNA de inferencia de IA: expone la moderación de texto y de imagen como
 * endpoints HTTP para consumo interno del Backend Core.
 *
 * <p>Complementa —no reemplaza— al pipeline asíncrono de moderación: el alta de
 * publicaciones sigue funcionando exactamente igual. Aquí se pide un veredicto
 * SINCRÓNICO, sin encolar nada y sin persistir nada.</p>
 *
 * <p><b>Seguridad:</b> la cadena de {@code IaInternaSecurityConfig} protege
 * {@code /api/ia/**} exigiendo el header {@code X-Internal-Api-Key} con el valor de
 * la variable de entorno {@code IA_INTERNAL_API_KEY} (no usa JWT de usuario).</p>
 *
 * <p><b>Política de fallo:</b> si la IA no responde, el veredicto es {@code FALLIDO}
 * y el campo {@code permitido} ya trae la política del tipo aplicada (texto
 * fail-open, imagen fail-closed), así el llamador no tiene que interpretarla.</p>
 *
 * <p>Límites configurables: {@code ia.limites.texto-max} (caracteres),
 * {@code ia.limites.imagen-max-bytes} e {@code ia.limites.imagen-mimes}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/ia/v1")
@Tag(name = "IA interna", description = "Inferencia de IA (texto y visión) para el Backend Core. "
		+ "Requiere el header X-Internal-Api-Key; no usa JWT.")
@SecurityRequirement(name = "apiKeyInterna")
public class InferenciaIaController {

    private final InferenciaIaService inferenciaIaService;

    private final long imagenMaxBytes;

    private final Set<String> imagenMimes;

    public InferenciaIaController(InferenciaIaService inferenciaIaService,
            @Value("${ia.limites.imagen-max-bytes:8388608}") long imagenMaxBytes,
            @Value("${ia.limites.imagen-mimes:image/jpeg,image/png,image/webp,image/gif}") String imagenMimes) {
        this.inferenciaIaService = inferenciaIaService;
        this.imagenMaxBytes = imagenMaxBytes;
        this.imagenMimes = Arrays.stream(imagenMimes.split(","))
                .map(String::trim)
                .map(mime -> mime.toLowerCase(Locale.ROOT))
                .filter(mime -> !mime.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Analiza un texto y devuelve el veredicto (fail-open si la IA falla).
     *
     * @return 200 con el veredicto · 400 si el texto viene vacío o supera el máximo
     */
    @Operation(summary = "Moderar un texto",
            description = "Analiza título/descripción y devuelve el veredicto. "
                    + "Si la IA no está disponible, veredicto=FALLIDO y permitido=true (fail-open).")
    @PostMapping(value = "/texto", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VeredictoIaResponse> inferirTexto(
            @Valid @RequestBody InferenciaTextoRequest request) {
        return ResponseEntity.ok(inferenciaIaService.inferirTexto(request.texto()));
    }

    /**
     * Analiza una imagen y devuelve el veredicto (fail-closed si la IA falla).
     *
     * <p>La imagen viaja como {@code multipart/form-data} en el campo
     * {@code archivo}.</p>
     *
     * @return 200 con el veredicto · 400 si falta o viene vacía · 413 si supera el
     *         tamaño máximo · 415 si el tipo no está soportado
     */
    @Operation(summary = "Moderar una imagen",
            description = "Analiza la foto de un producto (multipart, campo 'archivo'). "
                    + "Si la IA no está disponible, veredicto=FALLIDO y permitido=false (fail-closed).")
    @PostMapping(value = "/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> inferirImagen(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "El archivo de imagen está vacío o no se envió");
        }
        if (archivo.getSize() > imagenMaxBytes) {
            return error(HttpStatusCode.valueOf(413),
                    "La imagen supera el máximo permitido de " + imagenMaxBytes + " bytes");
        }
        String tipoMime = archivo.getContentType() == null
                ? ""
                : archivo.getContentType().toLowerCase(Locale.ROOT);
        if (!imagenMimes.contains(tipoMime)) {
            return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de imagen no soportado: '" + tipoMime + "'. Soportados: "
                            + String.join(", ", imagenMimes));
        }

        File temporal = null;
        try {
            // La extensión es importante: VisionAnalisisService deduce el MIME de la
            // imagen por el nombre del archivo para armar el data URL.
            temporal = File.createTempFile("ia_inferencia_", extensionDe(tipoMime));
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, temporal.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return ResponseEntity.ok(inferenciaIaService.inferirImagen(temporal));
        } catch (IOException e) {
            log.error("No se pudo leer la imagen enviada a la API interna de IA", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer la imagen enviada");
        } finally {
            if (temporal != null) {
                temporal.delete();
            }
        }
    }

    /** Mismo formato de error que {@code GlobalExceptionHandler}, para el llamador. */
    private ResponseEntity<Map<String, Object>> error(HttpStatusCode status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", mensaje);
        return ResponseEntity.status(status).body(body);
    }

    private static String extensionDe(String tipoMime) {
        return switch (tipoMime) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
