package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.dto.ProductoRequest;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.service.ProductoService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public ProductoDto obtener(@PathVariable Long id,
							   @AuthenticationPrincipal UsuarioPrincipal principal) {
		return productoService.obtener(id, principal.getUsuario());
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

	/**
	 * Recibe la imagen y la deja EN REVISIÓN. La respuesta es 202 (Accepted):
	 * la subida se aceptó, pero el resultado de la moderación es asíncrono y
	 * la imagen todavía NO está en Cloudinary ni el producto es visible.
	 */
	@PostMapping("/{id}/imagen")
	public ResponseEntity<Map<String, Object>> subirImagen(@PathVariable Long id,
										 @RequestParam("archivo") MultipartFile archivo,
										 @AuthenticationPrincipal UsuarioPrincipal principal) {
		Long imagenId = productoService.subirImagenProducto(id, archivo, principal.getUsuario());
		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "Imagen recibida; queda en revisión antes de publicarse");
		response.put("imagenId", imagenId);
		response.put("estado", "PENDIENTE");
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}

	/**
	 * Marca la publicación como "lista" (el cliente ya no subirá más contenido) y
	 * encola el pipeline asíncrono, que resolverá su estado: {@code ACTIVO} si no
	 * hay nada pendiente (p. ej. publicar sin imágenes) o {@code RECHAZADO} si algo
	 * fue rechazado. Respuesta 202: el resultado es asíncrono y hasta entonces el
	 * producto sigue {@code PENDIENTE} (no visible).
	 */
	@PostMapping("/{id}/publicar")
	public ResponseEntity<Map<String, Object>> publicar(@PathVariable Long id,
										@AuthenticationPrincipal UsuarioPrincipal principal) {
		productoService.publicar(id, principal.getUsuario());
		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "Publicación en cola; el pipeline asíncrono resolverá su estado");
		response.put("estado", "PENDIENTE");
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}
}