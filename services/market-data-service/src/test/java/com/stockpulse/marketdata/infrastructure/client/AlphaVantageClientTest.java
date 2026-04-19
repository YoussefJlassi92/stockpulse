package com.stockpulse.marketdata.infrastructure.client;

import com.stockpulse.marketdata.domain.StockPriceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AlphaVantageClientTest {

    private AlphaVantageClient client;

    @BeforeEach
    void setUp() {
        client = new AlphaVantageClient(new AlphaVantageProperties("demo", "https://www.alphavantage.co"));
    }

    @Test
    void parseQuote_returnsEmptyWhenResponseIsNull() {
        assertThat(client.parseQuote("AAPL", null)).isEmpty();
    }

    @Test
    void parseQuote_returnsEmptyWhenQuoteKeyMissing() {
        Map<String, Object> response = Map.of("Note", "API rate limit reached");
        assertThat(client.parseQuote("AAPL", response)).isEmpty();
    }

    @Test
    void parseQuote_parsesPriceVolumeAndChangePct() {
        Map<String, Object> response = Map.of(
                "Global Quote", Map.of(
                        "01. symbol", "AAPL",
                        "05. price", "173.5700",
                        "06. volume", "52345678",
                        "10. change percent", "1.2345%"
                )
        );

        Optional<StockPriceDto> result = client.parseQuote("AAPL", response);

        assertThat(result).isPresent();
        StockPriceDto dto = result.get();
        assertThat(dto.symbol()).isEqualTo("AAPL");
        assertThat(dto.price()).isEqualByComparingTo(new BigDecimal("173.5700"));
        assertThat(dto.volume()).isEqualTo(52_345_678L);
        assertThat(dto.changePct()).isEqualByComparingTo(new BigDecimal("1.2345"));
        assertThat(dto.source()).isEqualTo("ALPHA_VANTAGE");
    }

    @Test
    void parseQuote_returnsEmptyWhenPriceBlank() {
        Map<String, Object> response = Map.of(
                "Global Quote", Map.of("05. price", "  ")
        );
        assertThat(client.parseQuote("AAPL", response)).isEmpty();
    }

    @Test
    void parseQuote_handlesNullVolumeAndChangePct() {
        Map<String, Object> response = Map.of(
                "Global Quote", Map.of("05. price", "62.10")
        );

        Optional<StockPriceDto> result = client.parseQuote("BNP.PA", response);

        assertThat(result).isPresent();
        assertThat(result.get().volume()).isNull();
        assertThat(result.get().changePct()).isNull();
    }

    @Test
    void parseQuote_stripsPercentSignFromChangePct() {
        Map<String, Object> response = Map.of(
                "Global Quote", Map.of(
                        "05. price", "100.00",
                        "10. change percent", "-0.5678%"
                )
        );

        Optional<StockPriceDto> result = client.parseQuote("MSFT", response);

        assertThat(result).isPresent();
        assertThat(result.get().changePct()).isEqualByComparingTo(new BigDecimal("-0.5678"));
    }
}
