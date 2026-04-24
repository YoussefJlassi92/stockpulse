package com.stockpulse.portfolio.api;

public record CreatePortfolioRequest(
        String userId,
        String name,
        String description
) {}
