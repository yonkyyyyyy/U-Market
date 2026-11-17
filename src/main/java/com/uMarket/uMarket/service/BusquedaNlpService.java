package com.uMarket.uMarket.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import com.uMarket.uMarket.dto.BusquedaChatResponse;
import com.uMarket.uMarket.dto.BusquedaNlpResponse;
import com.uMarket.uMarket.dto.InterpretacionBusqueda;
import com.uMarket.uMarket.dto.InterpretacionLlm;
import com.uMarket.uMarket.dto.ProductoDto;
import com.uMarket.uMarket.model.Producto;
import com.uMarket.uMarket.repository.ProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Búsqueda de productos en lenguaje natural.
 *
 * <p>Flujo:</p>
 * <ol>
 *   <li>Se interpreta la consulta con un LLM ({@code /v1/chat/completions} con
 *       {@code response_format = json_object}) para extraer palabras clave,
 *       categoría, campus y presupuesto máximo.</li>
 *   <li>Si no hay API key, el LLM falla o devuelve algo inválido, se usa un
 *       parser heurístico local. La búsqueda nunca deja de funcionar.</li>
 *   <li>Los criterios extraídos se convierten en una consulta parametrizada a
 *       la base de datos; los resultados se ordenan en Java por relevancia.</li>
 * </ol>
 */
@Slf4j
@Service
public class BusquedaNlpService {

	private static final String ESTADO_ACTIVO = "ACTIVO";
	private static final String ORIGEN_LLM = "LLM";
	private static final String ORIGEN_HEURISTICA = "HEURISTICA";
	private static final String ENDPOINT_CHAT = "/v1/chat/completions";
	private static final int LIMITE_RESULTADOS = 10;
	private static final int MAX_PALABRAS_CLAVE = 8;
	private static final BigDecimal PRECIO_TOPE = new BigDecimal("1000000");
	private static final String CARACTERES_CON_TILDE = "áéíóúüñ";
	private static final String CARACTERES_SIN_TILDE = "aeiouun";

	private static final List<String> CATEGORIAS = List.of(
			"Libros y apuntes",
			"Tecnología",
			"Accesorios",
			"Hogar y dormitorio",
			"Ropa y cuidado personal",
			"Otros");

	private static final List<String> CAMPUSES = List.of(
			"Lima Centro", "Lima Este", "Lima Norte", "Lima Sur", "Ate", "Callao",
			"Arequipa", "Chiclayo", "Chimbote", "Huancayo", "Ica", "Piura", "Trujillo");

	private static final Map<String, String> CAMPUS_POR_DIRECCION = Map.of(
			"centro", "Lima Centro",
			"este", "Lima Este",
			"norte", "Lima Norte",
			"sur", "Lima Sur");

	/** Sinónimos cotidianos que ayudan al parser heurístico a inferir categoría. */
	private static final Map<String, List<String>> SINONIMOS_CATEGORIA = Map.of(
			"Tecnología", List.of(
					"tecnologia", "laptop", "laptops", "computadora", "computador", "pc", "celular",
					"telefono", "tablet", "monitor", "teclado", "mouse", "audifonos", "cargador",
					"calculadora", "impresora", "usb", "disco", "memoria"),
			"Libros y apuntes", List.of(
					"libro", "libros", "apunte", "apuntes", "cuaderno", "manual", "texto", "textos", "pdf"),
			"Accesorios", List.of(
					"accesorio", "accesorios", "mochila", "funda", "cable", "adaptador", "correa"),
			"Hogar y dormitorio", List.of(
					"hogar", "dormitorio", "cama", "colchon", "lampara", "silla", "escritorio",
					"mesa", "olla", "utensilio"),
			"Ropa y cuidado personal", List.of(
					"ropa", "casaca", "zapatos", "polera", "buzo", "pantalon", "perfume"),
			"Otros", List.of());

	private static final Set<String> STOPWORDS = Set.of(
			"busco", "busca", "buscan", "buscando", "necesito", "quiero", "quisiera", "compro",
			"comprar", "vendo", "vende", "venta", "algo", "algun", "alguna", "alguien",
			"de", "la", "el", "en", "y", "o", "a", "los", "las", "del", "al", "una", "uno", "un",
			"con", "para", "por", "que", "se", "me", "mi", "tu", "su", "es", "son",
			"hasta", "menos", "mas", "maximo", "max", "presupuesto", "precio", "costo",
			"soles", "sol", "s", "campus", "sede", "universidad", "utp",
			"barato", "barata", "economico", "economica", "buen", "buena", "estado",
			"favor", "porfa", "the", "and", "for");

