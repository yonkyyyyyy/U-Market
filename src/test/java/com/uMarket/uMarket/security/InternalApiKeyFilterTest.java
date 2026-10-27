package com.uMarket.uMarket.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests unitarios del filtro de API key interna. La cadena de seguridad (que es
 * quien responde 401) no se prueba aquí sino en
 * {@link com.uMarket.uMarket.controller.InferenciaIaControllerTest}.
 */
class InternalApiKeyFilterTest {

	private static final String CLAVE = "clave-de-prueba";
	private static final String RUTA = "/api/ia/v1/texto";

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void autenticaConRolServicioSiLaClaveCoincide() throws Exception {
		FilterChain cadena = mock(FilterChain.class);

		new InternalApiKeyFilter(CLAVE).doFilter(peticion(CLAVE), new MockHttpServletResponse(), cadena);

		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		assertThat(autenticacion).isNotNull();
		assertThat(autenticacion.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly(InternalApiKeyFilter.ROL_SERVICIO);
		verify(cadena).doFilter(any(), any());
	}

	@Test
	void noAutenticaSiLaClaveNoCoincide() throws Exception {
		FilterChain cadena = mock(FilterChain.class);

		new InternalApiKeyFilter(CLAVE).doFilter(peticion("otra-clave"), new MockHttpServletResponse(), cadena);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(cadena).doFilter(any(), any());
	}

	@Test
	void noAutenticaSiNoSeEnviaElHeader() throws Exception {
		FilterChain cadena = mock(FilterChain.class);

		new InternalApiKeyFilter(CLAVE).doFilter(
				new MockHttpServletRequest("POST", RUTA), new MockHttpServletResponse(), cadena);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(cadena).doFilter(any(), any());
	}

	@Test
	void noAutenticaSiElServidorNoTieneClaveConfigurada() throws Exception {
		// Fail-closed: sin IA_INTERNAL_API_KEY no entra nadie, ni con un header válido.
		FilterChain cadena = mock(FilterChain.class);

		new InternalApiKeyFilter("").doFilter(peticion(""), new MockHttpServletResponse(), cadena);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(cadena).doFilter(any(), any());
	}

	private MockHttpServletRequest peticion(String clave) {
		MockHttpServletRequest peticion = new MockHttpServletRequest("POST", RUTA);
		peticion.addHeader(InternalApiKeyFilter.HEADER, clave);
		return peticion;
	}
}
