package com.stockpulse.portfolio.application;

import com.stockpulse.portfolio.domain.Portfolio;
import com.stockpulse.portfolio.domain.PortfolioSummaryDto;
import com.stockpulse.portfolio.domain.Position;
import com.stockpulse.portfolio.domain.Transaction;
import com.stockpulse.portfolio.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void createPortfolio_savesPortfolioWithCorrectFields() {
        Portfolio expected = Portfolio.builder()
                .id(1L)
                .userId("user-1")
                .name("My Portfolio")
                .description("Test description")
                .build();
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(expected);

        Portfolio result = portfolioService.createPortfolio("user-1", "My Portfolio", "Test description");

        ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioRepository).save(captor.capture());
        Portfolio captured = captor.getValue();

        assertThat(captured.getUserId()).isEqualTo("user-1");
        assertThat(captured.getName()).isEqualTo("My Portfolio");
        assertThat(captured.getDescription()).isEqualTo("Test description");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void addPosition_createsPositionAndTransactionForNewPosition() {
        Portfolio portfolio = Portfolio.builder().id(1L).userId("user-1").name("P").build();
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        when(positionRepository.findByPortfolioIdAndSymbol(1L, "AAPL")).thenReturn(Optional.empty());

        Position savedPosition = Position.builder()
                .id(10L)
                .portfolioId(1L)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .avgBuyPrice(new BigDecimal("150.00"))
                .currentPrice(new BigDecimal("150.00"))
                .lastUpdated(OffsetDateTime.now())
                .build();
        when(positionRepository.save(any(Position.class))).thenReturn(savedPosition);

        Transaction savedTransaction = Transaction.builder().id(100L).build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        Position result = portfolioService.addPosition(1L, "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        ArgumentCaptor<Position> posCaptor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(posCaptor.capture());
        Position savedPos = posCaptor.getValue();
        assertThat(savedPos.getPortfolioId()).isEqualTo(1L);
        assertThat(savedPos.getSymbol()).isEqualTo("AAPL");
        assertThat(savedPos.getQuantity()).isEqualByComparingTo("10");
        assertThat(savedPos.getAvgBuyPrice()).isEqualByComparingTo("150.00");
        assertThat(savedPos.getCurrentPrice()).isEqualByComparingTo("150.00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getPortfolioId()).isEqualTo(1L);
        assertThat(savedTx.getSymbol()).isEqualTo("AAPL");
        assertThat(savedTx.getType()).isEqualTo(TransactionType.BUY);
        assertThat(savedTx.getQuantity()).isEqualByComparingTo("10");
        assertThat(savedTx.getPrice()).isEqualByComparingTo("150.00");
        assertThat(savedTx.getTotalAmount()).isEqualByComparingTo("1500.00");

        assertThat(result).isEqualTo(savedPosition);
    }

    @Test
    void addPosition_updatesWeightedAvgBuyPriceForExistingPosition() {
        Portfolio portfolio = Portfolio.builder().id(1L).userId("user-1").name("P").build();
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));

        Position existing = Position.builder()
                .id(10L)
                .portfolioId(1L)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .avgBuyPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("110.00"))
                .lastUpdated(OffsetDateTime.now())
                .build();
        when(positionRepository.findByPortfolioIdAndSymbol(1L, "AAPL")).thenReturn(Optional.of(existing));
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        portfolioService.addPosition(1L, "AAPL", new BigDecimal("10"), new BigDecimal("200.00"));

        ArgumentCaptor<Position> posCaptor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(posCaptor.capture());
        Position updated = posCaptor.getValue();

        assertThat(updated.getQuantity()).isEqualByComparingTo("20");
        assertThat(updated.getAvgBuyPrice()).isEqualByComparingTo("150.0000");
    }

    @Test
    void getPortfolioWithPnL_computesTotalValueTotalPnLAndTotalPnLPercent() {
        Portfolio portfolio = Portfolio.builder()
                .id(1L)
                .userId("user-1")
                .name("P")
                .build();
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));

        Position pos1 = Position.builder()
                .portfolioId(1L)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .avgBuyPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("120.00"))
                .build();

        Position pos2 = Position.builder()
                .portfolioId(1L)
                .symbol("GOOG")
                .quantity(new BigDecimal("5"))
                .avgBuyPrice(new BigDecimal("200.00"))
                .currentPrice(new BigDecimal("180.00"))
                .build();

        when(positionRepository.findByPortfolioId(1L)).thenReturn(List.of(pos1, pos2));

        PortfolioSummaryDto summary = portfolioService.getPortfolioWithPnL(1L);

        assertThat(summary.portfolioId()).isEqualTo(1L);
        assertThat(summary.userId()).isEqualTo("user-1");
        assertThat(summary.positions()).hasSize(2);

        assertThat(summary.totalValue()).isEqualByComparingTo("2100.00");

        BigDecimal expectedPnL = new BigDecimal("120.00").subtract(new BigDecimal("100.00")).multiply(new BigDecimal("10"))
                .add(new BigDecimal("180.00").subtract(new BigDecimal("200.00")).multiply(new BigDecimal("5")));
        assertThat(summary.totalPnL()).isEqualByComparingTo(expectedPnL);

        BigDecimal costBasis = new BigDecimal("100.00").multiply(new BigDecimal("10"))
                .add(new BigDecimal("200.00").multiply(new BigDecimal("5")));
        BigDecimal expectedPct = expectedPnL.divide(costBasis, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        assertThat(summary.totalPnLPercent()).isEqualByComparingTo(expectedPct);
    }

    @Test
    void getPortfolioWithPnL_throwsIllegalArgumentExceptionWhenPortfolioNotFound() {
        when(portfolioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getPortfolioWithPnL(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updatePositionPrice_updatesCurrentPriceOnAllMatchingPositions() {
        Position pos1 = Position.builder()
                .id(1L)
                .portfolioId(1L)
                .symbol("TSLA")
                .quantity(new BigDecimal("5"))
                .avgBuyPrice(new BigDecimal("200.00"))
                .currentPrice(new BigDecimal("210.00"))
                .build();
        Position pos2 = Position.builder()
                .id(2L)
                .portfolioId(2L)
                .symbol("TSLA")
                .quantity(new BigDecimal("3"))
                .avgBuyPrice(new BigDecimal("190.00"))
                .currentPrice(new BigDecimal("210.00"))
                .build();

        when(positionRepository.findBySymbol("TSLA")).thenReturn(List.of(pos1, pos2));
        when(positionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        portfolioService.updatePositionPrice("TSLA", new BigDecimal("250.00"));

        assertThat(pos1.getCurrentPrice()).isEqualByComparingTo("250.00");
        assertThat(pos2.getCurrentPrice()).isEqualByComparingTo("250.00");
        assertThat(pos1.getLastUpdated()).isNotNull();
        assertThat(pos2.getLastUpdated()).isNotNull();
        verify(positionRepository).saveAll(List.of(pos1, pos2));
    }

    @Test
    void addPosition_throwsIllegalArgumentExceptionWhenPortfolioNotFound() {
        when(portfolioRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.addPosition(99L, "AAPL", BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verify(positionRepository, never()).findByPortfolioIdAndSymbol(anyLong(), anyString());
    }
}
