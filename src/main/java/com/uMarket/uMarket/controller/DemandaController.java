package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.DemandaRequestDTO;
import com.uMarket.uMarket.dto.DemandaResponseDTO;
import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.DemandaService;
import com.uMarket.uMarket.service.EmparejamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Demandas (Se Busca)", description = "Creación, consulta, actualización y eliminación de demandas del módulo Se Busca")
public class DemandaController {

	private final DemandaService demandaService;
	private final EmparejamientoService emparejamientoService;

	public DemandaController(DemandaService demandaService, EmparejamientoService emparejamientoService) {
		this.demandaService = demandaService;
		this.emparejamientoService = emparejamientoService;
	}

	// POST /api/demandas -> 201 Created (usuario tomado del JWT/SecurityContext en el servicio)
	@Operation(summary = "Crear una demanda",
			description = "Crea una demanda en estado ACTIVA para el usuario autenticado (extraído del JWT). "
					+ "Requiere cabecera `Authorization: Bearer <token>`.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Demanda creada correctamente"),
			@ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
			@ApiResponse(responseCode = "401", description = "Falta token JWT o es inválido")
	})
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

	// GET /api/demandas/{id}/sugerencias -> 200 OK (productos ACTIVO de otros usuarios)
	@GetMapping("/{id}/sugerencias")
	public ResponseEntity<List<ProductoDto>> sugerencias(@PathVariable Long id) {
		return ResponseEntity.ok(emparejamientoService.buscarProductosSugeridosParaDemanda(id));
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
