package com.uMarket.uMarket.config;

import com.uMarket.uMarket.security.WebSocketChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración STOMP sobre WebSocket. El handshake HTTP queda abierto en
 * {@code /ws-umarket} (ver SecurityConfig); la autenticación real ocurre en el
 * frame CONNECT mediante {@link WebSocketChannelInterceptor}.
 *
 * Destinos:
 * <ul>
 *   <li>{@code /topic/**}: difusión global (p. ej. catálogo).</li>
 *   <li>{@code /queue/**}: colas privadas; se resuelven con {@code /user/**}
 *       cuando el servidor usa {@code convertAndSendToUser}.</li>
 *   <li>{@code /app/**}: destinos de entrada a métodos {@code @MessageMapping}.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final WebSocketChannelInterceptor channelInterceptor;
	private final String[] allowedOrigins;

	public WebSocketConfig(WebSocketChannelInterceptor channelInterceptor,
						   @Value("${app.websocket.allowed-origins:http://localhost:4200}") String allowedOrigins) {
		this.channelInterceptor = channelInterceptor;
		this.allowedOrigins = allowedOrigins.split("\\s*,\\s*");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws-umarket")
				.setAllowedOriginPatterns(allowedOrigins);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(channelInterceptor);
	}
}
