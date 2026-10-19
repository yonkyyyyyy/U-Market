package com.uMarket.uMarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Habilita la ejecución asíncrona (@Async) para que los listeners
 * de eventos no bloqueen el hilo principal (p. ej. el @Scheduled
 * de expiración de demandas o el request HTTP que dispara un match).
 *
 * <p>Alternativa válida: poner {@code @EnableAsync} directamente en
 * {@code UMarketApplication}. Se prefiere esta clase dedicada para
 * poder tunear el pool de hilos y aislar estas tareas del executor
 * por defecto de Spring.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Pool dedicado a las alertas internas. El listener lo referencia
	 * con {@code @Async("alertaExecutor")}.
	 */
	@Bean(name = "alertaExecutor")
	public Executor alertaExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("alertas-async-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.initialize();
		return executor;
	}
}
