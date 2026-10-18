package com.uMarket.uMarket.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autentica las llamadas a la API INTERNA de IA ({@code /api/ia/**}) mediante una
 * API key compartida en el header {@code X-Internal-Api-Key}.
 *
 * <p>Si la key coincide, deja en el {@code SecurityContext} una autenticación con
 * el rol {@code SERVICIO} (que es lo que exige la cadena de
 * {@code IaInternaSecurityConfig}). Si no coincide —o si el servidor no tiene la
 * key configurada— no autentica y la cadena responde 401. Es <b>fail-closed</b>:
 * sin la variable de entorno configurada no entra nadie.</p>
 *
 * <p><b>No</b> es un bean {@code @Component} a propósito: así Spring Boot no lo
 * registra como filtro global de servlet y solo corre dentro de la cadena de
 * {@code /api/ia/**} (lo instancia {@code IaInternaSecurityConfig}).</p>
 */
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

	/** Header donde el Backend Core envía la API key interna. */
	public static final String HEADER = "X-Internal-Api-Key";

	/** Rol interno que la cadena de /api/ia/** exige para dejar pasar. */
	public static final String ROL_SERVICIO = "ROLE_SERVICIO";

	private final String claveConfigurada;

	public InternalApiKeyFilter(String claveConfigurada) {
		this.claveConfigurada = claveConfigurada;
		if (!hayClaveConfigurada()) {
			log.warn("API interna de IA DESHABILITADA: no está configurada la variable de entorno "
					+ "IA_INTERNAL_API_KEY. Toda petición a /api/ia/** responderá 401.");
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String enviada = request.getHeader(HEADER);
		if (hayClaveConfigurada() && enviada != null && coincide(enviada)) {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					"api-interna-ia", null, List.of(new SimpleGrantedAuthority(ROL_SERVICIO)));
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		chain.doFilter(request, response);
	}

	private boolean hayClaveConfigurada() {
		return claveConfigurada != null && !claveConfigurada.isBlank();
	}

	/**
	 * Compara en tiempo constante ({@link MessageDigest#isEqual}) para no filtrar
	 * por tiempo cuánto coincide la key enviada con la configurada.
	 */
	private boolean coincide(String enviada) {
		return MessageDigest.isEqual(
				enviada.getBytes(StandardCharsets.UTF_8),
				claveConfigurada.getBytes(StandardCharsets.UTF_8));
	}
}
