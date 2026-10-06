package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.dto.ProductoRequest;
import com.uMarket.uMarket.enums.ModerationStatus;
import com.uMarket.uMarket.exception.ContenidoInapropiadoException;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.ArchivoMultimedia;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ArchivoMultimediaRepository;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProductoService {

	private static final Set<String> ESTADOS_VALIDOS = Set.of("DISPONIBLE", "VENDIDO", "PAUSADO");

	private final ProductoRepository productoRepository;
	private final ArchivoMultimediaRepository archivoMultimediaRepository;
	private final CloudinaryService cloudinaryService;
	private final ImageProcessingService imageProcessingService;
	private final OpenAIModeracionService openAIModeracionService;

	public ProductoService(ProductoRepository productoRepository,
								ArchivoMultimediaRepository archivoMultimediaRepository,
								CloudinaryService cloudinaryService,
								ImageProcessingService imageProcessingService,
								OpenAIModeracionService openAIModeracionService) {
		this.productoRepository = productoRepository;
		this.archivoMultimediaRepository = archivoMultimediaRepository;
		this.cloudinaryService = cloudinaryService;
		this.imageProcessingService = imageProcessingService;
		this.openAIModeracionService = openAIModeracionService;
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
		// Tarea IA: revisión de contenido ANTES de guardar.
		// Si el texto tiene insultos/denigraciones/palabras mayores → se rechaza (422).
		verificarContenido(request);

		Producto producto = new Producto();
		producto.setUsuario(usuario);
		producto.setTitulo(request.titulo().trim());
		producto.setDescripcion(request.descripcion());
		producto.setPrecio(request.precio());
		producto.setCategoria(normalizarCategoria(request.categoria()));
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
		producto.setCategoria(normalizarCategoria(request.categoria()));
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

	private String normalizarCategoria(String categoria) {
		if (categoria == null || categoria.isBlank()) {
			return null;
		}
		return categoria.trim().toUpperCase(Locale.ROOT);
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

	/**
	 * Revisa que el contenido a publicar (título + descripción) no contenga
	 * lenguaje inapropiado. Si lo contiene, lanza {@code ContenidoInapropiadoException}
	 * y la publicación NO se crea. Si OpenAI falla (fail-open), se permite publicar.
	 */
	private void verificarContenido(ProductoRequest request) {
		String texto = (request.titulo() == null ? "" : request.titulo().trim())
				+ " " + (request.descripcion() == null ? "" : request.descripcion().trim());
		if (texto.isBlank()) {
			return;
		}
		ModerationResultDTO resultado = openAIModeracionService.analizar(texto);
		if (resultado.getStatus() == ModerationStatus.REJECTED) {
			throw new ContenidoInapropiadoException(resultado);
		}
	}

	@Transactional
	public String subirImagenProducto(Long productoId, MultipartFile file, Usuario usuario) {
		try {
			Producto producto = obtenerEntidad(productoId);
			verificarPropietario(producto, usuario);

			File processedFile = imageProcessingService.compressAndResizeImage(file);
			Map uploadResult = cloudinaryService.uploadFile(processedFile);

			ArchivoMultimedia archivo = new ArchivoMultimedia();
			archivo.setProducto(producto);
			archivo.setUrl(uploadResult.get("secure_url").toString());
			archivo.setPublicId(uploadResult.get("public_id").toString());
			archivo.setFormato(uploadResult.get("format").toString());
			archivo.setSizeBytes(Long.valueOf(uploadResult.get("bytes").toString()));
			archivoMultimediaRepository.save(archivo);

			return archivo.getUrl();
		} catch (IOException e) {
			throw new RuntimeException("Error al procesar la imagen", e);
		}
	}
}