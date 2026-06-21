package com.fitness.activityservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${rabbitmq.queue.name}")
    private String queue;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Value("${rabbitmq.dead-letter.exchange}")
    private String deadLetterExchange;

    @Value("${rabbitmq.dead-letter.queue}")
    private String deadLetterQueue;

    @Value("${rabbitmq.dead-letter.routing-key}")
    private String deadLetterRoutingKey;

    @Bean
    public Queue activityQueue() {
        return QueueBuilder.durable(queue)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    @Bean
    public DirectExchange activityExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public DirectExchange activityDeadLetterExchange() {
        return new DirectExchange(deadLetterExchange);
    }

    @Bean
    public Binding activityBinding(Queue activityQueue, DirectExchange activityExchange) {
        return BindingBuilder.bind(activityQueue).to(activityExchange).with(routingKey);
    }

    @Bean
    public Queue activityDeadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    @Bean
    public Binding activityDeadLetterBinding(Queue activityDeadLetterQueue, DirectExchange activityDeadLetterExchange) {
        return BindingBuilder.bind(activityDeadLetterQueue).to(activityDeadLetterExchange).with(deadLetterRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
