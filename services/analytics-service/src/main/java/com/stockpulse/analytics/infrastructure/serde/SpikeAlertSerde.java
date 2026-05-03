package com.stockpulse.analytics.infrastructure.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockpulse.analytics.domain.SpikeAlert;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

/**
 * {@link Serde} for {@link SpikeAlert} records published to
 * {@code stock.prices.spikes}. Uses Jackson JSON with ISO-8601 date/time formatting.
 */
public class SpikeAlertSerde implements Serde<SpikeAlert> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final InnerSerializer serializer = new InnerSerializer();
    private final InnerDeserializer deserializer = new InnerDeserializer();

    @Override
    public Serializer<SpikeAlert> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<SpikeAlert> deserializer() {
        return deserializer;
    }

    public static final class InnerSerializer implements Serializer<SpikeAlert> {
        @Override
        public byte[] serialize(String topic, SpikeAlert data) {
            if (data == null) return null;
            try {
                return MAPPER.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize SpikeAlert", e);
            }
        }
    }

    public static final class InnerDeserializer implements Deserializer<SpikeAlert> {
        @Override
        public SpikeAlert deserialize(String topic, byte[] data) {
            if (data == null) return null;
            try {
                return MAPPER.readValue(data, SpikeAlert.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize SpikeAlert", e);
            }
        }
    }
}
