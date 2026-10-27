package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.dto.ProductoRequest;
import com.uMarket.uMarket.dto.TareaPesada;
import com.uMarket.uMarket.enums.EstadoModeracion;
import com.uMarket.uMarket.enums.ModerationStatus;
import com.uMarket.uMarket.exception.ContenidoInapropiadoException;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.ArchivoMultimedia;
import com.uMarket.uMarket.model.ImagenEnRevision;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ArchivoMultimediaRepository;
import com.uMarket.uMarket.repository.ImagenEnRevisionRepository;
import com.uMarket.uMarket.repository.ProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ProductoService {

	/** Estado publicado (el 'ACTIVO' del enunciado). */
	public static final String ESTADO_ACTIVO = "ACTIVO";
	public static final String ESTADO_PENDIENTE = "PENDIENTE";
	public static final String ESTADO_RECHAZADO = "RECHAZADO";

	/** Nomenclatura anterior del estado publicado: se acepta como alias y se normaliza. */
	private static final String ESTADO_ACTIVO_LEGACY = "DISPONIBLE";

	/** Estados que puede fijar el usuario directamente (los demás los controla la moderación). */
	private static final Set<String> ESTADOS_VALIDOS = Set.of(ESTADO_ACTIVO, "VENDIDO", "PAUSADO");

	/** Estados visibles en el catálogo público. Excluye PENDIENTE y RECHAZADO. */
	private static final Set<String> ESTADOS_PUBLICOS = Set.of(ESTADO_ACTIVO, "VENDIDO", "PAUSADO");

	private final ProductoRepository productoRepository;
	private final ArchivoMultimediaRepository archivoMultimediaRepository;
	private final ImagenEnRevisionRepository imagenEnRevisionRepository;
	private final CloudinaryService cloudinaryService;
	private final ImageProcessingService imageProcessingService;
	private final OpenAIModeracionService openAIModeracionService;
	private final VisionAnalisisService visionAnalisisService;
	private final StorageService storageService;
	private final TareaPesadaProducer tareaPesadaProducer;
	private final RegistroModeracionService registroModeracionService;

	public ProductoService(ProductoRepository productoRepository,
							ArchivoMultimediaRepository archivoMultimediaRepository,
							ImagenEnRevisionRepository imagenEnRevisionRepository,
							CloudinaryService cloudinaryService,
							ImageProcessingService imageProcessingService,
							OpenAIModeracionService openAIModeracionService,
							VisionAnalisisService visionAnalisisService,
							StorageService storageService,
							TareaPesadaProducer tareaPesadaProducer,
							RegistroModeracionService registroModeracionService) {
		this.productoRepository = productoRepository;
		this.archivoMultimediaRepository = archivoMultimediaRepository;
		this.imagenEnRevisionRepository = imagenEnRevisionRepository;
		this.cloudinaryService = cloudinaryService;
		this.imageProcessingService = imageProcessingService;
		this.openAIModeracionService = openAIModeracionService;
		this.visionAnalisisService = visionAnalisisService;
		this.storageService = storageService;
		this.tareaPesadaProducer = tareaPesadaProducer;
		this.registroModeracionService = registroModeracionService;
	}

	@Transactional(readOnly = true)
	public List<ProductoDto> listarTodos(String estado) {
		if (estado == null || estado.isBlank()) {
			// Catálogo público: solo productos aprobados (ni PENDIENTE ni RECHAZADO).
			return productoRepository.findAll().stream()
					.filter(producto -> ESTADOS_PUBLICOS.contains(producto.getEstado()))
					.map(ProductoDto::from).toList();
		}
		String normalizado = normalizarFiltroEstado(estado);
		if (!ESTADOS_PUBLICOS.contains(normalizado)) {
			// No se exponen publicaciones en revisión ni rechazadas.
			return List.of();
		}
		return productoRepository.findByEstado(normalizado).stream().map(ProductoDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ProductoDto> listarPorUsuario(Usuario usuario) {
		// El dueño SÍ ve sus productos en revisión y rechazados (para saber qué pasó).
		return productoRepository.findByUsuario(usuario).stream().map(ProductoDto::from).toList();
	}

	/**
	 * Devuelve la publicación para el usuario que consulta. Las publicaciones en
	 * revisión ({@code PENDIENTE}) o rechazadas ({@code RECHAZADO}) solo las ve su
	 * dueño; para el resto se responde 404 (no se revela que existen).
	 */
	@Transactional(readOnly = true)
	public ProductoDto obtener(Long id, Usuario usuario) {
		Producto producto = obtenerEntidad(id);
		boolean esDueno = producto.getUsuario().getId().equals(usuario.getId());
		if (!ESTADOS_PUBLICOS.contains(producto.getEstado()) && !esDueno) {
			throw new ResourceNotFoundException("Producto no encontrado con id: " + id);
		}
		return ProductoDto.from(producto);
	}

	/**
	 * Crea la publicación. El texto se revisa de forma SÍNCRONA (si es inapropiado
	 * responde 422 y no se crea nada).
	 *
	 * <p>La publicación nace {@code PENDIENTE} y <b>no se activa sola</b>: el cliente
	 * la publica con {@link #publicar(Long, Usuario)} (o subiendo una imagen), y es
	 * el pipeline asíncrono quien la pasa a {@code ACTIVO}. Así ninguna publicación
	 * es ACTIVA antes de pasar por el pipeline y no hay ventana de exposición entre
	 * la creación y la subida de la imagen.</p>
	 */
	@Transactional
	public ProductoDto crear(Usuario usuario, ProductoRequest request) {
		// Tarea IA (texto): revisión de contenido ANTES de guardar. Si es inapropiado
		// se registra el log y se lanza la excepción (nada se crea).
		ResultadoTexto texto = revisarTexto(request.titulo(), request.descripcion(), usuario, null);
		// Se valida el estado solicitado (para seguir devolviendo 400 si es inválido).
		normalizarEstado(request.estado());

		Producto producto = new Producto();
		producto.setUsuario(usuario);
		producto.setTitulo(request.titulo().trim());
		producto.setDescripcion(request.descripcion());
		producto.setPrecio(request.precio());
		producto.setCategoria(normalizarCategoria(request.categoria()));
		// Nace BLOQUEADA: no es visible hasta que el pipeline asíncrono la resuelva.
		producto.setEstado(ESTADO_PENDIENTE);
		producto.setCreatedAt(LocalDateTime.now());
		// NO se publica aquí: la activación la decide el pipeline cuando el cliente lo
		// pida (publicar(...)) o cuando se aprueben sus imágenes. Esperar evita exponer
		// la publicación antes de que el cliente termine de subir su contenido.
		Producto guardado = productoRepository.save(producto);
		// Ya con el id real, se deja el log de la decisión (panel de moderación).
		registrarLogTexto(guardado, usuario, texto);
		return ProductoDto.from(guardado);
	}

	/**
	 * Marca la publicación como "lista" y encola el pipeline asíncrono, que resolverá
	 * su estado: {@code ACTIVO} si no hay nada pendiente (p. ej. publicar sin
	 * imágenes), {@code PENDIENTE} si aún hay imágenes en revisión o {@code RECHAZADO}
	 * si alguna fue rechazada.
	 *
	 * <p>Es la vía para publicar sin imágenes sin exponer la publicación antes de
	 * tiempo: hasta que el worker no corra, el producto sigue {@code PENDIENTE}.</p>
	 */
	@Transactional
	public void publicar(Long id, Usuario usuario) {
		Producto producto = obtenerEntidad(id);
		verificarPropietario(producto, usuario);
		tareaPesadaProducer.publicar(TareaPesada.publicacion(id));
	}

	@Transactional
	public ProductoDto actualizar(Long id, Usuario usuario, ProductoRequest request) {
		Producto producto = obtenerEntidad(id);
		verificarPropietario(producto, usuario);
		// Tarea IA: también se revisa el texto al EDITAR (antes se podía esquivar el filtro).
		ResultadoTexto texto = revisarTexto(request.titulo(), request.descripcion(), usuario, id);
		producto.setTitulo(request.titulo().trim());
		producto.setDescripcion(request.descripcion());
		producto.setPrecio(request.precio());
		producto.setCategoria(normalizarCategoria(request.categoria()));
		// El estado de un producto aún no aprobado lo manda la moderación: no se puede
		// "activar" a mano uno PENDIENTE o RECHAZADO.
		if (!ESTADO_PENDIENTE.equals(producto.getEstado()) && !ESTADO_RECHAZADO.equals(producto.getEstado())) {
			producto.setEstado(normalizarEstado(request.estado()));
		}
		Producto guardado = productoRepository.save(producto);
		registrarLogTexto(guardado, usuario, texto);
		return ProductoDto.from(guardado);
	}

	@Transactional
	public void eliminar(Long id, Usuario usuario) {
		Producto producto = obtenerEntidad(id);
		verificarPropietario(producto, usuario);
		productoRepository.delete(producto);
	}

	/**
	 * Sube (recibe) la imagen de un producto y la deja EN REVISIÓN.
	 *
	 * <p>La imagen comprimida se guarda en el {@link StorageService} (staging, en
	 * disco) y se encola una {@link TareaPesada} para que el worker la modere.
	 * <b>La imagen no va a Cloudinary todavía</b>: eso ocurre en el worker solo si
	 * pasa el filtro. Mientras tanto el producto sigue {@code PENDIENTE}.</p>
	 *
	 * @return id de la imagen en revisión (para que el cliente pueda consultarla)
	 */
	@Transactional
	public Long subirImagenProducto(Long productoId, MultipartFile file, Usuario usuario) {
		Producto producto = obtenerEntidad(productoId);
		verificarPropietario(producto, usuario);

		File comprimida;
		try {
			comprimida = imageProcessingService.compressAndResizeImage(file);
		} catch (IOException e) {
			throw new RuntimeException("Error al procesar la imagen", e);
		}

		String stagingId = storageService.guardar(comprimida);
		comprimida.delete();

		ImagenEnRevision imagen = new ImagenEnRevision();
		imagen.setProducto(producto);
		imagen.setStagingPath(stagingId);
		imagen.setEstadoModeracion(EstadoModeracion.PENDIENTE);
		ImagenEnRevision guardada = imagenEnRevisionRepository.save(imagen);

		// Mientras la imagen se modera, el producto queda oculto (PENDIENTE).
		recalcularEstado(productoId);

		// Pipeline asíncrono: el worker moderará la imagen y lo activará (o rechazará).
		tareaPesadaProducer.publicar(
				TareaPesada.imagen(productoId, guardada.getId(), stagingId));

		return guardada.getId();
	}

	/**
	 * Paso ASÍNCRONO del pipeline: lo ejecuta el worker ({@code TareaPesadaWorker}).
	 * Toma una imagen en revisión, la analiza con visión; si pasa, la sube a
	 * Cloudinary y la aprueba; si no, la rechaza (fail-closed). Al final recalcula
	 * el estado del producto, que solo será {@code ACTIVO} si TODO pasó.
	 *
	 * <p><b>Idempotente:</b> si la imagen ya no está {@code PENDIENTE} (mensaje
	 * duplicado de RabbitMQ), no hace nada → nunca se sube dos veces.</p>
	 */
	@Transactional
	public void procesarImagenPendiente(Long imagenId) {
		ImagenEnRevision imagen = imagenEnRevisionRepository.findByIdForUpdate(imagenId).orElse(null);
		if (imagen == null || imagen.getEstadoModeracion() != EstadoModeracion.PENDIENTE) {
			return; // ya procesada (o no existe) → idempotente
		}

		File archivo = storageService.leer(imagen.getStagingPath());
		long inicio = System.nanoTime();
		ModerationResultDTO resultado = visionAnalisisService.analizar(archivo);
		long latenciaMs = (System.nanoTime() - inicio) / 1_000_000;

		if (resultado.getStatus() == ModerationStatus.APPROVED) {
			subirACloudinary(imagen, archivo);
			imagen.setEstadoModeracion(EstadoModeracion.APROBADO);
		} else {
			// Fail-closed: REJECTED (contenido prohibido) o FAILED (IA caída) ⇒ no se publica.
			imagen.setEstadoModeracion(EstadoModeracion.RECHAZADO);
		}
		imagenEnRevisionRepository.save(imagen);
		storageService.borrar(imagen.getStagingPath());

		recalcularEstado(imagen.getProducto().getId());

		// Log de la decisión de visión para el panel de moderación (best-effort).
		Producto producto = imagen.getProducto();
		ModerationStatus status = resultado.getStatus();
		registrarSeguro(() -> registroModeracionService.registrarImagen(
				producto.getId(), producto.getTitulo(), producto.getUsuario().getId(),
				resultado, motivoImagen(status), latenciaMs));
	}

	private void subirACloudinary(ImagenEnRevision imagen, File archivo) {
		try {
			// CloudinaryService.uploadFile BORRA el archivo que recibe: le damos una copia
			// para no destruir el staging (así, si algo falla después, el reintento de
			// RabbitMQ aún tiene la imagen para volver a intentarlo).
			File copia = File.createTempFile("upload_", extension(archivo.getName()));
			Files.copy(archivo.toPath(), copia.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Map<?, ?> subida = cloudinaryService.uploadFile(copia);

			ArchivoMultimedia multimedia = new ArchivoMultimedia();
			multimedia.setProducto(imagen.getProducto());
			multimedia.setUrl(subida.get("secure_url").toString());
			multimedia.setPublicId(subida.get("public_id").toString());
			multimedia.setFormato(subida.get("format").toString());
			multimedia.setSizeBytes(Long.valueOf(subida.get("bytes").toString()));
			archivoMultimediaRepository.save(multimedia);
		} catch (IOException e) {
			throw new RuntimeException("Error al subir la imagen a Cloudinary", e);
		}
	}

	/**
	 * Recalcula el estado del producto DERIVÁNDOLO de sus imágenes en revisión.
	 *
	 * <p>No se "flipea" un flag: se recalcula completo desde la fuente de verdad,
	 * dentro de un lock pesimista, para que dos workers concurrentes no se pisen
	 * (lost update) y para que el resultado no dependa del orden de llegada.</p>
	 *
	 * <p>{@code @Transactional} para poder invocarse también desde el worker (fuera
	 * de otra transacción); cuando se llama internamente reutiliza la transacción
	 * en curso.</p>
	 */
	@Transactional
	public void recalcularEstado(Long productoId) {
		Producto producto = productoRepository.findByIdForUpdate(productoId).orElse(null);
		if (producto == null) {
			return;
		}
		producto.setEstado(derivarEstado(imagenEnRevisionRepository.findByProductoId(productoId)));
		productoRepository.save(producto);
	}

	/**
	 * Deriva el estado del producto a partir de sus imágenes en revisión:
	 * <ul>
	 *   <li>Alguna {@code RECHAZADO} → {@code RECHAZADO} (una infracción tumba la publicación).</li>
	 *   <li>Sin imágenes o todas {@code APROBADO} → {@code ACTIVO} (no hay nada pendiente).</li>
	 *   <li>Alguna aún {@code PENDIENTE} → {@code PENDIENTE} (bloqueada hasta resolverse).</li>
	 * </ul>
	 */
	private String derivarEstado(List<ImagenEnRevision> imagenes) {
		boolean rechazado = imagenes.stream()
				.anyMatch(imagen -> imagen.getEstadoModeracion() == EstadoModeracion.RECHAZADO);
		if (rechazado) {
			return ESTADO_RECHAZADO;
		}
		// Lista vacía ⇒ allMatch devuelve true ⇒ sin imágenes el producto se publica.
		boolean todasAprobadas = imagenes.stream()
				.allMatch(imagen -> imagen.getEstadoModeracion() == EstadoModeracion.APROBADO);
		return todasAprobadas ? ESTADO_ACTIVO : ESTADO_PENDIENTE;
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

	private String extension(String nombre) {
		int punto = nombre.lastIndexOf('.');
		return punto >= 0 ? nombre.substring(punto) : ".jpg";
	}

	private String normalizarCategoria(String categoria) {
		if (categoria == null || categoria.isBlank()) {
			return null;
		}
		return categoria.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizarEstado(String estado) {
		if (estado == null || estado.isBlank()) {
			return ESTADO_ACTIVO;
		}
		String normalizado = estado.trim().toUpperCase(Locale.ROOT);
		// "DISPONIBLE" es la nomenclatura anterior de "ACTIVO": se acepta como alias.
		if (ESTADO_ACTIVO_LEGACY.equals(normalizado)) {
			normalizado = ESTADO_ACTIVO;
		}
		if (!ESTADOS_VALIDOS.contains(normalizado)) {
			throw new IllegalArgumentException("Estado inválido. Use: ACTIVO, VENDIDO o PAUSADO");
		}
		return normalizado;
	}

	/** Normaliza el filtro de estado del catálogo (mayúsculas y alias DISPONIBLE → ACTIVO). */
	private String normalizarFiltroEstado(String estado) {
		String normalizado = estado.trim().toUpperCase(Locale.ROOT);
		return ESTADO_ACTIVO_LEGACY.equals(normalizado) ? ESTADO_ACTIVO : normalizado;
	}

	/**
	 * Revisa que el contenido a publicar (título + descripción) no contenga
	 * lenguaje inapropiado. Si lo contiene, deja el log de la decisión y lanza
	 * {@code ContenidoInapropiadoException}: la publicación NO se crea. Si OpenAI
	 * falla (fail-open), se permite publicar.
	 *
	 * @return el análisis y su latencia, o {@code null} si no había texto que revisar
	 */
	private ResultadoTexto revisarTexto(String titulo, String descripcion, Usuario usuario, Long productoId) {
		String texto = (titulo == null ? "" : titulo.trim())
				+ " " + (descripcion == null ? "" : descripcion.trim());
		if (texto.isBlank()) {
			return null;
		}
		long inicio = System.nanoTime();
		ModerationResultDTO resultado = openAIModeracionService.analizar(texto);
		long latenciaMs = (System.nanoTime() - inicio) / 1_000_000;

		if (resultado.getStatus() == ModerationStatus.REJECTED) {
			// Se registra ANTES de lanzar: al rechazar, la transacción de crear/editar
			// se revierte, así que el log debe vivir en su propia transacción.
			ModerationStatus status = resultado.getStatus();
			registrarSeguro(() -> registroModeracionService.registrarTexto(
					productoId, titulo, usuario.getId(), resultado, texto, motivoTexto(status), latenciaMs));
			throw new ContenidoInapropiadoException(resultado);
		}
		return new ResultadoTexto(resultado, texto, latenciaMs);
	}

	/** Log de una revisión de texto que NO se rechazó (aprobada o con la IA caída). */
	private void registrarLogTexto(Producto producto, Usuario usuario, ResultadoTexto texto) {
		if (texto == null) {
			return;
		}
		ModerationStatus status = texto.resultado().getStatus();
		registrarSeguro(() -> registroModeracionService.registrarTexto(
				producto.getId(), producto.getTitulo(), usuario.getId(),
				texto.resultado(), texto.texto(), motivoTexto(status), texto.latenciaMs()));
	}

	private String motivoTexto(ModerationStatus status) {
		return switch (status) {
			case REJECTED -> "Lenguaje inapropiado detectado en el texto";
			case FAILED, PENDING -> "La IA de texto no estuvo disponible (fail-open: se publicó igual)";
			case APPROVED -> null;
		};
	}

	private String motivoImagen(ModerationStatus status) {
		return switch (status) {
			case REJECTED -> "Contenido prohibido detectado en la imagen";
			case FAILED, PENDING -> "La IA de visión no estuvo disponible (fail-closed: no se publicó)";
			case APPROVED -> null;
		};
	}

	/**
	 * El log de moderación es best-effort: si falla (BD, timeout), se avisa pero
	 * NUNCA se tumba la moderación ni la publicación.
	 */
	private void registrarSeguro(Runnable registro) {
		try {
			registro.run();
		} catch (RuntimeException e) {
			log.warn("No se pudo registrar el log de moderación: {}", e.getMessage());
		}
	}

	/** Análisis de texto ya resuelto, con lo necesario para dejar el log después. */
	private record ResultadoTexto(ModerationResultDTO resultado, String texto, long latenciaMs) {
	}
}
