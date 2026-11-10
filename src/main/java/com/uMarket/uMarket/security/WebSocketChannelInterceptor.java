package com.uMarket.uMarket.security;

import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Valida el JWT en el frame STOMP CONNECT. El handshake HTTP no lleva el token,
 * por eso la autenticación se hace aquí: si el token falta, está expirado o su
 * firma no es válida, se rechaza la conexión con {@link MessagingException}.
 *
 * Si es válido, se asocia un {@link java.security.Principal} cuyo nombre es el
 * correo institucional. Eso permite usar {@code convertAndSendToUser(correo, ...)}.
 */
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String TOKEN_HEADER = "token";
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String SESSION_TOKEN_ATTR = "token";

	private final JwtService jwtService;

	public WebSocketChannelInterceptor(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor =
				MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
			return message;
		}

		String token = resolverToken(accessor);
		if (token == null || token.isBlank()) {
			throw new MessagingException("Token JWT ausente en la conexión WebSocket");
		}

		String correo;
		try {
			correo = jwtService.extractSubject(token);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new MessagingException("Token JWT inválido", ex);
		}
		if (!jwtService.isValid(token, correo)) {
			throw new MessagingException("Token JWT inválido o expirado");
		}

		accessor.setUser(new UsernamePasswordAuthenticationToken(correo, null, List.of()));
		return message;
	}

	private String resolverToken(StompHeaderAccessor accessor) {
		String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
		if (authorization != null) {
			return authorization.startsWith(BEARER_PREFIX)
					? authorization.substring(BEARER_PREFIX.length()).trim()
					: authorization.trim();
		}

		String headerToken = accessor.getFirstNativeHeader(TOKEN_HEADER);
		if (headerToken != null) {
			return headerToken.trim();
		}

		// Fallback: token guardado en los atributos de la sesión HTTP.
		if (accessor.getSessionAttributes() != null) {
			Object sessionToken = accessor.getSessionAttributes().get(SESSION_TOKEN_ATTR);
			if (sessionToken instanceof String value) {
				return value.trim();
			}
		}
		return null;
	}
}
