package com.uMarket.uMarket.repository;

import com.uMarket.uMarket.enums.FuenteModeracion;
import com.uMarket.uMarket.enums.TipoModeracion;
import com.uMarket.uMarket.enums.VeredictoModeracion;
import com.uMarket.uMarket.model.RegistroModeracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ejecuta las consultas de agregación de la matriz contra una base REAL (H2, que es
 * lo que Spring Boot levanta para {@code @DataJpaTest}), no contra mocks.
 *
 * <p>Los tests de {@code RegistroModeracionService} mockean el repositorio, así que
 * aquí es donde se valida el JPQL: el {@code group by} de veredicto + feedback humano
 * y el rango de fechas.</p>
 *
 * <p><b>Ojo:</b> H2 es más permisivo que PostgreSQL. Un JPQL que compara un
 * parámetro contra {@code NULL} ({@code :desde is null or ...}) pasa aquí y revienta
 * en Postgres con «could not determine data type of parameter». Ese caso sólo lo
 * detecta la prueba contra Postgres real, por eso el rango de fechas es obligatorio
 * y se resuelve en el servicio.</p>
 */
@DataJpaTest
class RegistroModeracionRepositoryTest {

    /** Rango completo: es lo que manda el servicio cuando el panel no filtra por fechas. */
    private static final LocalDateTime DESDE_SIEMPRE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime HASTA_SIEMPRE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final LocalDateTime ahora = LocalDateTime.now();

    @Autowired
    private RegistroModeracionRepository registroRepository;

    @Test
    void contarPorVeredictoAgrupaYRespetaElRangoDeFechas() {
        registroRepository.save(registro(VeredictoModeracion.RECHAZADO, ahora.minusDays(1)));
        registroRepository.save(registro(VeredictoModeracion.RECHAZADO, ahora.minusDays(2)));
        registroRepository.save(registro(VeredictoModeracion.APROBADO, ahora.minusDays(3)));
        registroRepository.save(registro(VeredictoModeracion.APROBADO, ahora.minusDays(30)));
        registroRepository.save(registro(VeredictoModeracion.FALLIDO, ahora.minusDays(30)));

        // Rango completo: entra todo.
        assertThat(contar(registroRepository.contarPorVeredicto(DESDE_SIEMPRE, HASTA_SIEMPRE)))
                .containsEntry(VeredictoModeracion.RECHAZADO, 2L)
                .containsEntry(VeredictoModeracion.APROBADO, 2L)
                .containsEntry(VeredictoModeracion.FALLIDO, 1L);

        // Con "desde": deja fuera lo de hace 30 días.
        assertThat(contar(registroRepository.contarPorVeredicto(ahora.minusDays(7), HASTA_SIEMPRE)))
                .containsEntry(VeredictoModeracion.RECHAZADO, 2L)
                .containsEntry(VeredictoModeracion.APROBADO, 1L)
                .doesNotContainKey(VeredictoModeracion.FALLIDO);

        // Con "hasta": solo lo viejo.
        assertThat(contar(registroRepository.contarPorVeredicto(DESDE_SIEMPRE, ahora.minusDays(7))))
                .containsEntry(VeredictoModeracion.APROBADO, 1L)
                .containsEntry(VeredictoModeracion.FALLIDO, 1L)
                .doesNotContainKey(VeredictoModeracion.RECHAZADO);
    }

    @Test
    void contarRevisadosIgnoraLosNoRevisadosYAgrupaPorAciertoHumano() {
        registroRepository.save(revisado(VeredictoModeracion.RECHAZADO, true));
        registroRepository.save(revisado(VeredictoModeracion.RECHAZADO, true));
        registroRepository.save(revisado(VeredictoModeracion.RECHAZADO, false));
        registroRepository.save(revisado(VeredictoModeracion.APROBADO, true));
        // Sin revisar: no debe entrar a la matriz.
        registroRepository.save(registro(VeredictoModeracion.APROBADO, ahora));

        List<Object[]> filas = registroRepository.contarRevisadosPorVeredictoYAcierto(DESDE_SIEMPRE, HASTA_SIEMPRE);

        // Solo hay 3 combinaciones presentes: (RECHAZADO,true), (RECHAZADO,false), (APROBADO,true).
        assertThat(filas).hasSize(3);
        assertThat(buscar(filas, VeredictoModeracion.RECHAZADO, true)).isEqualTo(2L);
        assertThat(buscar(filas, VeredictoModeracion.RECHAZADO, false)).isEqualTo(1L);
        assertThat(buscar(filas, VeredictoModeracion.APROBADO, true)).isEqualTo(1L);
        // Total revisado = 4 (el quinto registro, sin feedback, queda fuera).
        assertThat(filas.stream().mapToLong(fila -> ((Number) fila[2]).longValue()).sum()).isEqualTo(4L);
    }

    @Test
    void contarRevisadosTambienAceptaRangoDeFechas() {
        registroRepository.save(revisado(VeredictoModeracion.RECHAZADO, true));
        registroRepository.save(revisado(VeredictoModeracion.APROBADO, false, ahora.minusDays(30)));

        List<Object[]> filas = registroRepository
                .contarRevisadosPorVeredictoYAcierto(ahora.minusDays(7), HASTA_SIEMPRE);

        assertThat(filas).hasSize(1);
        assertThat(buscar(filas, VeredictoModeracion.RECHAZADO, true)).isEqualTo(1L);
    }

    private Map<VeredictoModeracion, Long> contar(List<Object[]> filas) {
        Map<VeredictoModeracion, Long> conteo = new EnumMap<>(VeredictoModeracion.class);
        for (Object[] fila : filas) {
            conteo.put((VeredictoModeracion) fila[0], ((Number) fila[1]).longValue());
        }
        return conteo;
    }

    private long buscar(List<Object[]> filas, VeredictoModeracion veredicto, boolean acierto) {
        return filas.stream()
                .filter(fila -> fila[0] == veredicto && Boolean.valueOf(acierto).equals(fila[1]))
                .mapToLong(fila -> ((Number) fila[2]).longValue())
                .findFirst()
                .orElse(0L);
    }

    private RegistroModeracion registro(VeredictoModeracion veredicto, LocalDateTime createdAt) {
        RegistroModeracion registro = new RegistroModeracion();
        registro.setTipo(TipoModeracion.TEXTO);
        registro.setFuente(FuenteModeracion.PUBLICACION);
        registro.setVeredicto(veredicto);
        registro.setCreatedAt(createdAt);
        return registro;
    }

    private RegistroModeracion revisado(VeredictoModeracion veredicto, boolean acierto) {
        return revisado(veredicto, acierto, ahora);
    }

    private RegistroModeracion revisado(VeredictoModeracion veredicto, boolean acierto, LocalDateTime createdAt) {
        RegistroModeracion registro = registro(veredicto, createdAt);
        registro.setAciertoHumano(acierto);
        return registro;
    }
}
