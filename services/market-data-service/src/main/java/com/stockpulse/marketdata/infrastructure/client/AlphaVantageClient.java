package com.stockpulse.marketdata.infrastructure.client;

import com.stockpulse.marketdata.domain.StockPriceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for the Alpha Vantage GLOBAL_QUOTE endpoint.
 *
 * <p>Returns {@link Optional#empty()} on any HTTP or parsing error so callers
 * can continue processing remaining symbols without interruption.
 */
@Component
public class AlphaVantageClient {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);

    private static final String FUNCTION = "GLOBAL_QUOTE";
    private static final String QUOTE_KEY = "Global Quote";
    private static final String PRICE_KEY = "05. price";
    private static final String VOLUME_KEY = "06. volume";
    private static final String CHANGE_PCT_KEY = "10. change percent";

    private final RestClient restClient;
    private final AlphaVantageProperties properties;

    public AlphaVantageClient(AlphaVantageProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    /**
     * Fetches a real-time quote for the given stock symbol.
     *
     * @param symbol ticker symbol (e.g. "AAPL")
     * @return the parsed quote, or {@link Optional#empty()} if the call fails or
     *         the response contains no usable data
     */
    public Optional<StockPriceDto> fetchQuote(String symbol) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uri -> uri.path("/query")
                            .queryParam("function", FUNCTION)
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(Map.class);

            return parseQuote(symbol, response);

        } catch (RestClientException e) {
            log.warn("HTTP error fetching quote for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching quote for {}", symbol, e);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    Optional<StockPriceDto> parseQuote(String symbol, Map<String, Object> response) {
        if (response == null || !response.containsKey(QUOTE_KEY)) {
            log.warn("Empty or unexpected response for symbol {}", symbol);
            return Optional.empty();
        }

        Map<String, String> quote = (Map<String, String>) response.get(QUOTE_KEY);

        String rawPrice = quote.get(PRICE_KEY);
        if (rawPrice == null || rawPrice.isBlank()) {
            log.warn("Missing price in Alpha Vantage response for {}", symbol);
            return Optional.empty();
        }

        try {
            BigDecimal price = new BigDecimal(rawPrice.trim());
            Long volume = quote.containsKey(VOLUME_KEY)
                    ? Long.parseLong(quote.get(VOLUME_KEY).trim())
                    : null;
            BigDecimal changePct = parseChangePct(symbol, quote.get(CHANGE_PCT_KEY));

            return Optional.of(new StockPriceDto(
                    symbol,
                    price,
                    volume,
                    changePct,
                    OffsetDateTime.now(),
                    "ALPHA_VANTAGE"
            ));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse numeric fields for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private BigDecimal parseChangePct(String symbol, String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            // Alpha Vantage returns values like "1.23%" — strip the percent sign
            return new BigDecimal(raw.trim().replace("%", ""));
        } catch (NumberFormatException e) {
            log.warn("Could not parse change_pct '{}' for {}", raw, symbol);
            return null;
        }
    }
}
