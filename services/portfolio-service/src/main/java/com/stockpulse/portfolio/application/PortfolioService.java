package com.stockpulse.portfolio.application;

import com.stockpulse.portfolio.domain.Portfolio;
import com.stockpulse.portfolio.domain.PortfolioSummaryDto;
import com.stockpulse.portfolio.domain.Position;
import com.stockpulse.portfolio.domain.PositionDto;
import com.stockpulse.portfolio.domain.Transaction;
import com.stockpulse.portfolio.domain.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Core business logic for managing portfolios, positions, and transactions.
 *
 * <p>All mutating operations are transactional. P&amp;L calculations are performed
 * in-memory using the current prices stored on each {@link Position}.
 */
@Service
@Transactional
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;

    public PortfolioService(
            PortfolioRepository portfolioRepository,
            PositionRepository positionRepository,
            TransactionRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Creates and persists a new portfolio for the given user.
     *
     * @param userId      the owner's user identifier
     * @param name        the display name of the portfolio
     * @param description an optional description
     * @return the saved {@link Portfolio}
     */
    public Portfolio createPortfolio(String userId, String name, String description) {
        Portfolio portfolio = Portfolio.builder()
                .userId(userId)
                .name(name)
                .description(description)
                .build();
        Portfolio saved = portfolioRepository.save(portfolio);
        log.debug("Created portfolio id={} for userId={}", saved.getId(), userId);
        return saved;
    }

    /**
     * Adds or updates a position in the given portfolio and records the BUY transaction.
     *
     * <p>If a position for the same symbol already exists, the average buy price is
     * recalculated as a weighted average of the existing and incoming quantities.
     *
     * @param portfolioId the target portfolio
     * @param symbol      the ticker symbol
     * @param quantity    the number of shares being purchased
     * @param price       the price per share at the time of purchase
     * @return the created or updated {@link Position}
     * @throws IllegalArgumentException if the portfolio does not exist
     */
    public Position addPosition(Long portfolioId, String symbol, BigDecimal quantity, BigDecimal price) {
        portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        Position position = positionRepository.findByPortfolioIdAndSymbol(portfolioId, symbol)
                .map(existing -> {
                    BigDecimal existingCost = existing.getAvgBuyPrice().multiply(existing.getQuantity());
                    BigDecimal newCost = price.multiply(quantity);
                    BigDecimal totalQuantity = existing.getQuantity().add(quantity);
                    BigDecimal weightedAvg = existingCost.add(newCost)
                            .divide(totalQuantity, 4, RoundingMode.HALF_UP);
                    existing.setQuantity(totalQuantity);
                    existing.setAvgBuyPrice(weightedAvg);
                    existing.setLastUpdated(OffsetDateTime.now());
                    log.debug("Updated position portfolioId={} symbol={} newQty={} newAvg={}",
                            portfolioId, symbol, totalQuantity, weightedAvg);
                    return existing;
                })
                .orElseGet(() -> {
                    Position newPosition = Position.builder()
                            .portfolioId(portfolioId)
                            .symbol(symbol)
                            .quantity(quantity)
                            .avgBuyPrice(price)
                            .currentPrice(price)
                            .lastUpdated(OffsetDateTime.now())
                            .build();
                    log.debug("Created new position portfolioId={} symbol={}", portfolioId, symbol);
                    return newPosition;
                });

        Position savedPosition = positionRepository.save(position);

        Transaction transaction = Transaction.builder()
                .portfolioId(portfolioId)
                .symbol(symbol)
                .type(TransactionType.BUY)
                .quantity(quantity)
                .price(price)
                .totalAmount(quantity.multiply(price))
                .build();
        transactionRepository.save(transaction);

        return savedPosition;
    }

    /**
     * Builds a {@link PortfolioSummaryDto} with current P&amp;L figures for the given portfolio.
     *
     * <p>Total value is the sum of {@code currentPrice * quantity} for every position.
     * Total P&amp;L percentage is {@code totalPnL / totalCostBasis * 100}, guarded against
     * division by zero.
     *
     * @param portfolioId the portfolio to summarise
     * @return a populated {@link PortfolioSummaryDto}
     * @throws IllegalArgumentException if the portfolio does not exist
     */
    @Transactional(readOnly = true)
    public PortfolioSummaryDto getPortfolioWithPnL(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        List<Position> positions = positionRepository.findByPortfolioId(portfolioId);

        List<PositionDto> positionDtos = positions.stream()
                .map(p -> new PositionDto(
                        p.getSymbol(),
                        p.getQuantity(),
                        p.getAvgBuyPrice(),
                        p.getCurrentPrice(),
                        p.calculatePnL(),
                        p.calculatePnLPercent()))
                .toList();

        BigDecimal totalValue = positions.stream()
                .filter(p -> p.getCurrentPrice() != null)
                .map(p -> p.getCurrentPrice().multiply(p.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnL = positions.stream()
                .map(Position::calculatePnL)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCostBasis = positions.stream()
                .map(p -> p.getAvgBuyPrice().multiply(p.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnLPercent = totalCostBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalPnL.divide(totalCostBasis, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        return new PortfolioSummaryDto(
                portfolio.getId(),
                portfolio.getUserId(),
                portfolio.getName(),
                totalValue,
                totalPnL,
                totalPnLPercent,
                positionDtos);
    }

    /**
     * Updates the current price of all positions for the given symbol.
     *
     * @param symbol   the ticker symbol
     * @param newPrice the latest market price
     */
    public void updatePositionPrice(String symbol, BigDecimal newPrice) {
        List<Position> positions = positionRepository.findBySymbol(symbol);
        if (positions.isEmpty()) {
            log.debug("No positions found for symbol={}, skipping price update", symbol);
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        positions.forEach(p -> {
            p.setCurrentPrice(newPrice);
            p.setLastUpdated(now);
        });
        positionRepository.saveAll(positions);
        log.debug("Updated currentPrice={} for {} position(s) with symbol={}", newPrice, positions.size(), symbol);
    }

    /**
     * Returns all portfolios belonging to the given user.
     *
     * @param userId the user identifier
     * @return list of portfolios, may be empty
     */
    @Transactional(readOnly = true)
    public List<Portfolio> getPortfoliosByUserId(String userId) {
        return portfolioRepository.findByUserId(userId);
    }

    /**
     * Returns all positions in the given portfolio.
     *
     * @param portfolioId the portfolio identifier
     * @return list of positions, may be empty
     */
    @Transactional(readOnly = true)
    public List<Position> getPositions(Long portfolioId) {
        return positionRepository.findByPortfolioId(portfolioId);
    }
}
