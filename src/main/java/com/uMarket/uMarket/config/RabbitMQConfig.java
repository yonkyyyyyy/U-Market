package com.uMarket.uMarket.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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
	public static final String COLA_TAREAS_PESADAS = "tareas.pesadas";
	public static final String ROUTING_TAREA_PESADA = "tarea.pesada";

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