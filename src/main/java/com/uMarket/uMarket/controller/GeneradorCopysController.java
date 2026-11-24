package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.GenerarCopyRequest;
import com.uMarket.uMarket.dto.GenerarCopyResponse;
import com.uMarket.uMarket.service.GeneradorCopysService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/copys")
public class GeneradorCopysController {

	private final GeneradorCopysService generadorCopysService;

	public GeneradorCopysController(GeneradorCopysService generadorCopysService) {
		this.generadorCopysService = generadorCopysService;
	}

	@PostMapping("/generar")
	public GenerarCopyResponse generar(@Valid @RequestBody GenerarCopyRequest request) {
		return generadorCopysService.generar(request);
	}
}
