package com.stockpulse.analytics.infrastructure.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockpulse.analytics.domain.PriceAnalytics;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

/**
 * {@link Serde} for {@link PriceAnalytics} records published to
 * {@code stock.prices.analytics}. Uses Jackson JSON with ISO-8601 date/time formatting.
 */
public class PriceAnalyticsSerde implements Serde<PriceAnalytics> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final InnerSerializer serializer = new InnerSerializer();
    private final InnerDeserializer deserializer = new InnerDeserializer();

    @Override
    public Serializer<PriceAnalytics> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<PriceAnalytics> deserializer() {
        return deserializer;
    }

    public static final class InnerSerializer implements Serializer<PriceAnalytics> {
        @Override
        public byte[] serialize(String topic, PriceAnalytics data) {
            if (data == null) return null;
            try {
                return MAPPER.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize PriceAnalytics", e);
            }
        }
    }

    public static final class InnerDeserializer implements Deserializer<PriceAnalytics> {
        @Override
        public PriceAnalytics deserialize(String topic, byte[] data) {
            if (data == null) return null;
            try {
                return MAPPER.readValue(data, PriceAnalytics.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize PriceAnalytics", e);
            }
        }
    }
}
