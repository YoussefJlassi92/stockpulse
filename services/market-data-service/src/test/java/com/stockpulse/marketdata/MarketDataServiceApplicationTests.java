package com.stockpulse.marketdata;

import com.stockpulse.marketdata.application.StockPriceRepository;
import com.stockpulse.marketdata.application.StockPriceService;
import com.stockpulse.marketdata.domain.StockPriceDto;
import com.stockpulse.marketdata.infrastructure.client.AlphaVantageClient;
import com.stockpulse.marketdata.infrastructure.client.AlphaVantageProperties;
import com.stockpulse.marketdata.infrastructure.kafka.StockPriceProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: verifies the Spring context assembles and all key beans are wired.
 * Infrastructure beans (Kafka, JPA) are excluded via test application.yaml
 * and replaced with mocks to satisfy constructor injection.
 */
@SpringBootTest
class MarketDataServiceApplicationTests {

    @MockitoBean
    KafkaTemplate<String, StockPriceDto> kafkaTemplate;

    @MockitoBean
    StockPriceRepository stockPriceRepository;

    @Autowired
    StockPriceService stockPriceService;

    @Autowired
    AlphaVantageClient alphaVantageClient;

    @Autowired
    StockPriceProducer stockPriceProducer;

    @Autowired
    AlphaVantageProperties alphaVantageProperties;

    @Test
    void contextLoads() {
        assertThat(stockPriceService).isNotNull();
        assertThat(alphaVantageClient).isNotNull();
        assertThat(stockPriceProducer).isNotNull();
    }

    @Test
    void alphaVantagePropertiesAreLoaded() {
        assertThat(alphaVantageProperties.apiKey()).isNotBlank();
        assertThat(alphaVantageProperties.baseUrl()).isNotBlank();
    }
}
