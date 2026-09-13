package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ImagenResult;
import com.uMarket.uMarket.dto.ProcessedImage;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class ProductoImagenService {

	private static final Logger log = LoggerFactory.getLogger(ProductoImagenService.class);
	private static final long MAX_TAMANO = 5L * 1024 * 1024;
	private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

	private final ProductoRepository productoRepository;
	private final ImageProcessingService imageProcessingService;

	public ProductoImagenService(ProductoRepository productoRepository, ImageProcessingService imageProcessingService) {
		this.productoRepository = productoRepository;
		this.imageProcessingService = imageProcessingService;
	}

	@Transactional
	public ImagenResult guardarImagen(Long productoId, Usuario usuario, MultipartFile archivo) {
		Producto producto = obtenerEntidad(productoId);
		verificarPropietario(producto, usuario);
		validarArchivo(archivo);

		try {
			ProcessedImage procesada = imageProcessingService.process(archivo);
			producto.setImagenUrl(procesada.mediumUrl());
			productoRepository.save(producto);
			log.info("Imagen asignada al producto id={}", productoId);
			return ImagenResult.from(procesada);
		} catch (IOException e) {
			log.error("Error procesando imagen para el producto id={}", productoId, e);
			throw new IllegalStateException("No se pudo procesar la imagen: " + e.getMessage(), e);
		}
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

	private boolean esFormatoImagenValido(MultipartFile archivo) {
		String contentType = archivo.getContentType();
		if (contentType != null && TIPOS_PERMITIDOS.contains(contentType.toLowerCase())) {
			return true;
		}
		String nombre = archivo.getOriginalFilename();
		if (nombre == null) {
			return false;
		}
		int punto = nombre.lastIndexOf('.');
		if (punto < 0 || punto == nombre.length() - 1) {
			return false;
		}
		String extension = nombre.substring(punto + 1).toLowerCase();
		return Set.of("jpg", "jpeg", "png", "webp", "gif").contains(extension);
	}

	private void validarArchivo(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			throw new IllegalArgumentException("Debe adjuntar un archivo de imagen");
		}
		if (!esFormatoImagenValido(archivo)) {
			throw new IllegalArgumentException("Formato de imagen no permitido. Use: JPG, PNG, WEBP o GIF");
		}
		if (archivo.getSize() > MAX_TAMANO) {
			throw new IllegalArgumentException("La imagen no puede superar los 5 MB");
		}
	}
}