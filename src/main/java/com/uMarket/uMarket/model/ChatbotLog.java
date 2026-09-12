package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_logs")
@Getter
@Setter
@NoArgsConstructor
public class ChatbotLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "mensaje_usuario", nullable = false, columnDefinition = "TEXT")
	private String mensajeUsuario;

	@Column(name = "respuesta_ia", nullable = false, columnDefinition = "TEXT")
	private String respuestaIa;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

}