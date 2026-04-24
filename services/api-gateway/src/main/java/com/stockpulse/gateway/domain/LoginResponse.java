package com.stockpulse.gateway.domain;

public record LoginResponse(String token, String userId, long expiresIn) {}
