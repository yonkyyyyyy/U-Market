package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.MetricasProductoResponse;
import com.uMarket.uMarket.dto.RegistrarInteraccionRequest;
import com.uMarket.uMarket.service.AnaliticaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analitica")
@Tag(name = "Analítica", description = "Métricas de clics/interacción de productos")
public class AnaliticaController {

	private final AnaliticaService analiticaService;

	public AnaliticaController(AnaliticaService analiticaService) {
		this.analiticaService = analiticaService;
	}

	@PostMapping("/interacciones")
	public ResponseEntity<Void> registrar(@Valid @RequestBody RegistrarInteraccionRequest request) {
		analiticaService.registrar(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@GetMapping("/productos/{productoId}/metricas")
	public ResponseEntity<MetricasProductoResponse> metricas(@PathVariable Long productoId) {
		return ResponseEntity.ok(analiticaService.metricas(productoId));
	}

	@GetMapping("/resumen")
	public ResponseEntity<List<MetricasProductoResponse>> resumen() {
		return ResponseEntity.ok(analiticaService.resumen());
	}
}
