package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
public class Mensaje {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "remitente_id", nullable = false)
	private Usuario remitente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destinatario_id", nullable = false)
	private Usuario destinatario;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String contenido;

	@Column(name = "fecha_envio")
	private LocalDateTime fechaEnvio;

}