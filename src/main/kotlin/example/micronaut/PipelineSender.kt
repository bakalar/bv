package example.micronaut

import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaKey
import io.micronaut.configuration.kafka.annotation.Topic

/**
 * Notify that a book needs to be processed asynchronously.
 * <p>
 *     It is implemented by sending a message to a "books" Kafka topic.
 * <p>
 */
@KafkaClient(acks = KafkaClient.Acknowledge.ALL)
interface PipelineSender {

    @Topic("books")
    fun sendBook(@KafkaKey isbn: String, body: String)
}
