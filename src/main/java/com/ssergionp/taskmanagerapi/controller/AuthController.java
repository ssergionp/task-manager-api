package com.ssergionp.taskmanagerapi.controller;

import com.ssergionp.taskmanagerapi.dto.AuthRequestDTO;
import com.ssergionp.taskmanagerapi.dto.AuthResponseDTO;
import com.ssergionp.taskmanagerapi.dto.RefreshRequestDTO;
import com.ssergionp.taskmanagerapi.exception.InvalidRefreshTokenException;
import com.ssergionp.taskmanagerapi.model.RefreshToken;
import com.ssergionp.taskmanagerapi.model.Role;
import com.ssergionp.taskmanagerapi.model.User;
import com.ssergionp.taskmanagerapi.repository.UserRepository;
import com.ssergionp.taskmanagerapi.security.JwtService;
import com.ssergionp.taskmanagerapi.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Registro, login e renovação de token")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(summary = "Registrar novo usuário")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody AuthRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(gerarResposta(user));
    }

    @Operation(summary = "Login e geração de token JWT + refresh token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return ResponseEntity.ok(gerarResposta(user));
    }

    private AuthResponseDTO gerarResposta(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.criarRefreshToken(user);

        return new AuthResponseDTO(accessToken, refreshToken.getToken());
    }

    @Operation(summary = "Gerar novo access token a partir de um refresh token válido")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        RefreshToken refreshToken = refreshTokenService.buscarPorToken(dto.getRefreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token não encontrado"));

        if (!refreshTokenService.isValido(refreshToken)) {
            throw new InvalidRefreshTokenException("Refresh token expirado ou revogado");
        }

        User user = refreshToken.getUser();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();

        String novoAccessToken = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponseDTO(novoAccessToken, refreshToken.getToken()));
    }
}
