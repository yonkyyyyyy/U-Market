package com.uMarket.uMarket.service;

import com.uMarket.uMarket.exception.StockInsuficienteException;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.ProductoRepository;
import com.uMarket.uMarket.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración: demuestra que el stock se descuenta realmente en la BD
 * y que la sobreventa se previene con el UPDATE condicional.
 */
@SpringBootTest
@Transactional
class CompraStockIntegrationTest {

	@Autowired
	private ProductoService productoService;

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Test
	void compraDescuentaStockRealEnBaseDeDatos() {
		// --- Arrange: crea vendedor y comprador ---
		Usuario vendedor = new Usuario();
		vendedor.setNombre("Vendedor");
		vendedor.setCorreoInstitucional("vendedor@utp.edu.pe");
		vendedor.setPasswordHash("hash");
		vendedor.setRol("ESTUDIANTE");
		usuarioRepository.save(vendedor);

		Usuario comprador = new Usuario();
		comprador.setNombre("Comprador");
		comprador.setCorreoInstitucional("comprador@utp.edu.pe");
		comprador.setPasswordHash("hash");
		comprador.setRol("ESTUDIANTE");
		usuarioRepository.save(comprador);

		// --- Arrange: producto con 5 unidades ---
		Producto producto = new Producto();
		producto.setUsuario(vendedor);
		producto.setTitulo("Calculadora");
		producto.setPrecio(new java.math.BigDecimal("50.00"));
		producto.setEstado("ACTIVO");
		producto.setStock(5);
		productoRepository.save(producto);
		Long productoId = producto.getId();

		// --- Act: compra 2 unidades ---
		productoService.comprar(productoId, 2, comprador);

		// --- Assert: stock real en BD = 3 ---
		Producto enBD = productoRepository.findById(productoId).orElseThrow();
		assertThat(enBD.getStock()).isEqualTo(3);
		assertThat(enBD.getEstado()).isEqualTo("ACTIVO");

		// --- Act: compra 3 unidades más (agota stock) ---
		productoService.comprar(productoId, 3, comprador);

		// --- Assert: stock = 0, estado = VENDIDO ---
		Producto agotado = productoRepository.findById(productoId).orElseThrow();
		assertThat(agotado.getStock()).isZero();
		assertThat(agotado.getEstado()).isEqualTo("VENDIDO");

		// --- Act & Assert: sobreventa bloqueada ---
		// Producto VENDIDO → no se puede comprar
		assertThatThrownBy(() -> productoService.comprar(productoId, 1, comprador))
				.isInstanceOf(IllegalArgumentException.class);

		// --- Assert: stock sigue en 0, no se descontó ---
		Producto sinCambio = productoRepository.findById(productoId).orElseThrow();
		assertThat(sinCambio.getStock()).isZero();
	}

	@Test
	void compraMasDeLoDisponibleLanzaStockInsuficiente() {
		// --- Arrange ---
		Usuario vendedor = new Usuario();
		vendedor.setNombre("Vendedor2");
		vendedor.setCorreoInstitucional("vendedor2@utp.edu.pe");
		vendedor.setPasswordHash("hash");
		vendedor.setRol("ESTUDIANTE");
		usuarioRepository.save(vendedor);

		Usuario comprador = new Usuario();
		comprador.setNombre("Comprador2");
		comprador.setCorreoInstitucional("comprador2@utp.edu.pe");
		comprador.setPasswordHash("hash");
		comprador.setRol("ESTUDIANTE");
		usuarioRepository.save(comprador);

		Producto producto = new Producto();
		producto.setUsuario(vendedor);
		producto.setTitulo("Laptop");
		producto.setPrecio(new java.math.BigDecimal("1000.00"));
		producto.setEstado("ACTIVO");
		producto.setStock(5);
		productoRepository.save(producto);
		Long productoId = producto.getId();

		// --- Act & Assert: intentar comprar 6 de golpe ---
		assertThatThrownBy(() -> productoService.comprar(productoId, 6, comprador))
				.isInstanceOf(StockInsuficienteException.class);

		// --- Assert: stock sigue en 5, no se descontó nada ---
		Producto sinCambio = productoRepository.findById(productoId).orElseThrow();
		assertThat(sinCambio.getStock()).isEqualTo(5);
	}
}
