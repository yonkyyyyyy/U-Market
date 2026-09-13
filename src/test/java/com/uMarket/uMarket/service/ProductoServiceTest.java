package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.dto.ProductoRequest;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

	@Mock
	private ProductoRepository productoRepository;

	private ProductoService productoService;

	@BeforeEach
	void setUp() {
		productoService = new ProductoService(productoRepository);
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
	void listarPorEstadoFiltraEnRepositorio() {
		when(productoRepository.findByEstado("DISPONIBLE")).thenReturn(List.of());

		productoService.listarTodos("disponible");

		verify(productoRepository).findByEstado("DISPONIBLE");
	}

	@Test
	void crearAsignaEstadoPorDefectoDisponible() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductoRequest request = new ProductoRequest("Pendrive", "16GB", new BigDecimal("15.00"), null);

		ProductoDto result = productoService.crear(dueno, request);

		assertThat(result.estado()).isEqualTo("DISPONIBLE");
		assertThat(result.usuarioId()).isEqualTo(1L);
		assertThat(result.precio()).isEqualByComparingTo("15.00");
	}

	@Test
	void crearRechazaEstadoInvalido() {
		ProductoRequest request = new ProductoRequest("Pendrive", "16GB", new BigDecimal("15.00"), "ROBADO");

		assertThatThrownBy(() -> productoService.crear(usuario(1L, "ana@utp.edu.pe"), request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Estado inválido");
	}

	@Test
	void obtenerDevuelveProductoExistente() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		ProductoDto result = productoService.obtener(1L);

		assertThat(result.id()).isEqualTo(1L);
	}

	@Test
	void obtenerLanza404SiNoExiste() {
		when(productoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productoService.obtener(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void actualizarPermiteAlPropietario() {
		Usuario dueno = usuario(1L, "ana@utp.edu.pe");
		Producto producto = producto(1L, dueno);
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductoRequest request = new ProductoRequest("Calculadora Casio", "fx-991 CW", new BigDecimal("79.90"), "PAUSADO");

		ProductoDto result = productoService.actualizar(1L, dueno, request);

		assertThat(result.precio()).isEqualByComparingTo("79.90");
		assertThat(result.estado()).isEqualTo("PAUSADO");
	}

	@Test
	void actualizarBloqueaAUnNoPropietario() {
		Producto producto = producto(1L, usuario(1L, "ana@utp.edu.pe"));
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		ProductoRequest request = new ProductoRequest("Robado", "x", new BigDecimal("1.00"), "DISPONIBLE");

		assertThatThrownBy(() -> productoService.actualizar(1L, usuario(2L, "luis@utp.edu.pe"), request))
				.isInstanceOf(AccessDeniedException.class);

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
		producto.setEstado("DISPONIBLE");
		return producto;
	}
}