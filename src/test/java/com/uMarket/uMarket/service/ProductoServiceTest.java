package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.dto.ProductoRequest;
import com.uMarket.uMarket.dto.TareaPesada;
import com.uMarket.uMarket.enums.EstadoModeracion;
import com.uMarket.uMarket.enums.ModerationStatus;
import com.uMarket.uMarket.exception.ContenidoInapropiadoException;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.ImagenEnRevision;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ArchivoMultimediaRepository;
import com.uMarket.uMarket.repository.ImagenEnRevisionRepository;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

	@Mock
	private ProductoRepository productoRepository;
	@Mock
	private ArchivoMultimediaRepository archivoMultimediaRepository;
	@Mock
	private ImagenEnRevisionRepository imagenEnRevisionRepository;
	@Mock
	private CloudinaryService cloudinaryService;
	@Mock
	private ImageProcessingService imageProcessingService;
	@Mock
	private OpenAIModeracionService openAIModeracionService;
	@Mock
	private VisionAnalisisService visionAnalisisService;
	@Mock
	private StorageService storageService;
	@Mock
	private TareaPesadaProducer tareaPesadaProducer;

	private ProductoService productoService;

	@BeforeEach
	void setUp() {
		productoService = new ProductoService(
				productoRepository,
				archivoMultimediaRepository,
				imagenEnRevisionRepository,
				cloudinaryService,
				imageProcessingService,
				openAIModeracionService,
				visionAnalisisService,
				storageService,
				tareaPesadaProducer
		);
	}

	@Test
	void listarTodosDevuelveListaMapeada() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		when(productoRepository.findAll()).thenReturn(List.of(producto));

		List<ProductoDto> result = productoService.listarTodos(null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).titulo()).isEqualTo("Calculadora Casio");
		assertThat(result.get(0).vendedorNombre()).isEqualTo("Ana Torres");
	}

	@Test
	void listarTodosOcultaProductosEnRevisionYRechazados() {
		Producto enRevision = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		enRevision.setEstado("PENDIENTE");
		Producto rechazado = producto(2L, usuario(1L, "ana@utp.edu.pe"));
		rechazado.setEstado("RECHAZADO");
		when(productoRepository.findAll()).thenReturn(List.of(enRevision, rechazado));

		List<ProductoDto> result = productoService.listarTodos(null);

		assertThat(result).isEmpty();
	}

	@Test
	void listarPorEstadoFiltraEnRepositorio() {
		when(productoRepository.findByEstado("ACTIVO")).thenReturn(List.of());

		productoService.listarTodos("activo");

		verify(productoRepository).findByEstado("ACTIVO");
	}

	@Test
	void listarPorEstadoAceptaElAliasHeredadoDisponible() {
		when(productoRepository.findByEstado("ACTIVO")).thenReturn(List.of());

		// "DISPONIBLE" era el nombre anterior del estado publicado: se normaliza a ACTIVO.
		productoService.listarTodos("disponible");

		verify(productoRepository).findByEstado("ACTIVO");
	}

	@Test
	void listarPorEstadoNoExponePendientes() {
		List<ProductoDto> result = productoService.listarTodos("PENDIENTE");

		assertThat(result).isEmpty();
		verify(productoRepository, never()).findByEstado(any());
	}

	@Test
	void crearNacePendienteYNoSePublicaSolo() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		when(openAIModeracionService.analizar(any(String.class)))
				.thenReturn(moderacion(ModerationStatus.APPROVED));
		when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductoRequest request = new ProductoRequest("Pendrive", "16GB", new BigDecimal("15.00"), null, null);

		ProductoDto result = productoService.crear(dueno, request);

		// Nace BLOQUEADA: no es visible hasta que el pipeline asíncrono la resuelva.
		assertThat(result.estado()).isEqualTo("PENDIENTE");
		assertThat(result.usuarioId()).isEqualTo(1L);
		assertThat(result.precio()).isEqualByComparingTo("15.00");

		// NO se encola nada al crear: el cliente decide cuándo publicar (POST /{id}/publicar).
		// Así no hay ventana de exposición entre crear y subir la imagen.
		verify(tareaPesadaProducer, never()).publicar(any());
	}

	@Test
	void crearRechazaContenidoInapropiado() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		when(openAIModeracionService.analizar(any(String.class)))
				.thenReturn(moderacion(ModerationStatus.REJECTED));

		ProductoRequest request = new ProductoRequest("Vete a la mierda ladrón", "te voy a denigrar", new BigDecimal("15.00"), null, null);

		assertThatThrownBy(() -> productoService.crear(dueno, request))
				.isInstanceOf(ContenidoInapropiadoException.class);

		verify(productoRepository, never()).save(any());
	}

	@Test
	void crearPermiteSiOpenAiFalla() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		when(openAIModeracionService.analizar(any(String.class)))
				.thenReturn(moderacion(ModerationStatus.FAILED));
		when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductoRequest request = new ProductoRequest("Pendrive", "16GB", new BigDecimal("15.00"), null, null);

		ProductoDto result = productoService.crear(dueno, request);

		// Fail-open del texto: si OpenAI falla, la publicación se crea igual (aunque
		// siga BLOQUEADA hasta que el pipeline asíncrono la resuelva).
		assertThat(result.estado()).isEqualTo("PENDIENTE");
	}

	@Test
	void crearRechazaEstadoInvalido() {
		when(openAIModeracionService.analizar(any(String.class)))
				.thenReturn(moderacion(ModerationStatus.APPROVED));
		ProductoRequest request = new ProductoRequest("Pendrive", "16GB", new BigDecimal("15.00"), null, "ROBADO");

		assertThatThrownBy(() -> productoService.crear(usuario(1L, "ana@utp.edu.pe"), request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Estado inválido");
	}

	@Test
	void publicarEncolaLaTareaDePublicacion() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		productoService.publicar(1L, dueno);

		// El pipeline (no el usuario) es quien activará la publicación.
		ArgumentCaptor<TareaPesada> captor = ArgumentCaptor.forClass(TareaPesada.class);
		verify(tareaPesadaProducer).publicar(captor.capture());
		assertThat(captor.getValue().tipo()).isEqualTo(TareaPesada.TIPO_PUBLICACION);
	}

	@Test
	void publicarBloqueaAUnNoPropietario() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		assertThatThrownBy(() -> productoService.publicar(1L, usuario(2L, "luis@utp.edu.pe")))
				.isInstanceOf(AccessDeniedException.class);

		verify(tareaPesadaProducer, never()).publicar(any());
	}

	@Test
	void obtenerDevuelveProductoExistente() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		ProductoDto result = productoService.obtener(1L, usuario(2L, "luis@utp.edu.pe"));

		assertThat(result.id()).isEqualTo(1L);
	}

	@Test
	void obtenerLanza404SiNoExiste() {
		when(productoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productoService.obtener(99L, usuario(1L, "ana@utp.edu.pe")))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void obtenerOcultaUnaPublicacionEnRevisionAOtrosUsuarios() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		producto.setEstado("PENDIENTE");
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		// El dueño SÍ la ve (sabe qué pasó con su publicación)...
		assertThat(productoService.obtener(1L, usuario(1L, "ana@utp.edu.pe")).estado())
				.isEqualTo("PENDIENTE");
		// ...pero para el resto no existe (no se revela que está en revisión).
		assertThatThrownBy(() -> productoService.obtener(1L, usuario(2L, "luis@utp.edu.pe")))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void actualizarPermiteAlPropietario() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(openAIModeracionService.analizar(any(String.class)))
				.thenReturn(moderacion(ModerationStatus.APPROVED));
		when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductoRequest request = new ProductoRequest("Calculadora Casio", "fx-991 CW", new BigDecimal("79.90"), null, "PAUSADO");

		ProductoDto result = productoService.actualizar(1L, dueno, request);

		assertThat(result.precio()).isEqualByComparingTo("79.90");
		assertThat(result.estado()).isEqualTo("PAUSADO");
	}

	@Test
	void actualizarNoActivaUnProductoAunEnRevision() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		producto.setEstado("PENDIENTE");
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(openAIModeracionService.analizar(any(String.class)))
				.thenReturn(moderacion(ModerationStatus.APPROVED));
		when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// El dueño intenta activarlo a mano: no puede, manda la moderación.
		ProductoRequest request = new ProductoRequest("Calculadora Casio", "fx-991 CW", new BigDecimal("79.90"), null, "ACTIVO");

		ProductoDto result = productoService.actualizar(1L, dueno, request);

		assertThat(result.estado()).isEqualTo("PENDIENTE");
	}

	@Test
	void actualizarBloqueaAUnNoPropietario() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		ProductoRequest request = new ProductoRequest("Robado", "x", new BigDecimal("1.00"), null, "ACTIVO");

		assertThatThrownBy(() -> productoService.actualizar(1L, usuario(2L, "luis@utp.edu.pe"), request))
				.isInstanceOf(AccessDeniedException.class);

		verify(productoRepository, never()).save(any());
	}

	@Test
	void actualizarRechazaContenidoInapropiado() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(openAIModeracionService.analizar(any(String.class)))
				.thenReturn(moderacion(ModerationStatus.REJECTED));

		ProductoRequest request = new ProductoRequest("Vete a la mierda ladrón", "te voy a denigrar", new BigDecimal("15.00"), null, "ACTIVO");

		assertThatThrownBy(() -> productoService.actualizar(1L, dueno, request))
				.isInstanceOf(ContenidoInapropiadoException.class);

		verify(productoRepository, never()).save(any());
	}

	@Test
	void eliminarPermiteAlPropietario() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		productoService.eliminar(1L, dueno);

		verify(productoRepository).delete(producto);
	}

	@Test
	void eliminarBloqueaAUnNoPropietario() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		assertThatThrownBy(() -> productoService.eliminar(1L, usuario(2L, "luis@utp.edu.pe")))
				.isInstanceOf(AccessDeniedException.class);

		verify(productoRepository, never()).delete(any());
	}

	@Test
	void subirImagenGuardaEnStagingOcultaElProductoYEncolaLaModeracion() throws Exception {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(imageProcessingService.compressAndResizeImage(any()))
				.thenReturn(File.createTempFile("img_test", ".jpg"));
		when(storageService.guardar(any())).thenReturn("uuid-123.jpg");
		when(imagenEnRevisionRepository.save(any(ImagenEnRevision.class))).thenAnswer(invocation -> {
			ImagenEnRevision imagen = invocation.getArgument(0);
			imagen.setId(7L);
			return imagen;
		});
		when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
		when(imagenEnRevisionRepository.findByProductoId(1L))
				.thenReturn(List.of(imagen(7L, producto, EstadoModeracion.PENDIENTE, "uuid-123.jpg")));

		Long imagenId = productoService.subirImagenProducto(1L, mock(MultipartFile.class), dueno);

		assertThat(imagenId).isEqualTo(7L);
		// Mientras se modera, el producto deja de ser visible.
		assertThat(producto.getEstado()).isEqualTo("PENDIENTE");
		// La subida es asíncrona: todavía NO se analiza ni se toca Cloudinary.
		verify(visionAnalisisService, never()).analizar(any());
		verify(cloudinaryService, never()).uploadFile(any());
		verify(tareaPesadaProducer).publicar(any(TareaPesada.class));
	}

	@Test
	void procesarImagenAprobadaLaSubeACloudinaryYActivaElProducto() throws Exception {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		producto.setEstado("PENDIENTE");
		ImagenEnRevision imagen = imagen(7L, producto, EstadoModeracion.PENDIENTE, "uuid-123.jpg");

		when(imagenEnRevisionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(imagen));
		when(storageService.leer("uuid-123.jpg")).thenReturn(File.createTempFile("staging_test", ".jpg"));
		when(visionAnalisisService.analizar(any()))
				.thenReturn(moderacion(ModerationStatus.APPROVED));
		when(cloudinaryService.uploadFile(any())).thenReturn(Map.of(
				"secure_url", "https://res.cloudinary.com/umarket/cuaderno.jpg",
				"public_id", "umarket/cuaderno",
				"format", "jpg",
				"bytes", "1234"));
		when(archivoMultimediaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(imagenEnRevisionRepository.findByProductoId(1L)).thenReturn(List.of(imagen));
		when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));

		productoService.procesarImagenPendiente(7L);

		assertThat(imagen.getEstadoModeracion()).isEqualTo(EstadoModeracion.APROBADO);
		assertThat(producto.getEstado()).isEqualTo("ACTIVO");
		verify(cloudinaryService).uploadFile(any());
		verify(archivoMultimediaRepository).save(any());
		verify(storageService).borrar("uuid-123.jpg");
	}

	@Test
	void procesarImagenRechazadaNoSubeNiActivaElProducto() throws Exception {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		producto.setEstado("PENDIENTE");
		ImagenEnRevision imagen = imagen(7L, producto, EstadoModeracion.PENDIENTE, "uuid-123.jpg");

		when(imagenEnRevisionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(imagen));
		when(storageService.leer("uuid-123.jpg")).thenReturn(File.createTempFile("staging_test", ".jpg"));
		when(visionAnalisisService.analizar(any()))
				.thenReturn(moderacion(ModerationStatus.REJECTED));
		when(imagenEnRevisionRepository.findByProductoId(1L)).thenReturn(List.of(imagen));
		when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));

		productoService.procesarImagenPendiente(7L);

		assertThat(imagen.getEstadoModeracion()).isEqualTo(EstadoModeracion.RECHAZADO);
		assertThat(producto.getEstado()).isEqualTo("RECHAZADO");
		verify(cloudinaryService, never()).uploadFile(any());
		verify(archivoMultimediaRepository, never()).save(any());
	}

	@Test
	void procesarImagenEsIdempotenteAnteMensajeDuplicado() throws Exception {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		ImagenEnRevision yaAprobada = imagen(7L, producto, EstadoModeracion.APROBADO, "uuid-123.jpg");
		when(imagenEnRevisionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(yaAprobada));

		productoService.procesarImagenPendiente(7L);

		// Ya estaba procesada: no se vuelve a analizar ni a subir (ni a recalcular).
		verify(visionAnalisisService, never()).analizar(any());
		verify(cloudinaryService, never()).uploadFile(any());
		verify(productoRepository, never()).save(any());
	}

	@Test
	void recalcularEstadoActivaSiTodasLasImagenesEstanAprobadas() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		producto.setEstado("PENDIENTE");
		when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
		when(imagenEnRevisionRepository.findByProductoId(1L))
				.thenReturn(List.of(imagen(1L, producto, EstadoModeracion.APROBADO, "a.jpg")));

		productoService.recalcularEstado(1L);

		assertThat(producto.getEstado()).isEqualTo("ACTIVO");
	}

	@Test
	void recalcularEstadoRechazaSiAlgunaImagenFueRechazada() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		producto.setEstado("ACTIVO");
		when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
		when(imagenEnRevisionRepository.findByProductoId(1L)).thenReturn(List.of(
				imagen(1L, producto, EstadoModeracion.APROBADO, "a.jpg"),
				imagen(2L, producto, EstadoModeracion.RECHAZADO, "b.jpg")));

		productoService.recalcularEstado(1L);

		// Una sola infracción tumba la publicación, aunque otra imagen haya pasado.
		assertThat(producto.getEstado()).isEqualTo("RECHAZADO");
	}

	@Test
	void recalcularEstadoQuedaPendienteSiFaltaModerarAlguna() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		producto.setEstado("PENDIENTE");
		when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
		when(imagenEnRevisionRepository.findByProductoId(1L)).thenReturn(List.of(
				imagen(1L, producto, EstadoModeracion.APROBADO, "a.jpg"),
				imagen(2L, producto, EstadoModeracion.PENDIENTE, "b.jpg")));

		productoService.recalcularEstado(1L);

		assertThat(producto.getEstado()).isEqualTo("PENDIENTE");
	}

	@Test
	void recalcularEstadoSinImagenesDejaElProductoVisible() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
		when(imagenEnRevisionRepository.findByProductoId(1L)).thenReturn(List.of());

		productoService.recalcularEstado(1L);

		// La imagen es opcional: sin imágenes no hay nada que moderar → visible.
		assertThat(producto.getEstado()).isEqualTo("ACTIVO");
	}

	private Usuario usuario(Long id, String correo) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNombre("Ana Torres");
		usuario.setCorreoInstitucional(correo);
		usuario.setPasswordHash("hash");
		usuario.setRol("ESTUDIANTE");
		return usuario;
	}

	private Producto producto(Long id, Usuario usuario) {
		Producto producto = new Producto();
		producto.setId(id);
		producto.setUsuario(usuario);
		producto.setTitulo("Calculadora Casio");
		producto.setDescripcion("fx-991LA CW");
		producto.setPrecio(new BigDecimal("89.50"));
		producto.setEstado("ACTIVO");
		return producto;
	}

	private ImagenEnRevision imagen(Long id, Producto producto, EstadoModeracion estado, String stagingPath) {
		ImagenEnRevision imagen = new ImagenEnRevision();
		imagen.setId(id);
		imagen.setProducto(producto);
		imagen.setEstadoModeracion(estado);
		imagen.setStagingPath(stagingPath);
		return imagen;
	}

	private ModerationResultDTO moderacion(ModerationStatus status) {
		ModerationResultDTO resultado = new ModerationResultDTO();
		resultado.setStatus(status);
		resultado.setConfidenceScore(status == ModerationStatus.REJECTED ? 0.98 : 0.01);
		return resultado;
	}
}
