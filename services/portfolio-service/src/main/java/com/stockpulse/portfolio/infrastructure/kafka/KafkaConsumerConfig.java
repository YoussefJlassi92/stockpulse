package com.stockpulse.portfolio.infrastructure.kafka;

import com.stockpulse.portfolio.domain.StockPriceEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Explicit Kafka listener container configuration for the portfolio-service consumer.
 *
 * <h2>Why not rely solely on application.yaml?</h2>
 * <p>Spring Boot auto-configures a {@link ConcurrentKafkaListenerContainerFactory} from
 * {@code spring.kafka.*} properties. Overriding it here lets us set consumer-level
 * properties that are not exposed as first-class Spring Boot properties without losing
 * the rest of the auto-configured values (deserializers, trusted packages, etc.).
 *
 * <h2>Key settings</h2>
 * <dl>
 *   <dt>{@code AckMode.MANUAL}</dt>
 *   <dd>Offsets are committed only when {@code Acknowledgment#acknowledge()} is called
 *       explicitly. This pairs with the error strategy in {@link StockPriceConsumer} to
 *       prevent silent message loss on processing failures.</dd>
 *
 *   <dt>{@code max.poll.records = 10}</dt>
 *   <dd>Limits the number of records returned in a single {@code poll()} call. Lower values
 *       reduce the batch size committed per acknowledge cycle, which shrinks the replay
 *       window if a consumer crashes mid-batch.</dd>
 *
 *   <dt>{@code max.poll.interval.ms = 30000}</dt>
 *   <dd>If the consumer does not call {@code poll()} again within 30 seconds, the broker
 *       considers it dead and triggers a partition rebalance. With manual ack and a
 *       potentially slow DB write, keeping this value low ensures stalled consumers are
 *       evicted quickly so another instance can take over.</dd>
 * </dl>
 */
@Configuration
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ConsumerFactory<String, StockPriceEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 30_000);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockPriceEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, StockPriceEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, StockPriceEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
