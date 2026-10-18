package com.uMarket.uMarket.service;

import com.uMarket.uMarket.dto.MatrizClasificacion;
import com.uMarket.uMarket.dto.ModerationResultDTO;
import com.uMarket.uMarket.enums.FuenteModeracion;
import com.uMarket.uMarket.enums.ModerationStatus;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import com.uMarket.uMarket.exception.ResourceNotFoundException;
import com.uMarket.uMarket.model.RegistroModeracion;
import com.uMarket.uMarket.repository.RegistroModeracionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroModeracionServiceTest {

    @Mock
    private RegistroModeracionRepository registroRepository;

    private RegistroModeracionService service;

    @BeforeEach
    void setUp() {
        service = new RegistroModeracionService(registroRepository, "omni-moderation-latest", "gpt-4o-mini");
    }

    @Test
    void registrarTextoGuardaVeredictoCategoriasYExtractoRecortado() {
        ModerationResultDTO resultado = moderacion(ModerationStatus.REJECTED, 0.93, List.of("hate", "harassment"));

        service.registrarTexto(7L, "Producto", 3L, resultado, "x".repeat(400), "Lenguaje inapropiado", 42L);

        RegistroModeracion guardado = capturarGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoModeracion.TEXTO);
        assertThat(guardado.getFuente()).isEqualTo(FuenteModeracion.PUBLICACION);
        assertThat(guardado.getProductoId()).isEqualTo(7L);
        assertThat(guardado.getUsuarioId()).isEqualTo(3L);
        assertThat(guardado.getVeredicto()).isEqualTo(VeredictoModeracion.RECHAZADO);
        assertThat(guardado.getScore()).isEqualTo(0.93);
        assertThat(guardado.getCategorias()).isEqualTo("hate, harassment");
        assertThat(guardado.getModelo()).isEqualTo("omni-moderation-latest");
        assertThat(guardado.getLatenciaMs()).isEqualTo(42L);
        // Se guarda un extracto, nunca el texto completo.
        assertThat(guardado.getExtracto()).hasSize(RegistroModeracion.MAX_EXTRACTO);
        // Sin revisar todavía: no entra a la matriz.
        assertThat(guardado.getAciertoHumano()).isNull();
    }

    @Test
    void registrarImagenUsaElModeloDeVision() {
        ModerationResultDTO resultado = moderacion(ModerationStatus.APPROVED, 0.02, List.of());

        service.registrarImagen(7L, "Producto", 3L, resultado, null, 15L);

        RegistroModeracion guardado = capturarGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoModeracion.IMAGEN);
        assertThat(guardado.getFuente()).isEqualTo(FuenteModeracion.IMAGEN);
        assertThat(guardado.getVeredicto()).isEqualTo(VeredictoModeracion.APROBADO);
        assertThat(guardado.getModelo()).isEqualTo("gpt-4o-mini");
        // Una aprobación no tiene motivo ni extracto.
        assertThat(guardado.getMotivo()).isNull();
        assertThat(guardado.getExtracto()).isNull();
        assertThat(guardado.getCategorias()).isNull();
    }

    @Test
    void registrarTtlQuedaComoFallidoYNoComoRechazoDeContenido() {
        service.registrarTtl(7L, "Producto", 3L);

        RegistroModeracion guardado = capturarGuardado();
        assertThat(guardado.getFuente()).isEqualTo(FuenteModeracion.TTL);
        // La IA nunca respondió: no es una decisión de clasificación.
        assertThat(guardado.getVeredicto()).isEqualTo(VeredictoModeracion.FALLIDO);
        assertThat(guardado.getMotivo()).containsIgnoringCase("caducó");
    }

    @Test
    void cambiarDeModerationStatusAVeredicto() {
        assertThat(VeredictoModeracion.desde(ModerationStatus.APPROVED)).isEqualTo(VeredictoModeracion.APROBADO);
        assertThat(VeredictoModeracion.desde(ModerationStatus.REJECTED)).isEqualTo(VeredictoModeracion.RECHAZADO);
        assertThat(VeredictoModeracion.desde(ModerationStatus.FAILED)).isEqualTo(VeredictoModeracion.FALLIDO);
    }

    @Test
    void calcularMatrizDerivaLosCuatroCuadrantesDelFeedbackHumano() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 12, 31, 23, 59);
        when(registroRepository.contarPorVeredicto(desde, hasta)).thenReturn(List.<Object[]>of(
                new Object[]{VeredictoModeracion.RECHAZADO, 3L},
                new Object[]{VeredictoModeracion.APROBADO, 5L},
                new Object[]{VeredictoModeracion.FALLIDO, 1L}));
        when(registroRepository.contarRevisadosPorVeredictoYAcierto(desde, hasta)).thenReturn(List.<Object[]>of(
                new Object[]{VeredictoModeracion.RECHAZADO, true, 2L},   // VP: rechazó y era prohibido
                new Object[]{VeredictoModeracion.RECHAZADO, false, 1L},  // FP: rechazó y era válido
                new Object[]{VeredictoModeracion.APROBADO, true, 3L},    // VN: aprobó y estaba bien
                new Object[]{VeredictoModeracion.APROBADO, false, 1L})); // FN: aprobó y era prohibido

        MatrizClasificacion matriz = service.calcularMatriz(desde, hasta);

        assertThat(matriz.verdaderoPositivo()).isEqualTo(2);
        assertThat(matriz.falsoPositivo()).isEqualTo(1);
        assertThat(matriz.falsoNegativo()).isEqualTo(1);
        assertThat(matriz.verdaderoNegativo()).isEqualTo(3);
        assertThat(matriz.revisados()).isEqualTo(7);
        assertThat(matriz.fallidos()).isEqualTo(1);
        assertThat(matriz.total()).isEqualTo(9);
        // 9 en total, 1 fallido (no medible) y 7 revisados ⇒ 1 pendiente de revisar.
        assertThat(matriz.pendientesDeRevision()).isEqualTo(1);
        assertThat(matriz.exactitud()).isEqualTo(5d / 7d, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(matriz.precision()).isEqualTo(2d / 3d, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(matriz.recall()).isEqualTo(2d / 3d, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(matriz.f1()).isEqualTo(2d / 3d, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void calcularMatrizSinRevisionesNoDivideEntreCero() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 12, 31, 23, 59);
        when(registroRepository.contarPorVeredicto(desde, hasta)).thenReturn(List.<Object[]>of(
                new Object[]{VeredictoModeracion.APROBADO, 4L}));
        when(registroRepository.contarRevisadosPorVeredictoYAcierto(desde, hasta)).thenReturn(List.of());

        MatrizClasificacion matriz = service.calcularMatriz(desde, hasta);

        assertThat(matriz.revisados()).isZero();
        assertThat(matriz.pendientesDeRevision()).isEqualTo(4);
        assertThat(matriz.exactitud()).isZero();
        assertThat(matriz.precision()).isZero();
        assertThat(matriz.recall()).isZero();
        assertThat(matriz.f1()).isZero();
    }

    @Test
    void calcularMatrizSinFechasConsultaElRangoCompletoYNoPasaNullAlRepositorio() {
        // Comparar un parámetro contra NULL en JPQL funciona en H2 pero PostgreSQL lo
        // rechaza ("could not determine data type of parameter"); el servicio jamás
        // debe mandar null.
        when(registroRepository.contarPorVeredicto(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(registroRepository.contarRevisadosPorVeredictoYAcierto(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        service.calcularMatriz(null, null);

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(registroRepository).contarPorVeredicto(desde.capture(), hasta.capture());
        assertThat(desde.getValue()).isNotNull().isBefore(LocalDateTime.of(2000, 1, 1, 0, 0));
        assertThat(hasta.getValue()).isNotNull().isAfter(LocalDateTime.of(9000, 1, 1, 0, 0));
    }

    @Test
    void calcularMatrizPasaElRangoDeFechasAlRepositorio() {
        LocalDateTime desde = LocalDateTime.now().minusDays(7);
        LocalDateTime hasta = LocalDateTime.now();
        when(registroRepository.contarPorVeredicto(desde, hasta)).thenReturn(List.of());
        when(registroRepository.contarRevisadosPorVeredictoYAcierto(desde, hasta)).thenReturn(List.of());

        service.calcularMatriz(desde, hasta);

        verify(registroRepository).contarPorVeredicto(desde, hasta);
        verify(registroRepository).contarRevisadosPorVeredictoYAcierto(desde, hasta);
    }

    @Test
    void listarSinFiltrosUsaElPaginadoCompleto() {
        Pageable pageable = PageRequest.of(0, 20);
        when(registroRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        Page<RegistroModeracion> pagina = service.listar(null, null, pageable);

        assertThat(pagina).isEmpty();
        verify(registroRepository).findAll(pageable);
    }

    @Test
    void listarConAmbosFiltrosCombinaTipoYVeredicto() {
        Pageable pageable = PageRequest.of(0, 20);
        when(registroRepository.findByTipoAndVeredicto(TipoModeracion.IMAGEN, VeredictoModeracion.RECHAZADO, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        service.listar(TipoModeracion.IMAGEN, VeredictoModeracion.RECHAZADO, pageable);

        verify(registroRepository).findByTipoAndVeredicto(TipoModeracion.IMAGEN, VeredictoModeracion.RECHAZADO, pageable);
    }

    @Test
    void revisarGuardaElFeedbackHumanoYNormalizaElComentario() {
        RegistroModeracion registro = new RegistroModeracion();
        registro.setId(1L);
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registro));
        when(registroRepository.save(any(RegistroModeracion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistroModeracion revisado = service.revisar(1L, false, "  era válido  ", 9L);

        assertThat(revisado.getAciertoHumano()).isFalse();
        assertThat(revisado.getRevisadoPor()).isEqualTo(9L);
        assertThat(revisado.getFechaRevision()).isNotNull();
        assertThat(revisado.getComentarioRevision()).isEqualTo("era válido");
    }

    @Test
    void revisarLanza404SiElLogNoExiste() {
        when(registroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revisar(99L, true, null, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(registroRepository, never()).save(any());
    }

    private RegistroModeracion capturarGuardado() {
        ArgumentCaptor<RegistroModeracion> captor = ArgumentCaptor.forClass(RegistroModeracion.class);
        verify(registroRepository).save(captor.capture());
        return captor.getValue();
    }

    private ModerationResultDTO moderacion(ModerationStatus status, double score, List<String> categorias) {
        ModerationResultDTO resultado = new ModerationResultDTO();
        resultado.setStatus(status);
        resultado.setConfidenceScore(score);
        resultado.setCategoriasDetectadas(categorias);
        return resultado;
    }
}
