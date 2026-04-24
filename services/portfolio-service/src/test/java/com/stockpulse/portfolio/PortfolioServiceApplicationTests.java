package com.stockpulse.portfolio;

import com.stockpulse.portfolio.api.PortfolioController;
import com.stockpulse.portfolio.application.PortfolioRepository;
import com.stockpulse.portfolio.application.PortfolioService;
import com.stockpulse.portfolio.application.PositionRepository;
import com.stockpulse.portfolio.application.TransactionRepository;
import com.stockpulse.portfolio.domain.StockPriceEvent;
import com.stockpulse.portfolio.infrastructure.kafka.StockPriceConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: verifies the Spring context assembles and all key beans are wired.
 * Infrastructure beans (JPA, Kafka, Flyway) are excluded via test application.yaml
 * and replaced with mocks to satisfy constructor injection.
 */
@SpringBootTest
class PortfolioServiceApplicationTests {

    @MockitoBean
    PortfolioRepository portfolioRepository;

    @MockitoBean
    PositionRepository positionRepository;

    @MockitoBean
    TransactionRepository transactionRepository;

    @MockitoBean
    KafkaTemplate<String, StockPriceEvent> kafkaTemplate;

    @Autowired
    PortfolioService portfolioService;

    @Autowired
    StockPriceConsumer stockPriceConsumer;

    @Autowired
    PortfolioController portfolioController;

    @Test
    void contextLoads() {
        assertThat(portfolioService).isNotNull();
        assertThat(stockPriceConsumer).isNotNull();
        assertThat(portfolioController).isNotNull();
    }
}
