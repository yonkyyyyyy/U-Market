package com.uMarket.uMarket.config;

import com.uMarket.uMarket.security.InternalApiKeyFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadena de seguridad EXCLUSIVA de la API interna de IA ({@code /api/ia/**}).
 *
 * <p>Está separada de {@code SecurityConfig} a propósito: las llamadas internas no
 * usan JWT de usuario sino una API key compartida, así la cadena "de usuarios"
 * (con {@code JwtAuthFilter}) queda intacta. Spring Security elige la primera
 * cadena que matchea; esta, por {@code @Order(1)} y su {@code securityMatcher},
 * solo atiende {@code /api/ia/**} y el resto de rutas sigue por la cadena normal.</p>
 *
 * <p>La API key se toma de {@code ia.internal.api-key} (variable de entorno
 * {@code IA_INTERNAL_API_KEY}). Si está vacía, {@link InternalApiKeyFilter} no
 * autentica a nadie y todo responde 401 (fail-closed).</p>
 */
@Configuration
@EnableWebSecurity
public class IaInternaSecurityConfig {

	@Value("${ia.internal.api-key:}")
	private String apiKey;

	@Bean
	@Order(1)
	public SecurityFilterChain iaInternaFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/api/ia/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"message\":\"API key interna ausente o inválida\"}");
						})
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setContentType("application/json");
							response.getWriter().write("{\"message\":\"Acceso denegado\"}");
						}))
				.authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SERVICIO"))
				.addFilterBefore(new InternalApiKeyFilter(apiKey), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
