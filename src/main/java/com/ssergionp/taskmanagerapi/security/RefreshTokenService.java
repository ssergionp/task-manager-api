package com.ssergionp.taskmanagerapi.security;

import com.ssergionp.taskmanagerapi.model.RefreshToken;
import com.ssergionp.taskmanagerapi.model.User;
import com.ssergionp.taskmanagerapi.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMillis;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken criarRefreshToken(User user) {
        // remove qualquer refresh token anterior desse usuário
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusNanos(refreshExpirationMillis * 1_000_000));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> buscarPorToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isValido(RefreshToken refreshToken) {
        return !refreshToken.isRevoked() && refreshToken.getExpiryDate().isAfter(LocalDateTime.now());
    }

    public void revogar(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}
