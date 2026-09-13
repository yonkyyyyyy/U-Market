package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.ImagenResult;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.ProductoImagenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/productos")
public class ProductoImagenController {

	private final ProductoImagenService productoImagenService;

	public ProductoImagenController(ProductoImagenService productoImagenService) {
		this.productoImagenService = productoImagenService;
	}

	@PostMapping("/{id}/imagen")
	@ResponseStatus(HttpStatus.OK)
	public ImagenResult subirImagen(@PathVariable Long id,
									@RequestParam("file") MultipartFile file,
									@AuthenticationPrincipal UsuarioPrincipal principal) {
		return productoImagenService.guardarImagen(id, principal.getUsuario(), file);
	}
}