	private static final List<Pattern> PATRONES_PRECIO = List.of(
			Pattern.compile("(?:s\\s*/\\s*|s/\\s*)(\\d+(?:[.,]\\d{1,2})?)"),
			Pattern.compile("(?:hasta|maximo|max|menos de|no mas de|presupuesto(?: de)?|como maximo)"
					+ "\\s+(?:s\\s*/\\s*|soles?\\s+)?(\\d+(?:[.,]\\d{1,2})?)"),
			Pattern.compile("(\\d+(?:[.,]\\d{1,2})?)\\s*(?:soles|soles peruanos|s\\b)"));

	private static final String PROMPT_SISTEMA = """
			Eres un asistente que interpreta búsquedas de un mercado universitario peruano.
			Devuelve SOLO un objeto JSON válido, sin markdown ni explicaciones, con estas claves:
			{"palabrasClave": ["..."], "categoria": "..." o null, "campus": "..." o null,
			 "precioMaximo": número o null, "tags": ["..."]}

			Reglas:
			- palabrasClave: los productos u objetos buscados, en minúsculas y sin tildes. No incluyas
			  conectores, verbos de búsqueda ni el campus.
			- categoria: SOLO si se deduce claramente. Valores válidos: %s.
			- campus: SOLO si se menciona. Valores válidos: %s.
			- precioMaximo: presupuesto máximo en soles, solo si se menciona. Número, sin símbolos.
			- tags: términos adicionales útiles para desambiguar (marca, modelo, condición).
			- Si un dato no aparece en la consulta, usa null (o [] para las listas). No inventes datos.
			""".formatted(String.join(", ", CATEGORIAS), String.join(", ", CAMPUSES));

	private final RestClient openaiRestClient;
	private final ProductoRepository productoRepository;
	private final ObjectMapper objectMapper;
	private final String model;
	private final boolean llmHabilitada;
	private final String apiKey;
	private final int candidatosMax;

	public BusquedaNlpService(RestClient openaiRestClient,
			ProductoRepository productoRepository,
			ObjectMapper objectMapper,
			@Value("${openai.busqueda-model:gpt-4o-mini}") String model,
			@Value("${openai.busqueda-habilitada:true}") boolean llmHabilitada,
			@Value("${openai.api-key:}") String apiKey,
			@Value("${matching.candidatos-max:100}") int candidatosMax) {
		this.openaiRestClient = openaiRestClient;
		this.productoRepository = productoRepository;
		this.objectMapper = objectMapper;
		this.model = model;
		this.llmHabilitada = llmHabilitada;
		this.apiKey = apiKey;
		this.candidatosMax = candidatosMax;
	}

