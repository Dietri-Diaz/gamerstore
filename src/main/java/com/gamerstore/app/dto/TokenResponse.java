package com.gamerstore.app.dto;

// Par de tokens (access + refresh) devuelto tras login o refresh
public record TokenResponse(String accessToken, String refreshToken) {}
