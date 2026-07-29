package com.ssergionp.taskmanagerapi.controller;

import com.ssergionp.taskmanagerapi.dto.AuthRequestDTO;
import com.ssergionp.taskmanagerapi.dto.AuthResponseDTO;
import com.ssergionp.taskmanagerapi.model.Role;
import com.ssergionp.taskmanagerapi.model.User;
import com.ssergionp.taskmanagerapi.repository.UserRepository;
import com.ssergionp.taskmanagerapi.security.JwtService;
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
@Tag(name = "Autenticação", description = "Registro e login de usuários")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Operation(summary = "Registrar novo usuário")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody AuthRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();

        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponseDTO(token));
    }

    @Operation(summary = "Login e geração de token JWT")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();

        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(new AuthResponseDTO(token));
    }
}
