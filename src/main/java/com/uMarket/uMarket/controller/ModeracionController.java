package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.BloqueoDemandaRequest;
import com.uMarket.uMarket.dto.DemandaResponseDTO;
import com.uMarket.uMarket.model.AuditoriaDemanda;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.ModeracionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/moderacion/demandas")
@PreAuthorize("hasRole('ADMIN')")
public class ModeracionController {

	private final ModeracionService moderacionService;

	public ModeracionController(ModeracionService moderacionService) {
		this.moderacionService = moderacionService;
	}

	// GET /api/admin/moderacion/demandas?page=0&size=10
	// Lista TODAS las demandas (ACTIVA, CUMPLIDA, CANCELADA, BLOQUEADA) para el dashboard.
	@GetMapping
	public ResponseEntity<Page<DemandaResponseDTO>> listarTodas(
			@PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(moderacionService.listarTodas(pageable));
	}

	// PATCH /api/admin/moderacion/demandas/{id}/bloquear
	// Body: { "motivo": "Contenido inapropiado..." }
	@PatchMapping("/{id}/bloquear")
	public ResponseEntity<DemandaResponseDTO> bloquear(
			@PathVariable Long id,
			@Valid @RequestBody BloqueoDemandaRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		Long adminId = principal.getUsuario().getId();
		return ResponseEntity.ok(moderacionService.bloquear(id, adminId, request.motivo()));
	}

	// GET /api/admin/moderacion/demandas/{id}/auditoria
	// Historial de acciones de moderación sobre una demanda.
	@GetMapping("/{id}/auditoria")
	public ResponseEntity<List<AuditoriaDemanda>> historial(@PathVariable Long id) {
		return ResponseEntity.ok(moderacionService.historial(id));
	}
}
