package com.uMarket.uMarket.model;

import com.uMarket.uMarket.enums.TipoEvento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "interaccion_eventos")
@Getter
@Setter
@NoArgsConstructor
public class InteraccionEvento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "producto_id", nullable = false)
	private Long productoId;

	@Column(name = "usuario_id")
	private Long usuarioId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_evento", nullable = false, length = 20)
	private TipoEvento tipoEvento;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	@PrePersist
	void prePersist() {
		if (creadoEn == null) {
			creadoEn = Instant.now();
		}
	}
}
