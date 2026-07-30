package com.ssergionp.taskmanagerapi.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssergionp.taskmanagerapi.dto.AuthRequestDTO;
import com.ssergionp.taskmanagerapi.dto.RefreshRequestDTO;
import com.ssergionp.taskmanagerapi.repository.RefreshTokenRepository;
import com.ssergionp.taskmanagerapi.repository.TaskRepository;
import com.ssergionp.taskmanagerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private AuthRequestDTO criarCredenciais() {
        AuthRequestDTO dto = new AuthRequestDTO();
        dto.setUsername("usuario_auth_teste");
        dto.setPassword("senha123");
        return dto;
    }

    @Test
    void deveRegistrarERetornarTokenERefreshToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarCredenciais())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void deveLogarERetornarTokenERefreshToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criarCredenciais())));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarCredenciais())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void deveGerarNovoAccessTokenComRefreshTokenValido() throws Exception {
        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarCredenciais())))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(response).get("refreshToken").asText();

        RefreshRequestDTO refreshDto = new RefreshRequestDTO();
        refreshDto.setRefreshToken(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    void deveRetornar401AoUsarRefreshTokenInexistente() throws Exception {
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken("token-que-nao-existe");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("não encontrado")));
    }

    @Test
    void deveRevogarRefreshTokenNoLogout() throws Exception {
        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarCredenciais())))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(response).get("refreshToken").asText();

        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken(refreshToken);

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        // depois do logout, o mesmo refresh token não deve mais funcionar
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("revogado")));
    }
}
