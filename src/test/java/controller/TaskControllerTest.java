package com.ssergionp.taskmanagerapi.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssergionp.taskmanagerapi.dto.TaskRequestDTO;
import com.ssergionp.taskmanagerapi.model.Role;
import com.ssergionp.taskmanagerapi.model.User;
import com.ssergionp.taskmanagerapi.repository.TaskRepository;
import com.ssergionp.taskmanagerapi.repository.UserRepository;
import com.ssergionp.taskmanagerapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // cria um usuário de teste e gera um token válido para usar nas requisições
        User user = new User();
        user.setUsername("usuario_teste");
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setRole(Role.USER);
        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();

        token = jwtService.generateToken(userDetails);
    }

    @Test
    void deveCriarTarefaERetornar201() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setTitle("Estudar Spring Boot");
        dto.setDescription("Terminar o CRUD");
        dto.setDueDate(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Estudar Spring Boot"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void deveRetornar400AoCriarTarefaComTituloVazio() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setTitle("");
        dto.setDescription("Descrição qualquer");

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("título")));
    }

    @Test
    void deveListarTarefasCriadas() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setTitle("Tarefa de teste");
        dto.setDueDate(LocalDate.now().plusDays(1));

        mockMvc.perform(post("/tasks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));

        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Tarefa de teste"));
    }

    @Test
    void deveRetornar404AoBuscarTarefaInexistente() throws Exception {
        mockMvc.perform(get("/tasks/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("999")));
    }

    @Test
    void deveDeletarTarefaERetornar204() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setTitle("Tarefa a deletar");
        dto.setDueDate(LocalDate.now().plusDays(1));

        String response = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/tasks/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized());
    }
}
