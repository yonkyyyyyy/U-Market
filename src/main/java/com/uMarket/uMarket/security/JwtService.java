package com.uMarket.uMarket.security;

import com.uMarket.uMarket.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(@Value("${jwt.secret}") String secret,
					  @Value("${jwt.expiration}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
		this.expirationMs = expirationMs;
	}

	public String generateToken(Usuario usuario) {
		long now = System.currentTimeMillis();
		return Jwts.builder()
				.subject(usuario.getCorreoInstitucional())
				.claim("rol", usuario.getRol())
				.issuedAt(new Date(now))
				.expiration(new Date(now + expirationMs))
				.signWith(key)
				.compact();
	}

	public String extractSubject(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isValid(String token, String correo) {
		Claims claims = parseClaims(token);
		return claims.getSubject().equals(correo) && claims.getExpiration().after(new Date());
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}