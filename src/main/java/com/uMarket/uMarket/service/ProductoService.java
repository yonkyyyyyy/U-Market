package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.dto.ProductoRequest;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProductoService {

	private static final Set<String> ESTADOS_VALIDOS = Set.of("DISPONIBLE", "VENDIDO", "PAUSADO");

	private final ProductoRepository productoRepository;

	public ProductoService(ProductoRepository productoRepository) {
		this.productoRepository = productoRepository;
	}

	@Transactional(readOnly = true)
	public List<ProductoDto> listarTodos(String estado) {
		if (estado == null || estado.isBlank()) {
			return productoRepository.findAll().stream().map(ProductoDto::from).toList();
		}
		return productoRepository.findByEstado(estado.trim().toUpperCase(Locale.ROOT))
				.stream().map(ProductoDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ProductoDto> listarPorUsuario(Usuario usuario) {
		return productoRepository.findByUsuario(usuario).stream().map(ProductoDto::from).toList();
	}

	@Transactional(readOnly = true)
	public ProductoDto obtener(Long id) {
		return ProductoDto.from(obtenerEntidad(id));
	}

	@Transactional
	public ProductoDto crear(Usuario usuario, ProductoRequest request) {
		Producto producto = new Producto();
		producto.setUsuario(usuario);
		producto.setTitulo(request.titulo().trim());
		producto.setDescripcion(request.descripcion());
		producto.setPrecio(request.precio());
		producto.setEstado(normalizarEstado(request.estado()));
		producto.setCreatedAt(LocalDateTime.now());
		return ProductoDto.from(productoRepository.save(producto));
	}

	@Transactional
	public ProductoDto actualizar(Long id, Usuario usuario, ProductoRequest request) {
		Producto producto = obtenerEntidad(id);
		verificarPropietario(producto, usuario);
		producto.setTitulo(request.titulo().trim());
		producto.setDescripcion(request.descripcion());
		producto.setPrecio(request.precio());
		producto.setEstado(normalizarEstado(request.estado()));
		return ProductoDto.from(productoRepository.save(producto));
	}

	@Transactional
	public void eliminar(Long id, Usuario usuario) {
		Producto producto = obtenerEntidad(id);
		verificarPropietario(producto, usuario);
		productoRepository.delete(producto);
	}

	private Producto obtenerEntidad(Long id) {
		return productoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
	}

	private void verificarPropietario(Producto producto, Usuario usuario) {
		if (!producto.getUsuario().getId().equals(usuario.getId())) {
			throw new AccessDeniedException("No puedes modificar un producto que no te pertenece");
		}
	}

	private String normalizarEstado(String estado) {
		if (estado == null || estado.isBlank()) {
			return "DISPONIBLE";
		}
		String normalizado = estado.trim().toUpperCase(Locale.ROOT);
		if (!ESTADOS_VALIDOS.contains(normalizado)) {
			throw new IllegalArgumentException("Estado inválido. Use: DISPONIBLE, VENDIDO o PAUSADO");
		}
		return normalizado;
	}
}