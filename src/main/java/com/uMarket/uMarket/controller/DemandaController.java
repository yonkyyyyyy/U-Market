package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.DemandaDto;
import com.uMarket.uMarket.dto.DemandaRequest;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.DemandaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demandas")
public class DemandaController {

	private final DemandaService demandaService;

	public DemandaController(DemandaService demandaService) {
		this.demandaService = demandaService;
	}

	@GetMapping
	public List<DemandaDto> listar() {
		return demandaService.listarTodas();
	}

	@GetMapping("/mias")
	public List<DemandaDto> listarMias(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return demandaService.listarPorUsuario(principal.getUsuario());
	}

	@GetMapping("/{id}")
	public DemandaDto obtener(@PathVariable Long id) {
		return demandaService.obtener(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DemandaDto crear(@Valid @RequestBody DemandaRequest request,
							@AuthenticationPrincipal UsuarioPrincipal principal) {
		return demandaService.crear(principal.getUsuario(), request);
	}

	@PutMapping("/{id}")
	public DemandaDto actualizar(@PathVariable Long id,
								 @Valid @RequestBody DemandaRequest request,
								 @AuthenticationPrincipal UsuarioPrincipal principal) {
		return demandaService.actualizar(id, principal.getUsuario(), request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable Long id,
						 @AuthenticationPrincipal UsuarioPrincipal principal) {
		demandaService.eliminar(id, principal.getUsuario());
	}
}