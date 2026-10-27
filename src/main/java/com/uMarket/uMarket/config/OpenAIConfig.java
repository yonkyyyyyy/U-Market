package com.uMarket.uMarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP para la API de OpenAI (moderación de contenido).
 *
 * <p>Configuración en {@code application.yaml} bajo la clave {@code openai.*}.
 * La API key se lee de la variable de entorno {@code OPENAI_API_KEY} y nunca
 * se commitea (patrón igual que Cloudinary).</p>
 */
@Configuration
public class OpenAIConfig {

	@Value("${openai.base-url:https://api.openai.com}")
	private String baseUrl;

	@Value("${openai.api-key:}")
	private String apiKey;

	@Bean
	public RestClient openaiRestClient() {
		return RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader("Authorization", "Bearer " + apiKey)
				.defaultHeader("Content-Type", "application/json")
				.build();
	}
}