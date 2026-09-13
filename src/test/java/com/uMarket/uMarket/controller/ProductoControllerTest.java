package com.uMarket.uMarket.controller;

import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.dto.ProductoRequest;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.security.UsuarioPrincipal;
import com.uMarket.uMarket.security.JwtService;
import com.uMarket.uMarket.service.CustomUserDetailsService;
import com.uMarket.uMarket.service.ProductoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductoService productoService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	private final UsuarioPrincipal principal = new UsuarioPrincipal(usuario());

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	private void autenticar() {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
		SecurityContextHolder.setContext(context);
	}

	@Test
	void listarProductosDevuelveCatalogo() throws Exception {
		when(productoService.listarTodos(null)).thenReturn(List.of(productoDto()));

		mockMvc.perform(get("/api/productos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].titulo").value("Calculadora Casio"))
				.andExpect(jsonPath("$[0].vendedorNombre").value("Ana Torres"));

		verify(productoService).listarTodos(null);
	}

	@Test
	void listarProductosFiltraPorEstado() throws Exception {
		when(productoService.listarTodos("DISPONIBLE")).thenReturn(List.of(productoDto()));

		mockMvc.perform(get("/api/productos").param("estado", "DISPONIBLE"))
				.andExpect(status().isOk());

		verify(productoService).listarTodos("DISPONIBLE");
	}

	@Test
	void obtenerProductoExistente() throws Exception {
		when(productoService.obtener(1L)).thenReturn(productoDto());

		mockMvc.perform(get("/api/productos/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void obtenerProductoInexistenteDa404() throws Exception {
		when(productoService.obtener(99L)).thenThrow(new ResourceNotFoundException("Producto no encontrado con id: 99"));

		mockMvc.perform(get("/api/productos/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Producto no encontrado con id: 99"));
	}

	@Test
	void crearProductoDevuelve201() throws Exception {
		autenticar();
		when(productoService.crear(any(Usuario.class), any(ProductoRequest.class))).thenReturn(productoDto());

		mockMvc.perform(post("/api/productos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\":\"Calculadora Casio\",\"descripcion\":\"fx-991LA CW\",\"precio\":89.50}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.titulo").value("Calculadora Casio"));
	}

	@Test
	void crearProductoConPrecioInvalidoDa400() throws Exception {
		autenticar();
		mockMvc.perform(post("/api/productos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\":\"Calculadora\",\"precio\":0}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void actualizarProductoDevuelve200() throws Exception {
		autenticar();
		when(productoService.actualizar(eq(1L), any(Usuario.class), any(ProductoRequest.class))).thenReturn(productoDto());

		mockMvc.perform(put("/api/productos/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\":\"Calculadora Casio\",\"precio\":79.90,\"estado\":\"PAUSADO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void eliminarProductoDevuelve204() throws Exception {
		autenticar();
		mockMvc.perform(delete("/api/productos/1"))
				.andExpect(status().isNoContent());

		verify(productoService).eliminar(1L, principal.getUsuario());
	}

	private ProductoDto productoDto() {
		return new ProductoDto(1L, "Calculadora Casio", "fx-991LA CW", new BigDecimal("89.50"),
				"DISPONIBLE", null, 1L, "Ana Torres", null);
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNombre("Ana Torres");
		usuario.setCorreoInstitucional("ana@utp.edu.pe");
		usuario.setPasswordHash("hash");
		usuario.setRol("ESTUDIANTE");
		return usuario;
	}
}