package com.ssergionp.taskmanagerapi.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.ssergionp.taskmanagerapi.model.AuthProvider;
import com.ssergionp.taskmanagerapi.model.RefreshToken;
import com.ssergionp.taskmanagerapi.model.Role;
import com.ssergionp.taskmanagerapi.model.User;
import com.ssergionp.taskmanagerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private OAuth2User oAuth2User;

    private OAuth2LoginSuccessHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EMAIL_TESTE = "usuario.google@teste.com";

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginSuccessHandler(userRepository, jwtService, refreshTokenService);
    }

    @Test
    void deveCriarNovoUsuarioQuandoEmailNaoExisteAindaERetornarTokens() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL_TESTE);
        when(userRepository.findByUsername(EMAIL_TESTE)).thenReturn(Optional.empty());

        User novoUsuario = new User();
        novoUsuario.setId(1L);
        novoUsuario.setUsername(EMAIL_TESTE);
        novoUsuario.setRole(Role.USER);
        novoUsuario.setAuthProvider(AuthProvider.GOOGLE);

        when(userRepository.save(any(User.class))).thenReturn(novoUsuario);
        when(jwtService.generateToken(any())).thenReturn("access-token-fake");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token-fake");
        when(refreshTokenService.criarRefreshToken(any(User.class))).thenReturn(refreshToken);

        Authentication authentication = new UsernamePasswordAuthenticationToken(oAuth2User, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        // confirma que um novo usuario foi criado com AuthProvider GOOGLE e sem senha
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals(EMAIL_TESTE)
                        && user.getAuthProvider() == AuthProvider.GOOGLE
                        && user.getPassword() == null
        ));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("token").asText()).isEqualTo("access-token-fake");
        assertThat(body.get("refreshToken").asText()).isEqualTo("refresh-token-fake");
    }

    @Test
    void deveReaproveitarUsuarioExistenteQuandoEmailJaEstaCadastrado() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL_TESTE);

        User usuarioExistente = new User();
        usuarioExistente.setId(5L);
        usuarioExistente.setUsername(EMAIL_TESTE);
        usuarioExistente.setRole(Role.USER);
        usuarioExistente.setAuthProvider(AuthProvider.GOOGLE);

        when(userRepository.findByUsername(EMAIL_TESTE)).thenReturn(Optional.of(usuarioExistente));
        when(jwtService.generateToken(any())).thenReturn("access-token-existente");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token-existente");
        when(refreshTokenService.criarRefreshToken(usuarioExistente)).thenReturn(refreshToken);

        Authentication authentication = new UsernamePasswordAuthenticationToken(oAuth2User, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        // nao deve criar um novo usuario, ja que o email ja existe
        verify(userRepository, never()).save(any(User.class));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("token").asText()).isEqualTo("access-token-existente");
        assertThat(body.get("refreshToken").asText()).isEqualTo("refresh-token-existente");
    }
}