	/**
	 * Interpreta la consulta y devuelve los productos ACTIVO más relevantes.
	 *
	 * @param consulta texto libre escrito por el usuario
	 * @return consulta original, interpretación y resultados ordenados por relevancia
	 */
	@Transactional(readOnly = true)
	public BusquedaNlpResponse buscar(String consulta) {
		String q = consulta == null ? "" : consulta.trim();
		if (q.isEmpty()) {
			InterpretacionBusqueda vacia = new InterpretacionBusqueda(
					List.of(), null, null, null, List.of(), ORIGEN_HEURISTICA);
			return new BusquedaNlpResponse(q, vacia, List.of());
		}

		InterpretacionBusqueda interpretacion = interpretar(q);
		if (interpretacion.vacia()) {
			return new BusquedaNlpResponse(q, interpretacion, List.of());
		}

		List<Producto> candidatos = productoRepository.buscarCandidatosNlp(
				criterioBusqueda(interpretacion),
				PageRequest.of(0, candidatosMax, Sort.by(Sort.Direction.DESC, "createdAt")));

		List<ProductoDto> resultados = candidatos.stream()
				.map(producto -> new Scored(producto, puntuar(interpretacion, producto)))
				.sorted(Comparator.comparingDouble((Scored s) -> s.score()).reversed()
						.thenComparing(s -> s.producto().getCreatedAt(),
								Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(LIMITE_RESULTADOS)
				.map(scored -> ProductoDto.from(scored.producto()))
				.toList();

		return new BusquedaNlpResponse(q, interpretacion, resultados);
	}

	Specification<Producto> criterioBusqueda(InterpretacionBusqueda interpretacion) {
		return (root, query, builder) -> {
			List<Predicate> criterios = new ArrayList<>();
			criterios.add(builder.equal(root.get("estado"), ESTADO_ACTIVO));

			if (interpretacion.categoria() != null) {
				criterios.add(builder.equal(
						builder.lower(root.<String>get("categoria")),
						interpretacion.categoria().toLowerCase(Locale.ROOT)));
			}
			if (interpretacion.campus() != null) {
				criterios.add(builder.equal(
						builder.lower(root.<String>get("campus")),
						interpretacion.campus().toLowerCase(Locale.ROOT)));
			}
			if (interpretacion.precioMaximo() != null) {
				criterios.add(builder.lessThanOrEqualTo(
						root.<BigDecimal>get("precio"), interpretacion.precioMaximo()));
			}

			Set<String> terminos = new LinkedHashSet<>();
			terminos.addAll(limpiarLista(interpretacion.palabrasClave()));
			terminos.addAll(limpiarLista(interpretacion.tags()));
			if (!terminos.isEmpty()) {
				List<Predicate> coincidencias = new ArrayList<>();
				for (String termino : terminos) {
					String patron = "%" + escaparLike(normalizar(termino)) + "%";
					for (String campo : List.of("titulo", "descripcion", "categoria", "campus")) {
						coincidencias.add(builder.like(
								normalizarSql(builder, root.<String>get(campo)), patron, '\\'));
					}
				}
				criterios.add(builder.or(coincidencias.toArray(Predicate[]::new)));
			}

			return builder.and(criterios.toArray(Predicate[]::new));
		};
	}

	private Expression<String> normalizarSql(
			CriteriaBuilder builder, Expression<String> campo) {
		Expression<String> normalizado = builder.lower(campo);
		for (int i = 0; i < CARACTERES_CON_TILDE.length(); i++) {
			normalizado = builder.function("replace", String.class, normalizado,
					builder.literal(String.valueOf(CARACTERES_CON_TILDE.charAt(i))),
					builder.literal(String.valueOf(CARACTERES_SIN_TILDE.charAt(i))));
		}
		return normalizado;
	}

	private String escaparLike(String valor) {
		return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private InterpretacionBusqueda interpretar(String consulta) {
		InterpretacionBusqueda heuristica = interpretarHeuristicamente(consulta);
		if (!llmHabilitada || apiKey == null || apiKey.isBlank()) {
			return heuristica;
		}
		try {
			InterpretacionLlm llm = llamarLlm(consulta);
			return fusionar(llm, heuristica);
		} catch (RuntimeException e) {
			// Sin cuota, con key inválida o con JSON inesperado: la búsqueda sigue
			// funcionando con reglas locales.
			log.warn("No se pudo interpretar la búsqueda con el LLM ({}). Se usa el parser heurístico.",
					e.getMessage());
			return heuristica;
		}
	}

	private InterpretacionLlm llamarLlm(String consulta) {
		Map<String, Object> cuerpo = new HashMap<>();
		cuerpo.put("model", model);
		cuerpo.put("temperature", 0.0);
		cuerpo.put("response_format", Map.of("type", "json_object"));
		cuerpo.put("messages", List.of(
				Map.of("role", "system", "content", PROMPT_SISTEMA),
				Map.of("role", "user", "content", consulta)));

		BusquedaChatResponse response = openaiRestClient.post()
				.uri(ENDPOINT_CHAT)
				.body(cuerpo)
				.retrieve()
				.body(BusquedaChatResponse.class);

		String contenido = response == null ? null : response.contenido();
		if (contenido == null || contenido.isBlank()) {
			throw new IllegalStateException("respuesta vacía del modelo");
		}
		return objectMapper.readValue(contenido, InterpretacionLlm.class);
	}

	/** Completa lo que el LLM no haya devuelto con lo detectado por las reglas. */
	private InterpretacionBusqueda fusionar(InterpretacionLlm llm, InterpretacionBusqueda heuristica) {
		List<String> palabras = limpiarLista(llm.palabrasClave());
		if (palabras.isEmpty()) {
			palabras = heuristica.palabrasClave();
		}
		String categoria = canonizarCategoria(llm.categoria());
		if (categoria == null) {
			categoria = heuristica.categoria();
		}
		String campus = canonizarCampus(llm.campus());
		if (campus == null) {
			campus = heuristica.campus();
		}
		BigDecimal precio = sanearPrecio(llm.precioMaximo());
		if (precio == null) {
			precio = heuristica.precioMaximo();
		}
		List<String> tags = limpiarLista(llm.tags());
		if (tags.isEmpty()) {
			tags = heuristica.tags();
		}
		return new InterpretacionBusqueda(palabras, categoria, campus, precio, tags, ORIGEN_LLM);
	}

	private InterpretacionBusqueda interpretarHeuristicamente(String consulta) {
		String normalizada = normalizar(consulta);
		BigDecimal precio = extraerPrecioMaximo(normalizada);
		String categoria = detectarCategoria(normalizada);
		String campus = detectarCampus(normalizada);
		List<String> palabras = extraerPalabrasClave(normalizada, categoria, campus);
		return new InterpretacionBusqueda(palabras, categoria, campus, precio, List.of(), ORIGEN_HEURISTICA);
	}

	private String detectarCategoria(String consultaNormalizada) {
		for (String categoria : CATEGORIAS) {
			if (contieneFrase(consultaNormalizada, normalizar(categoria))) {
				return categoria;
			}
		}
		for (Map.Entry<String, List<String>> entry : SINONIMOS_CATEGORIA.entrySet()) {
			for (String sinonimo : entry.getValue()) {
				if (contieneFrase(consultaNormalizada, normalizar(sinonimo))) {
					return entry.getKey();
				}
			}
		}
		return null;
	}

	private String detectarCampus(String consultaNormalizada) {
		for (String campus : CAMPUSES) {
			if (contieneFrase(consultaNormalizada, normalizar(campus))) {
				return campus;
			}
		}
		// "campus sur" / "sede norte" sin el nombre completo del campus.
		for (Map.Entry<String, String> entry : CAMPUS_POR_DIRECCION.entrySet()) {
			String direccion = entry.getKey();
			if (contieneFrase(consultaNormalizada, "campus " + direccion)
					|| contieneFrase(consultaNormalizada, "sede " + direccion)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private BigDecimal extraerPrecioMaximo(String consultaNormalizada) {
		for (Pattern patron : PATRONES_PRECIO) {
			Matcher matcher = patron.matcher(consultaNormalizada);
			if (matcher.find()) {
				try {
					BigDecimal valor = new BigDecimal(matcher.group(1).replace(',', '.'));
					return sanearPrecio(valor);
				} catch (NumberFormatException ignored) {
					// Se prueba el siguiente patrón.
				}
			}
		}
		return null;
	}

	private List<String> extraerPalabrasClave(String consultaNormalizada, String categoria, String campus) {
		Set<String> excluidas = new HashSet<>(STOPWORDS);
		for (String nombre : CATEGORIAS) {
			excluidas.addAll(Arrays.asList(normalizar(nombre).split("\\s+")));
		}
		if (campus != null) {
			excluidas.addAll(Arrays.asList(normalizar(campus).split("\\s+")));
		}
		return Arrays.stream(consultaNormalizada.split("[^\\p{L}\\p{N}]+"))
				.map(String::trim)
				.filter(token -> token.length() >= 3)
				.filter(token -> token.chars().anyMatch(Character::isLetter))
				.filter(token -> !excluidas.contains(token))
				.distinct()
				.limit(MAX_PALABRAS_CLAVE)
				.collect(Collectors.toList());
	}

	private boolean coincideCategoria(String filtro, String categoriaProducto) {
		if (filtro == null || filtro.isBlank()) {
			return true;
		}
		return categoriaProducto != null && normalizar(filtro).equals(normalizar(categoriaProducto));
	}

	private boolean coincideCampus(String filtro, String campusProducto) {
		if (filtro == null || filtro.isBlank()) {
			return true;
		}
		return campusProducto != null && normalizar(filtro).equals(normalizar(campusProducto));
	}

	private double puntuar(InterpretacionBusqueda interpretacion, Producto producto) {
		String texto = textoProducto(producto);
		double score = 0.0;

		if (!interpretacion.palabrasClave().isEmpty()) {
			long encontradas = interpretacion.palabrasClave().stream()
					.filter(palabra -> coincideTexto(palabra, texto))
					.count();
			double cobertura = (double) encontradas / interpretacion.palabrasClave().size();
			String titulo = normalizar(producto.getTitulo());
			long enTitulo = interpretacion.palabrasClave().stream()
					.filter(palabra -> coincideTexto(palabra, titulo))
					.count();
			double boostTitulo = encontradas == 0 ? 0.0 : (double) enTitulo / encontradas;
			score += 0.55 * cobertura * (1.0 + 0.35 * boostTitulo);
		}

		if (interpretacion.categoria() != null
				&& coincideCategoria(interpretacion.categoria(), producto.getCategoria())) {
			score += 0.25;
		}
		if (interpretacion.campus() != null
				&& coincideCampus(interpretacion.campus(), producto.getCampus())) {
			score += 0.10;
		}
		if (!interpretacion.tags().isEmpty()) {
			long encontradas = interpretacion.tags().stream()
					.filter(tag -> coincideTexto(normalizar(tag), texto))
					.count();
			score += 0.10 * ((double) encontradas / interpretacion.tags().size());
		}
		if (interpretacion.precioMaximo() != null && producto.getPrecio() != null
				&& producto.getPrecio().compareTo(interpretacion.precioMaximo()) <= 0) {
			score += 0.10;
		}

		score += 0.05 * recencia(producto.getCreatedAt());
		return Math.min(1.0, Math.max(0.0, score));
	}

	private double recencia(LocalDateTime creado) {
		if (creado == null) {
			return 0.0;
		}
		long dias = ChronoUnit.DAYS.between(creado, LocalDateTime.now());
		if (dias < 0) {
			dias = 0;
		}
		return Math.max(0.0, 1.0 - (dias / 30.0));
	}

	private String textoProducto(Producto producto) {
		return normalizar((producto.getTitulo() == null ? "" : producto.getTitulo()) + " "
				+ (producto.getDescripcion() == null ? "" : producto.getDescripcion()) + " "
				+ (producto.getCategoria() == null ? "" : producto.getCategoria()) + " "
				+ (producto.getCampus() == null ? "" : producto.getCampus()));
	}

	private boolean coincideTexto(String palabra, String texto) {
		if (palabra == null || palabra.isBlank() || texto == null || texto.isBlank()) {
			return false;
		}
		String buscada = normalizar(palabra);
		if (texto.contains(buscada)) {
			return true;
		}
		// Coincidencia básica de plural: "laptops" → "laptop".
		return buscada.length() > 4 && buscada.endsWith("s")
				&& texto.contains(buscada.substring(0, buscada.length() - 1));
	}

	private List<String> limpiarLista(List<String> valores) {
		if (valores == null) {
			return List.of();
		}
		Set<String> limpios = new LinkedHashSet<>();
		for (String valor : valores) {
			String normalizado = normalizar(valor);
			if (!normalizado.isBlank() && normalizado.length() >= 2) {
				limpios.add(normalizado);
			}
			if (limpios.size() >= MAX_PALABRAS_CLAVE) {
				break;
			}
		}
		return new ArrayList<>(limpios);
	}

	private String canonizarCategoria(String categoria) {
		if (categoria == null || categoria.isBlank()) {
			return null;
		}
		for (String opcion : CATEGORIAS) {
			if (normalizar(opcion).equals(normalizar(categoria))) {
				return opcion;
			}
		}
		return categoria.trim();
	}

	private String canonizarCampus(String campus) {
		if (campus == null || campus.isBlank()) {
			return null;
		}
		for (String opcion : CAMPUSES) {
			if (normalizar(opcion).equals(normalizar(campus))) {
				return opcion;
			}
		}
		return campus.trim();
	}

	private BigDecimal sanearPrecio(BigDecimal precio) {
		if (precio == null || precio.signum() <= 0 || precio.compareTo(PRECIO_TOPE) > 0) {
			return null;
		}
		return precio;
	}

	private boolean contieneFrase(String texto, String frase) {
		if (texto == null || frase == null || frase.isBlank()) {
			return false;
		}
		return Pattern.compile("(^|\\s)" + Pattern.quote(frase) + "($|\\s)")
				.matcher(texto)
				.find();
	}

	private String normalizar(String valor) {
		if (valor == null) {
			return "";
		}
		return Normalizer.normalize(valor, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.trim();
	}

	private record Scored(Producto producto, double score) {
	}
}
