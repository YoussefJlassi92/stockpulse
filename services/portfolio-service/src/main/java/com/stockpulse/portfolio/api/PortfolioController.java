package com.stockpulse.portfolio.api;

import com.stockpulse.portfolio.application.PortfolioService;
import com.stockpulse.portfolio.domain.Portfolio;
import com.stockpulse.portfolio.domain.PortfolioSummaryDto;
import com.stockpulse.portfolio.domain.Position;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/user/{userId}")
    public List<Portfolio> getPortfoliosByUserId(@PathVariable String userId) {
        return portfolioService.getPortfoliosByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Portfolio createPortfolio(@RequestBody CreatePortfolioRequest request) {
        return portfolioService.createPortfolio(request.userId(), request.name(), request.description());
    }

    @GetMapping("/{portfolioId}/summary")
    public PortfolioSummaryDto getPortfolioSummary(@PathVariable Long portfolioId) {
        return portfolioService.getPortfolioWithPnL(portfolioId);
    }

    @GetMapping("/{portfolioId}/positions")
    public List<Position> getPositions(@PathVariable Long portfolioId) {
        return portfolioService.getPositions(portfolioId);
    }

    @PostMapping("/{portfolioId}/positions")
    @ResponseStatus(HttpStatus.CREATED)
    public Position addPosition(
            @PathVariable Long portfolioId,
            @RequestBody AddPositionRequest request) {
        return portfolioService.addPosition(portfolioId, request.symbol(), request.quantity(), request.price());
    }
}
