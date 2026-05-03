package com.stockpulse.analytics.infrastructure.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.analytics.domain.PriceWindow;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

/**
 * {@link Serde} for {@link PriceWindow}, the internal aggregation accumulator stored
 * in the Kafka Streams window state store. Not published externally.
 */
public class PriceWindowSerde implements Serde<PriceWindow> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Serializer<PriceWindow> serializer() {
        return (topic, data) -> {
            if (data == null) return null;
            try {
                return MAPPER.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize PriceWindow", e);
            }
        };
    }

    @Override
    public Deserializer<PriceWindow> deserializer() {
        return (topic, data) -> {
            if (data == null) return null;
            try {
                return MAPPER.readValue(data, PriceWindow.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize PriceWindow", e);
            }
        };
    }
}
