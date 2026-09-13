package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.dto.ProductoRequest;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.ProductoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

	private final ProductoService productoService;

	public ProductoController(ProductoService productoService) {
		this.productoService = productoService;
	}

	@GetMapping
	public List<ProductoDto> listar(@RequestParam(required = false) String estado) {
		return productoService.listarTodos(estado);
	}

	@GetMapping("/mios")
	public List<ProductoDto> listarMios(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return productoService.listarPorUsuario(principal.getUsuario());
	}

	@GetMapping("/{id}")
	public ProductoDto obtener(@PathVariable Long id) {
		return productoService.obtener(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductoDto crear(@Valid @RequestBody ProductoRequest request,
							 @AuthenticationPrincipal UsuarioPrincipal principal) {
		return productoService.crear(principal.getUsuario(), request);
	}

	@PutMapping("/{id}")
	public ProductoDto actualizar(@PathVariable Long id,
								  @Valid @RequestBody ProductoRequest request,
								  @AuthenticationPrincipal UsuarioPrincipal principal) {
		return productoService.actualizar(id, principal.getUsuario(), request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable Long id,
						 @AuthenticationPrincipal UsuarioPrincipal principal) {
		productoService.eliminar(id, principal.getUsuario());
	}
}