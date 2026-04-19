package com.stockpulse.marketdata.application;

import com.stockpulse.marketdata.domain.StockPrice;
import com.stockpulse.marketdata.domain.StockPriceDto;
import com.stockpulse.marketdata.infrastructure.client.AlphaVantageClient;
import com.stockpulse.marketdata.infrastructure.kafka.StockPriceProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the recurring stock-price fetch cycle:
 * <ol>
 *   <li>Fetch a live quote from Alpha Vantage for each configured symbol.</li>
 *   <li>Persist the quote to the {@code stock_prices} table via JPA.</li>
 *   <li>Publish the quote to the Kafka {@code stock.prices} topic.</li>
 * </ol>
 *
 * <p>The schedule is driven by {@code app.scheduling.stock-fetch-cron} and the
 * symbol list by {@code app.stocks.symbols} (comma-separated).
 */
@Service
public class StockPriceService {

    private static final Logger log = LoggerFactory.getLogger(StockPriceService.class);

    private final AlphaVantageClient alphaVantageClient;
    private final StockPriceProducer producer;
    private final StockPriceRepository repository;
    private final List<String> symbols;

    public StockPriceService(
            AlphaVantageClient alphaVantageClient,
            StockPriceProducer producer,
            StockPriceRepository repository,
            @Value("${app.stocks.symbols}") List<String> symbols) {
        this.alphaVantageClient = alphaVantageClient;
        this.producer = producer;
        this.repository = repository;
        this.symbols = symbols;
    }

    /**
     * Triggered on the cron schedule defined by {@code app.scheduling.stock-fetch-cron}.
     * Iterates over all configured symbols and processes each one independently so a
     * single failure does not abort the remaining symbols.
     */
    @Scheduled(cron = "${app.scheduling.stock-fetch-cron}")
    public void fetchAndPublish() {
        log.info("Starting scheduled stock-price fetch for {} symbol(s)", symbols.size());
        symbols.forEach(this::processSymbol);
    }

    /**
     * Fetches, persists, and publishes a quote for a single symbol.
     * Any exception is caught and logged so the scheduler loop continues.
     *
     * @param symbol ticker symbol to process
     */
    public void processSymbol(String symbol) {
        try {
            alphaVantageClient.fetchQuote(symbol).ifPresentOrElse(
                    dto -> {
                        save(dto);
                        producer.publish(dto);
                        log.debug("Processed quote for {}: {}", symbol, dto.price());
                    },
                    () -> log.warn("No quote returned for symbol {}", symbol)
            );
        } catch (Exception e) {
            log.error("Unhandled error processing symbol {}: {}", symbol, e.getMessage(), e);
        }
    }

    private void save(StockPriceDto dto) {
        StockPrice entity = new StockPrice(
                null,
                dto.symbol(),
                dto.price(),
                dto.volume(),
                dto.changePct(),
                dto.fetchedAt(),
                dto.source()
        );
        repository.save(entity);
    }
}
