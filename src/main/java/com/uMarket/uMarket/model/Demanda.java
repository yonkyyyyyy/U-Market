package com.uMarket.uMarket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demandas")
@Getter
@Setter
@NoArgsConstructor
public class Demanda {

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

	@Column(name = "presupuesto_estimado", precision = 10, scale = 2)
	private BigDecimal presupuestoEstimado;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

}