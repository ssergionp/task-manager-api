package com.ssergionp.taskmanagerapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssergionp.taskmanagerapi.model.AuthProvider;
import com.ssergionp.taskmanagerapi.model.RefreshToken;
import com.ssergionp.taskmanagerapi.model.Role;
import com.ssergionp.taskmanagerapi.model.User;
import com.ssergionp.taskmanagerapi.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        User user = userRepository.findByUsername(email)
                .orElseGet(() -> criarUsuarioGoogle(email));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities("ROLE_" + user.getRole().name())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.criarRefreshToken(user);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", accessToken);
        body.put("refreshToken", refreshToken.getToken());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private User criarUsuarioGoogle(String email) {
        User user = new User();
        user.setUsername(email);
        user.setPassword(null);
        user.setRole(Role.USER);
        user.setAuthProvider(AuthProvider.GOOGLE);
        return userRepository.save(user);
    }
}
