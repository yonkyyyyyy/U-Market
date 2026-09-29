package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.TareaPesada;
import com.uMarket.uMarket.service.TareaPesadaProducer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint TEMPORAL para verificar la tarea 1 (workers asíncronos).
 * Encola una tarea de ejemplo y el worker la consume sin bloquear la request.
 * Se reemplazará por el pipeline real en la tarea 4.
 */
@RestController
@RequestMapping("/api/moderacion/demo")
public class ModeracionDemoController {

    private final TareaPesadaProducer producer;

    public ModeracionDemoController(TareaPesadaProducer producer) {
        this.producer = producer;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> encolarDemo(
            @RequestParam(defaultValue = "IMAGEN") String tipo) {

        TareaPesada tarea = TareaPesada.nueva(
                tipo, 1L, 1L, "https://res.cloudinary.com/umarket/demo.jpg");

        producer.publicar(tarea);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "mensaje", "Tarea encolada",
                        "tareaId", tarea.id().toString()));
    }
}