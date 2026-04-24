package com.stockpulse.portfolio.application;

import com.stockpulse.portfolio.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByPortfolioIdOrderByExecutedAtDesc(Long portfolioId);
}
