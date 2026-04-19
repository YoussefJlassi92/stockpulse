package com.stockpulse.marketdata.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the Alpha Vantage REST API.
 * Bound from the {@code app.alpha-vantage} prefix in application.yaml.
 */
@ConfigurationProperties(prefix = "app.alpha-vantage")
public record AlphaVantageProperties(String apiKey, String baseUrl) {}
