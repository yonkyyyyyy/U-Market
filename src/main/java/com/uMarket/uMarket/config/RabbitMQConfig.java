package com.uMarket.uMarket.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String EXCHANGE_TAREAS = "umarket.exchange";

	// --- Cola de la Tarea 1 (demo) -------------------------------------------------
	// Se conserva tal cual (no se le cambian los argumentos para no romper brokers
	// donde ya exista). El pipeline real usa la cola de moderación de abajo.
	public static final String COLA_TAREAS_PESADAS = "tareas.pesadas";
	public static final String ROUTING_TAREA_PESADA = "tarea.pesada";

	// --- Pipeline de moderación de productos (Tarea 4) -----------------------------
	public static final String COLA_MODERACION = "productos.moderacion";
	public static final String ROUTING_MODERACION = "producto.moderacion";

	// Dead Letter: a dónde van los mensajes que fallan tras agotar los reintentos.
	public static final String EXCHANGE_DLX = "umarket.dlx";
	public static final String COLA_DLQ = "productos.moderacion.dlq";
	public static final String ROUTING_DLQ = "productos.moderacion.dlq";

	@Bean
	public TopicExchange tareasExchange() {
		// exchange durable: sobrevive reinicios del broker
		return new TopicExchange(EXCHANGE_TAREAS, true, false);
	}

	@Bean
	public Queue colaTareasPesadas() {
		// queue durable: los mensajes no se pierden si RabbitMQ se reinicia
		return new Queue(COLA_TAREAS_PESADAS, true);
	}

	@Bean
	public Binding bindingTareasPesadas(Queue colaTareasPesadas, TopicExchange tareasExchange) {
		return BindingBuilder.bind(colaTareasPesadas).to(tareasExchange).with(ROUTING_TAREA_PESADA);
	}

	/**
	 * Exchange (direct) para los mensajes "muertos". Va aparte del exchange de tareas
	 * porque la DLQ se enruta por clave exacta, no por patrón de topic.
	 */
	@Bean
	public DirectExchange deadLetterExchange() {
		return new DirectExchange(EXCHANGE_DLX, true, false);
	}

	/**
	 * Cola principal del pipeline de moderación. Si un mensaje se rechaza sin
	 * reencolar (p. ej. al agotar los reintentos de application.yaml), RabbitMQ
	 * lo manda al {@code dead-letter-exchange} en vez de perderlo.
	 */
	@Bean
	public Queue colaModeracion() {
		return QueueBuilder.durable(COLA_MODERACION)
				.deadLetterExchange(EXCHANGE_DLX)
				.deadLetterRoutingKey(ROUTING_DLQ)
				.build();
	}

	@Bean
	public Binding bindingModeracion(Queue colaModeracion, TopicExchange tareasExchange) {
		return BindingBuilder.bind(colaModeracion).to(tareasExchange).with(ROUTING_MODERACION);
	}

	/**
	 * Cola de mensajes muertos: aquí quedan las tareas que no se pudieron procesar,
	 * para poder inspeccionarlas y reintentarlas a mano cuando el servicio vuelva.
	 */
	@Bean
	public Queue colaDlq() {
		return QueueBuilder.durable(COLA_DLQ).build();
	}

	@Bean
	public Binding bindingDlq(Queue colaDlq, DirectExchange deadLetterExchange) {
		return BindingBuilder.bind(colaDlq).to(deadLetterExchange).with(ROUTING_DLQ);
	}

	/**
	 * Spring AMQP 4.x usa Jackson 3. El converter agrega un header __TypeId__
	 * para que el worker convierta el JSON de vuelta al DTO correcto.
	 * Sólo se confían los DTO del proyecto (evita el trust de paquetes arbitrarios).
	 */
	@Bean
	public MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter("com.uMarket.uMarket.dto");
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(messageConverter);
		return template;
	}
}
