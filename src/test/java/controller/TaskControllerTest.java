package com.ssergionp.taskmanagerapi.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssergionp.taskmanagerapi.dto.TaskRequestDTO;
import com.ssergionp.taskmanagerapi.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    @BeforeEach
    void limparBanco() {
        taskRepository.deleteAll();
    }

    @Test
    void deveCriarTarefaERetornar201() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setTitle("Estudar Spring Boot");
        dto.setDescription("Terminar o CRUD");
        dto.setDueDate(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/tasks")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Tarefa de teste"));
    }

    @Test
    void deveRetornar404AoBuscarTarefaInexistente() throws Exception {
        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("999")));
    }

    @Test
    void deveDeletarTarefaERetornar204() throws Exception {
        TaskRequestDTO dto = new TaskRequestDTO();
        dto.setTitle("Tarefa a deletar");
        dto.setDueDate(LocalDate.now().plusDays(1));

        String response = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/tasks/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/" + id))
                .andExpect(status().isNotFound());
    }
}
