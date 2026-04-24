package com.stockpulse.portfolio.application;

import com.stockpulse.portfolio.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByPortfolioId(Long portfolioId);

    List<Position> findBySymbol(String symbol);

    Optional<Position> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);
}
