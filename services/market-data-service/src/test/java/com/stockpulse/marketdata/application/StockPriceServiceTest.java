package com.stockpulse.marketdata.application;

import com.stockpulse.marketdata.domain.StockPriceDto;
import com.stockpulse.marketdata.infrastructure.client.AlphaVantageClient;
import com.stockpulse.marketdata.infrastructure.kafka.StockPriceProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockPriceServiceTest {

    @Mock private AlphaVantageClient alphaVantageClient;
    @Mock private StockPriceProducer producer;
    @Mock private StockPriceRepository repository;

    private StockPriceService service;

    @BeforeEach
    void setUp() {
        service = new StockPriceService(
                alphaVantageClient, producer, repository,
                List.of("AAPL", "MSFT")
        );
    }

    @Test
    void fetchAndPublish_processesAllSymbols() {
        StockPriceDto aaplDto = aDto("AAPL");
        StockPriceDto msftDto = aDto("MSFT");
        when(alphaVantageClient.fetchQuote("AAPL")).thenReturn(Optional.of(aaplDto));
        when(alphaVantageClient.fetchQuote("MSFT")).thenReturn(Optional.of(msftDto));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.fetchAndPublish();

        verify(repository, times(2)).save(any());
        verify(producer).publish(aaplDto);
        verify(producer).publish(msftDto);
    }

    @Test
    void processSymbol_skipsPublishWhenClientReturnsEmpty() {
        when(alphaVantageClient.fetchQuote("AAPL")).thenReturn(Optional.empty());

        service.processSymbol("AAPL");

        verifyNoInteractions(repository, producer);
    }

    @Test
    void processSymbol_continuesAfterUnexpectedException() {
        when(alphaVantageClient.fetchQuote("AAPL")).thenThrow(new RuntimeException("timeout"));

        // must not propagate
        service.processSymbol("AAPL");

        verifyNoInteractions(repository, producer);
    }

    @Test
    void processSymbol_savesCorrectEntityFields() {
        StockPriceDto dto = aDto("GOOGL");
        when(alphaVantageClient.fetchQuote("GOOGL")).thenReturn(Optional.of(dto));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processSymbol("GOOGL");

        verify(repository).save(argThat(entity ->
                entity.getSymbol().equals("GOOGL") &&
                entity.getPrice().compareTo(dto.price()) == 0 &&
                entity.getId() == null  // DB assigns the id
        ));
    }

    private StockPriceDto aDto(String symbol) {
        return new StockPriceDto(symbol, new BigDecimal("100.00"), 500_000L,
                new BigDecimal("0.50"), OffsetDateTime.now(), "ALPHA_VANTAGE");
    }
}
