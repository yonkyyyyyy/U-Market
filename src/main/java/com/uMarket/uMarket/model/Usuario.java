package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(name = "correo_institucional", nullable = false, unique = true, length = 150)
	private String correoInstitucional;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false, length = 50)
	private String rol = "ESTUDIANTE";

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Producto> productos = new ArrayList<>();

	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Demanda> demandas = new ArrayList<>();

	@OneToMany(mappedBy = "remitente", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Mensaje> mensajesEnviados = new ArrayList<>();

	@OneToMany(mappedBy = "destinatario", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Mensaje> mensajesRecibidos = new ArrayList<>();

	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChatbotLog> chatbotLogs = new ArrayList<>();

}