package com.stockpulse.portfolio.application;

import com.stockpulse.portfolio.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUserId(String userId);
}
