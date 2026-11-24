package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.MetricasProductoResponse;
import com.uMarket.uMarket.dto.RegistrarInteraccionRequest;
import com.uMarket.uMarket.enums.TipoEvento;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.InteraccionEvento;
import com.uMarket.uMarket.repository.InteraccionRepository;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnaliticaServiceImpl implements AnaliticaService {

	private final InteraccionRepository interaccionRepository;
	private final ProductoRepository productoRepository;

	public AnaliticaServiceImpl(InteraccionRepository interaccionRepository, ProductoRepository productoRepository) {
		this.interaccionRepository = interaccionRepository;
		this.productoRepository = productoRepository;
	}

	@Override
	@Transactional
	public void registrar(RegistrarInteraccionRequest request) {
		if (!productoRepository.existsById(request.productoId())) {
			throw new ResourceNotFoundException("Producto no encontrado: " + request.productoId());
		}
		InteraccionEvento ev = new InteraccionEvento();
		ev.setProductoId(request.productoId());
		ev.setUsuarioId(request.usuarioId());
		ev.setTipoEvento(request.tipoEvento());
		interaccionRepository.save(ev);
	}

	@Override
	@Transactional(readOnly = true)
	public MetricasProductoResponse metricas(Long productoId) {
		if (!productoRepository.existsById(productoId)) {
			throw new ResourceNotFoundException("Producto no encontrado: " + productoId);
		}
		long clicks = interaccionRepository.countByProductoIdAndTipoEvento(productoId, TipoEvento.CLICK);
		long vistas = interaccionRepository.countByProductoIdAndTipoEvento(productoId, TipoEvento.VISTA);
		return new MetricasProductoResponse(productoId, clicks, vistas);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MetricasProductoResponse> resumen() {
		List<Object[]> rows = interaccionRepository.topProductosPorTipo(TipoEvento.CLICK, PageRequest.of(0, 10));
		return rows.stream()
				.map(r -> new MetricasProductoResponse((Long) r[0], ((Number) r[1]).longValue(), 0L))
				.toList();
	}
}
