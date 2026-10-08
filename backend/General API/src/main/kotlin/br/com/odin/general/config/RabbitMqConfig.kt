package br.com.odin.general.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig(
    @Value("\${odin.events.exchange}") private val exchangeName: String,
    @Value("\${odin.events.notification-queue}") private val notificationQueueName: String,
) {
    @Bean
    fun odinEventsExchange(): TopicExchange = TopicExchange(exchangeName, true, false)

    @Bean
    fun notificationJobsQueue(): Queue = Queue(notificationQueueName, true)

    @Bean
    fun notificationJobsBinding(
        odinEventsExchange: TopicExchange,
        notificationJobsQueue: Queue,
    ): Binding =
        BindingBuilder
            .bind(notificationJobsQueue)
            .to(odinEventsExchange)
            .with("notification.#")
}
