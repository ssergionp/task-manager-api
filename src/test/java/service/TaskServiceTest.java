package com.ssergionp.taskmanagerapi.service;

import com.ssergionp.taskmanagerapi.dto.TaskRequestDTO;
import com.ssergionp.taskmanagerapi.dto.TaskResponseDTO;
import com.ssergionp.taskmanagerapi.exception.TaskNotFoundException;
import com.ssergionp.taskmanagerapi.model.Role;
import com.ssergionp.taskmanagerapi.model.Task;
import com.ssergionp.taskmanagerapi.model.TaskStatus;
import com.ssergionp.taskmanagerapi.model.User;
import com.ssergionp.taskmanagerapi.repository.TaskRepository;
import com.ssergionp.taskmanagerapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private TaskRequestDTO requestDTO;
    private User usuarioLogado;

    @BeforeEach
    void setUp() {
        usuarioLogado = new User();
        usuarioLogado.setId(1L);
        usuarioLogado.setUsername("usuario_teste");
        usuarioLogado.setRole(Role.USER);

        // simula um usuário autenticado no contexto de segurança,
        // já que o TaskService consulta SecurityContextHolder para saber quem está logado
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(usuarioLogado.getUsername(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        task = new Task();
        task.setId(1L);
        task.setTitle("Estudar Spring Boot");
        task.setDescription("Terminar o CRUD");
        task.setStatus(TaskStatus.TODO);
        task.setCreatedAt(LocalDateTime.now());
        task.setDueDate(LocalDate.now().plusDays(5));
        task.setOwner(usuarioLogado);

        requestDTO = new TaskRequestDTO();
        requestDTO.setTitle("Estudar Spring Boot");
        requestDTO.setDescription("Terminar o CRUD");
        requestDTO.setDueDate(LocalDate.now().plusDays(5));
    }

    @AfterEach
    void tearDown() {
        // limpa o contexto de segurança para não vazar estado entre os testes
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveCriarTarefaComSucesso() {
        when(userRepository.findByUsername("usuario_teste")).thenReturn(Optional.of(usuarioLogado));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponseDTO resultado = taskService.criar(requestDTO);

        assertThat(resultado.getTitle()).isEqualTo("Estudar Spring Boot");
        assertThat(resultado.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void deveListarTodasAsTarefas() {
        when(userRepository.findByUsername("usuario_teste")).thenReturn(Optional.of(usuarioLogado));
        when(taskRepository.findByOwner(usuarioLogado)).thenReturn(List.of(task));

        List<TaskResponseDTO> resultado = taskService.listarTodas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitle()).isEqualTo("Estudar Spring Boot");
    }

    @Test
    void deveBuscarTarefaPorIdComSucesso() {
        when(userRepository.findByUsername("usuario_teste")).thenReturn(Optional.of(usuarioLogado));
        when(taskRepository.findByIdAndOwner(1L, usuarioLogado)).thenReturn(Optional.of(task));

        TaskResponseDTO resultado = taskService.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoAoBuscarTarefaInexistente() {
        when(userRepository.findByUsername("usuario_teste")).thenReturn(Optional.of(usuarioLogado));
        when(taskRepository.findByIdAndOwner(99L, usuarioLogado)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.buscarPorId(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deveAtualizarTarefaComSucesso() {
        when(userRepository.findByUsername("usuario_teste")).thenReturn(Optional.of(usuarioLogado));
        when(taskRepository.findByIdAndOwner(1L, usuarioLogado)).thenReturn(Optional.of(task));
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
        when(userRepository.findByUsername("usuario_teste")).thenReturn(Optional.of(usuarioLogado));
        when(taskRepository.findByIdAndOwner(1L, usuarioLogado)).thenReturn(Optional.of(task));

        taskService.deletar(1L);

        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    void deveLancarExcecaoAoDeletarTarefaInexistente() {
        when(userRepository.findByUsername("usuario_teste")).thenReturn(Optional.of(usuarioLogado));
        when(taskRepository.findByIdAndOwner(99L, usuarioLogado)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deletar(99L))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).delete(any());
    }
}
