package com.stockpulse.analytics.infrastructure.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockpulse.analytics.domain.StockPriceEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

/**
 * {@link Serde} for {@link StockPriceEvent} backed by Jackson JSON.
 *
 * <p>Uses inner {@link InnerSerializer} and {@link InnerDeserializer} classes so each
 * can be used independently when constructing a {@code TestInputTopic} or
 * {@code TestOutputTopic} in unit tests without instantiating the full {@code Serde}.
 *
 * <p>A shared, pre-configured {@link ObjectMapper} with {@link JavaTimeModule} is used
 * so that {@link java.time.OffsetDateTime} fields round-trip correctly via ISO-8601.
 */
public class StockPriceEventSerde implements Serde<StockPriceEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final InnerSerializer serializer = new InnerSerializer();
    private final InnerDeserializer deserializer = new InnerDeserializer();

    @Override
    public Serializer<StockPriceEvent> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<StockPriceEvent> deserializer() {
        return deserializer;
    }

    public static final class InnerSerializer implements Serializer<StockPriceEvent> {
        @Override
        public byte[] serialize(String topic, StockPriceEvent data) {
            if (data == null) return null;
            try {
                return MAPPER.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize StockPriceEvent", e);
            }
        }
    }

    public static final class InnerDeserializer implements Deserializer<StockPriceEvent> {
        @Override
        public StockPriceEvent deserialize(String topic, byte[] data) {
            if (data == null) return null;
            try {
                return MAPPER.readValue(data, StockPriceEvent.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize StockPriceEvent", e);
            }
        }
    }
}
