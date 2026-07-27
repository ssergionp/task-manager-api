package com.ssergionp.taskmanagerapi.service;

import com.ssergionp.taskmanagerapi.dto.TaskRequestDTO;
import com.ssergionp.taskmanagerapi.dto.TaskResponseDTO;
import com.ssergionp.taskmanagerapi.exception.TaskNotFoundException;
import com.ssergionp.taskmanagerapi.model.Task;
import com.ssergionp.taskmanagerapi.model.TaskStatus;
import com.ssergionp.taskmanagerapi.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private TaskRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setTitle("Estudar Spring Boot");
        task.setDescription("Terminar o CRUD");
        task.setStatus(TaskStatus.TODO);
        task.setCreatedAt(LocalDateTime.now());
        task.setDueDate(LocalDate.now().plusDays(5));

        requestDTO = new TaskRequestDTO();
        requestDTO.setTitle("Estudar Spring Boot");
        requestDTO.setDescription("Terminar o CRUD");
        requestDTO.setDueDate(LocalDate.now().plusDays(5));
    }

    @Test
    void deveCriarTarefaComSucesso() {
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponseDTO resultado = taskService.criar(requestDTO);

        assertThat(resultado.getTitle()).isEqualTo("Estudar Spring Boot");
        assertThat(resultado.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void deveListarTodasAsTarefas() {
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<TaskResponseDTO> resultado = taskService.listarTodas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitle()).isEqualTo("Estudar Spring Boot");
    }

    @Test
    void deveBuscarTarefaPorIdComSucesso() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponseDTO resultado = taskService.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoAoBuscarTarefaInexistente() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.buscarPorId(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deveAtualizarTarefaComSucesso() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskRequestDTO novoDto = new TaskRequestDTO();
        novoDto.setTitle("Título atualizado");
        novoDto.setDescription("Descrição atualizada");
        novoDto.setDueDate(LocalDate.now().plusDays(10));

        TaskResponseDTO resultado = taskService.atualizar(1L, novoDto);

        assertThat(resultado).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void deveDeletarTarefaComSucesso() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deletar(1L);

        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void deveLancarExcecaoAoDeletarTarefaInexistente() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deletar(99L))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).deleteById(any());
    }
}
