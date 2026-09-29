package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.DemandaRequestDTO;
import com.uMarket.uMarket.dto.DemandaResponseDTO;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.DemandaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demandas")
public class DemandaController {

	private final DemandaService demandaService;

	public DemandaController(DemandaService demandaService) {
		this.demandaService = demandaService;
	}

	// POST /api/demandas -> 201 Created (usuario tomado del JWT/SecurityContext en el servicio)
	@PostMapping
	public ResponseEntity<DemandaResponseDTO> crear(@Valid @RequestBody DemandaRequestDTO request) {
		DemandaResponseDTO creada = demandaService.crear(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(creada);
	}

	// GET /api/demandas?estado=ACTIVA&page=0&size=10 -> 200 OK
	@GetMapping
	public ResponseEntity<Page<DemandaResponseDTO>> listarTodas(
			@RequestParam(required = false) String estado,
			@PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(demandaService.listarTodas(estado, pageable));
	}

	// GET /api/demandas/mis-demandas -> 200 OK (solo las del token activo)
	@GetMapping("/mis-demandas")
	public ResponseEntity<Page<DemandaResponseDTO>> listarMisDemandas(
			@PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(demandaService.listarMisDemandas(pageable));
	}

	// Alias legacy: GET /api/demandas/mias
	@GetMapping("/mias")
	public ResponseEntity<Page<DemandaResponseDTO>> listarMias(
			@PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(demandaService.listarMisDemandas(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<DemandaResponseDTO> obtener(@PathVariable Long id) {
		return ResponseEntity.ok(demandaService.obtener(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<DemandaResponseDTO> actualizar(@PathVariable Long id,
			@Valid @RequestBody DemandaRequestDTO request) {
		return ResponseEntity.ok(demandaService.actualizar(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		demandaService.eliminar(id, principal.getUsuario());
		return ResponseEntity.noContent().build();
	}
}
