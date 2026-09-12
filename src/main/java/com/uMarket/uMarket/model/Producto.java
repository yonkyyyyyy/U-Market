package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(nullable = false, length = 150)
	private String titulo;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	@Column(nullable = false, length = 30)
	private String estado = "DISPONIBLE";

	@Column(name = "created_at")
	private LocalDateTime createdAt;

}