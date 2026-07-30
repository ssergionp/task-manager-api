package com.ssergionp.taskmanagerapi.repository;

import com.ssergionp.taskmanagerapi.model.RefreshToken;
import com.ssergionp.taskmanagerapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
