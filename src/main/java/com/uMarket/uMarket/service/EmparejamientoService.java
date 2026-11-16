package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.DemandaResponseDTO;
import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.event.AlertaInternaEvent;
import com.uMarket.uMarket.event.ProductoActivadoEvent;
import com.uMarket.uMarket.event.TipoAlerta;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.Demanda;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.model.Usuario;
import com.uMarket.uMarket.repository.DemandaRepository;
import com.uMarket.uMarket.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmparejamientoService {

	private static final int LIMITE_SUGERENCIAS = 10;

	private static final String ESTADO_PRODUCTO_ACTIVO = "ACTIVO";

	private static final Set<String> STOPWORDS = Set.of(
			"busco", "busca", "buscan", "necesito", "quiero", "compro",
			"de", "la", "el", "en", "y", "a", "los", "las", "del", "al",
			"una", "uno", "un", "con", "para", "por", "que", "se", "me",
			"mi", "tu", "su", "es", "son", "the", "and", "for", "vendo",
			"vende", "venta", "compra");

	/** Peso de cada criterio en el score final (suman 1.0). */
	private static final double PESO_CATEGORIA = 0.30;
	private static final double PESO_TEXTO = 0.30;
	private static final double PESO_TAGS = 0.10;
	private static final double PESO_PRECIO = 0.15;
	private static final double PESO_CAMPUS = 0.10;
	private static final double PESO_RECENCIA = 0.05;

	private final DemandaRepository demandaRepository;
	private final ProductoRepository productoRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Value("${matching.umbral:0.25}")
	private double umbral = 0.25;

	@Value("${matching.candidatos-max:100}")
	private int candidatosMax = 100;

	public EmparejamientoService(DemandaRepository demandaRepository,
			ProductoRepository productoRepository,
			ApplicationEventPublisher eventPublisher) {
		this.demandaRepository = demandaRepository;
		this.productoRepository = productoRepository;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Sugiere productos ACTIVO de otros usuarios que mejor coincidan con la
	 * demanda, ordenados por un score de relevancia (categoría, texto, etiquetas,
	 * precio, campus y recencia). Publica una alerta si hay sugerencias por
	 * encima del umbral.
	 */
	@Transactional(readOnly = true)
	public List<ProductoDto> buscarProductosSugeridosParaDemanda(Long demandaId) {
		Demanda demanda = demandaRepository.findById(demandaId)
				.orElseThrow(() -> new ResourceNotFoundException("Demanda no encontrada con id: " + demandaId));

		Set<String> tokensDemanda = tokens(demanda.getTitulo(), demanda.getDescripcion());
		Set<String> tagsDemanda = tagsDe(demanda);
		if (demanda.getCategoria() == null && tokensDemanda.isEmpty() && tagsDemanda.isEmpty()) {
			return List.of();
		}

		String keyword = extraerPalabraClave(demanda.getTitulo(), demanda.getDescripcion());
		if (keyword.isEmpty() && !tagsDemanda.isEmpty()) {
			keyword = tagsDemanda.iterator().next();
		}
		Pageable limite = PageRequest.of(0, candidatosMax);
		String texto = keyword.isEmpty() ? "8f3a2c9e1b7d4a6f0e2c5b8d" : keyword;

		List<Producto> candidatos = productoRepository.buscarSugeridosParaDemanda(
				demanda.getCategoria(),
				texto,
				demanda.getCampus(),
				demanda.getUsuario().getId(),
				limite);

		List<ProductoDto> sugeridos = candidatos.stream()
				.filter(p -> p.getEstado() != null && ESTADO_PRODUCTO_ACTIVO.equalsIgnoreCase(p.getEstado()))
				.map(p -> new Scored(p, calcularScore(demanda, p, tokensDemanda)))
				.filter(s -> s.score >= umbral)
				.sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed()
						.thenComparing(s -> s.producto.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(LIMITE_SUGERENCIAS)
				.map(s -> ProductoDto.from(s.producto))
				.toList();

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
	 * Demanda inversa: demandas ACTIVAS de otros usuarios compatibles con un
	 * producto publicado. Solo el dueño de la publicación puede consultarlas.
	 */
	@Transactional(readOnly = true)
	public List<DemandaResponseDTO> buscarDemandasCompatiblesParaProducto(Long productoId, Usuario solicitante) {
		Producto producto = productoRepository.findById(productoId)
				.orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));

		if (solicitante == null || !producto.getUsuario().getId().equals(solicitante.getId())) {
			throw new AccessDeniedException("Solo el dueño de la publicación puede ver las demandas compatibles");
		}

		return demandasCompatibles(producto).stream()
				.map(DemandaResponseDTO::from)
				.toList();
	}

	/**
	 * Se ejecuta cuando el pipeline activa una publicación. Busca demandas
	 * activas compatibles y genera una alerta para cada autor.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onProductoActivado(ProductoActivadoEvent event) {
		notificarDemandasCompatibles(event.productoId());
	}

	/**
	 * Notifica a los autores de las demandas compatibles. Pensado para la
	 * demanda inversa: el vendedor publica y quienes buscaban ese producto
	 * reciben la alerta.
	 */
	@Transactional(readOnly = true)
	public void notificarDemandasCompatibles(Long productoId) {
		Producto producto = productoRepository.findById(productoId).orElse(null);
		if (producto == null || !ESTADO_PRODUCTO_ACTIVO.equalsIgnoreCase(producto.getEstado())) {
			return;
		}

		for (Demanda demanda : demandasCompatibles(producto)) {
			String campus = producto.getCampus() == null ? "" : " Campus: " + producto.getCampus() + ".";
			eventPublisher.publishEvent(new AlertaInternaEvent(
					demanda.getUsuario().getId(),
					TipoAlerta.MATCH_ENCONTRADO,
					"Hay una publicación que coincide con tu búsqueda",
					"El producto '" + producto.getTitulo() + "' coincide con tu demanda '"
							+ demanda.getTitulo() + "'." + campus
			));
		}
	}

	private List<Demanda> demandasCompatibles(Producto producto) {
		String keyword = extraerPalabraClave(producto.getTitulo(), producto.getDescripcion());
		String categoria = producto.getCategoria() == null ? "" : producto.getCategoria();
		String campus = producto.getCampus() == null ? "" : producto.getCampus();
		if (keyword.isEmpty() && categoria.isBlank() && campus.isBlank()) {
			return List.of();
		}

		Pageable pageable = PageRequest.of(0, candidatosMax);
		return demandaRepository.buscarCandidatasParaProducto(
						categoria, keyword, campus, producto.getUsuario().getId(), pageable).stream()
				.map(demanda -> new ScoredDemanda(demanda, calcularScore(demanda, producto)))
				.filter(scored -> scored.score >= umbral)
				.sorted(Comparator.comparingDouble((ScoredDemanda s) -> s.score).reversed()
						.thenComparing(s -> s.demanda.getFechaCreacion(),
								Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(LIMITE_SUGERENCIAS)
				.map(scored -> scored.demanda)
				.toList();
	}

	/**
	 * Calcula el score de relevancia (0..1) de un producto para una demanda.
	 * Expuesto para pruebas unitarias.
	 */
	double calcularScore(Demanda demanda, Producto producto) {
		return calcularScore(demanda, producto, tokens(demanda.getTitulo(), demanda.getDescripcion()));
	}

	private double calcularScore(Demanda demanda, Producto producto, Set<String> tokensDemanda) {
		double score = 0.0;

		// 1) Categoría
		if (demanda.getCategoria() != null && !demanda.getCategoria().isBlank()
				&& demanda.getCategoria().equalsIgnoreCase(producto.getCategoria())) {
			score += PESO_CATEGORIA;
		} else if (producto.getCategoria() != null && tokensDemanda.contains(normalizar(producto.getCategoria()))) {
			score += PESO_CATEGORIA * 0.4;
		}

		// 2) Texto (overlap de tokens, con boost si está en el título)
		Set<String> tokensProducto = tokens(producto.getTitulo(), producto.getDescripcion());
		if (!tokensDemanda.isEmpty() && !tokensProducto.isEmpty()) {
			Set<String> interseccion = new HashSet<>(tokensDemanda);
			interseccion.retainAll(tokensProducto);
			double union = tokensDemanda.size() + tokensProducto.size() - interseccion.size();
			double jaccard = union == 0 ? 0.0 : interseccion.size() / union;

			// Boost si algún token común aparece en el título del producto.
			Set<String> tokensTituloProducto = tokens(producto.getTitulo(), null);
			long enTitulo = interseccion.stream().filter(tokensTituloProducto::contains).count();
			double boost = interseccion.isEmpty() ? 0.0 : (double) enTitulo / interseccion.size();
			double textoScore = jaccard * (1.0 + 0.25 * boost);
			score += PESO_TEXTO * Math.min(1.0, textoScore);
		}

		// 3) Etiquetas de la demanda presentes en el texto del producto.
		Set<String> tagsDemanda = tagsDe(demanda);
		if (!tagsDemanda.isEmpty()) {
			String textoProducto = normalizar(
					(producto.getTitulo() == null ? "" : producto.getTitulo()) + " "
							+ (producto.getDescripcion() == null ? "" : producto.getDescripcion()) + " "
							+ (producto.getCategoria() == null ? "" : producto.getCategoria()));
			long encontradas = tagsDemanda.stream().filter(textoProducto::contains).count();
			score += PESO_TAGS * ((double) encontradas / tagsDemanda.size());
		}

		// 4) Precio vs presupuesto
		BigDecimal presupuesto = demanda.getPresupuestoEstimado();
		BigDecimal precio = producto.getPrecio();
		if (presupuesto != null && precio != null) {
			if (precio.compareTo(presupuesto) <= 0) {
				score += PESO_PRECIO;
			} else {
				// Decaimiento suave si excede hasta 50% por encima.
				double ratio = precio.doubleValue() / presupuesto.doubleValue();
				if (ratio <= 1.5) {
					score += PESO_PRECIO * (1.5 - ratio);
				}
			}
		} else {
			score += PESO_PRECIO * 0.5;
		}

		// 5) Campus (cruce espacial): mismo campus normalizado.
		String campusDemanda = normalizarCampus(demanda.getCampus());
		String campusProducto = normalizarCampus(producto.getCampus());
		if (campusDemanda != null && campusDemanda.equals(campusProducto)) {
			score += PESO_CAMPUS;
		}

		// 6) Recencia
		LocalDateTime creado = producto.getCreatedAt();
		if (creado != null) {
			long dias = ChronoUnit.DAYS.between(creado, LocalDateTime.now());
			if (dias < 0) {
				dias = 0;
			}
			double recencia = Math.max(0.0, 1.0 - (dias / 30.0));
			score += PESO_RECENCIA * recencia;
		}

		return Math.min(1.0, Math.max(0.0, score));
	}

	/**
	 * Tokeniza y normaliza texto (título + descripción), quitando stopwords y
	 * tokens cortos. Expuesto para pruebas.
	 */
	Set<String> tokens(String titulo, String descripcion) {
		String fuente = (titulo != null ? titulo : "") + " " + (descripcion != null ? descripcion : "");
		return java.util.Arrays.stream(fuente.split("[^\\p{L}\\p{N}]+"))
				.map(this::normalizar)
				.filter(t -> t.length() >= 3 && !STOPWORDS.contains(t))
				.collect(Collectors.toCollection(HashSet::new));
	}

	private Set<String> tagsDe(Demanda demanda) {
		return demanda.getTags() == null ? Set.of() : demanda.getTags();
	}

	private String normalizarCampus(String campus) {
		if (campus == null || campus.isBlank()) {
			return null;
		}
		return normalizar(campus).trim();
	}

	private String normalizar(String token) {
		if (token == null) {
			return "";
		}
		String nfd = Normalizer.normalize(token, Normalizer.Form.NFD);
		return nfd.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
	}

	/**
	 * Extrae el token más largo y significativo del título (fallback: descripción),
	 * sin tildes, en minúsculas y sin stopwords. Si no hay ninguno útil, devuelve "".
	 */
	String extraerPalabraClave(String titulo, String descripcion) {
		String fuente = (titulo != null && !titulo.isBlank()) ? titulo : (descripcion != null ? descripcion : "");
		return java.util.Arrays.stream(fuente.split("[^\\p{L}\\p{N}]+"))
				.map(token -> Normalizer.normalize(token, Normalizer.Form.NFD)
						.replaceAll("\\p{M}", "")
						.toLowerCase(Locale.ROOT))
				.filter(token -> token.length() >= 3 && !STOPWORDS.contains(token))
				.max(Comparator.comparingInt(String::length))
				.orElse("");
	}

	private static final class Scored {
		final Producto producto;
		final double score;

		Scored(Producto producto, double score) {
			this.producto = producto;
			this.score = score;
		}
	}

	private static final class ScoredDemanda {
		final Demanda demanda;
		final double score;

		ScoredDemanda(Demanda demanda, double score) {
			this.demanda = demanda;
			this.score = score;
		}
	}
}
