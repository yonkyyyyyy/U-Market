package com.uMarket.uMarket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	@Bean
	public OpenAPI uMarketOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("API U-Market - Módulo Se Busca")
						.version("1.0")
						.description("API REST del módulo \"Se Busca\" de U-Market: "
								+ "gestión de demandas, emparejamiento con productos y moderación. "
								+ "Todas las peticiones (salvo registro/login) requieren JWT Bearer."))
				// Requisito global: el candado aparece en todos los endpoints.
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components()
						.addSecuritySchemes(SECURITY_SCHEME_NAME,
								new SecurityScheme()
										.name(SECURITY_SCHEME_NAME)
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("Pega el token JWT obtenido en /api/auth/login "
												+ "(sin el prefijo \"Bearer \", Swagger lo agrega solo).")));
	}
}
