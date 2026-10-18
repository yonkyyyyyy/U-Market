package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.event.AlertaInternaEvent;
import com.uMarket.uMarket.event.TipoAlerta;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.repository.DemandaRepository;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EmparejamientoService {

	private static final int LIMITE_SUGERENCIAS = 10;

	private static final Set<String> STOPWORDS = Set.of(
			"busco", "busca", "buscan", "necesito", "quiero", "compro",
			"de", "la", "el", "en", "y", "a", "los", "las", "del", "al",
			"una", "uno", "un", "con", "para", "por", "que", "se", "me",
			"mi", "tu", "su", "es", "son", "the", "and", "for");

	private final DemandaRepository demandaRepository;
	private final ProductoRepository productoRepository;
	private final ApplicationEventPublisher eventPublisher;

	public EmparejamientoService(DemandaRepository demandaRepository,
			ProductoRepository productoRepository,
			ApplicationEventPublisher eventPublisher) {
		this.demandaRepository = demandaRepository;
		this.productoRepository = productoRepository;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Sugiere productos DISPONIBLE de otros usuarios que coincidan con la
	 * categoría de la demanda o con la palabra clave más relevante de su título.
	 */
	@Transactional(readOnly = true)
	public List<ProductoDto> buscarProductosSugeridosParaDemanda(Long demandaId) {
		Demanda demanda = demandaRepository.findById(demandaId)
				.orElseThrow(() -> new ResourceNotFoundException("Demanda no encontrada con id: " + demandaId));

		String keyword = extraerPalabraClave(demanda.getTitulo(), demanda.getDescripcion());
		if (demanda.getCategoria() == null && keyword.isEmpty()) {
			return List.of();
		}

		Pageable limite = PageRequest.of(0, LIMITE_SUGERENCIAS);
		// Sin keyword útil: texto imposible de matchear para que solo aplique el filtro de categoría.
		String texto = keyword.isEmpty() ? "8f3a2c9e1b7d4a6f0e2c5b8d" : keyword;
		List<ProductoDto> sugeridos = productoRepository.buscarSugeridosParaDemanda(
				demanda.getCategoria(),
				texto,
				demanda.getUsuario().getId(),
				limite).stream().map(ProductoDto::from).toList();

		// Alerta desacoplada: no bloquea la respuesta aunque persistir la alerta tarde.
		if (!sugeridos.isEmpty()) {
			eventPublisher.publishEvent(new AlertaInternaEvent(
					demanda.getUsuario().getId(),
					TipoAlerta.MATCH_ENCONTRADO,
					"¡Encontramos coincidencias para tu búsqueda!",
					"Tu demanda '" + demanda.getTitulo() + "' tiene " + sugeridos.size()
							+ " producto(s) sugerido(s). Revísalos en el módulo Se Busca."
			));
		}
		return sugeridos;
	}

	/**
	 * Extrae el token más largo y significativo del título (fallback: descripción),
	 * sin tildes, en minúsculas y sin stopwords. Si no hay ninguno útil, devuelve "".
	 */
	String extraerPalabraClave(String titulo, String descripcion) {
		String fuente = (titulo != null && !titulo.isBlank()) ? titulo : (descripcion != null ? descripcion : "");
		return Arrays.stream(fuente.split("[^\\p{L}\\p{N}]+"))
				.map(token -> Normalizer.normalize(token, Normalizer.Form.NFD)
						.replaceAll("\\p{M}", "")
						.toLowerCase(Locale.ROOT))
				.filter(token -> token.length() >= 3 && !STOPWORDS.contains(token))
				.max(Comparator.comparingInt(String::length))
				.orElse("");
	}
}
