package com.ssergionp.taskmanagerapi.service;

import com.ssergionp.taskmanagerapi.dto.TaskRequestDTO;
import com.ssergionp.taskmanagerapi.dto.TaskResponseDTO;
import com.ssergionp.taskmanagerapi.exception.TaskNotFoundException;
import com.ssergionp.taskmanagerapi.model.Task;
import com.ssergionp.taskmanagerapi.model.TaskStatus;
import com.ssergionp.taskmanagerapi.model.User;
import com.ssergionp.taskmanagerapi.repository.TaskRepository;
import com.ssergionp.taskmanagerapi.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private User getUsuarioLogado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário autenticado não encontrado"));
    }

    public TaskResponseDTO criar(TaskRequestDTO dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());
        task.setOwner(getUsuarioLogado());

        Task salva = taskRepository.save(task);
        return toResponseDTO(salva);
    }

    public List<TaskResponseDTO> listarTodas() {
        User usuario = getUsuarioLogado();
        return taskRepository.findByOwner(usuario)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public TaskResponseDTO buscarPorId(Long id) {
        User usuario = getUsuarioLogado();
        Task task = taskRepository.findByIdAndOwner(id, usuario)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return toResponseDTO(task);
    }

    public TaskResponseDTO atualizar(Long id, TaskRequestDTO dto) {
        User usuario = getUsuarioLogado();
        Task task = taskRepository.findByIdAndOwner(id, usuario)
                .orElseThrow(() -> new TaskNotFoundException(id));

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());

        Task atualizada = taskRepository.save(task);
        return toResponseDTO(atualizada);
    }

    public void deletar(Long id) {
        User usuario = getUsuarioLogado();
        Task task = taskRepository.findByIdAndOwner(id, usuario)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(task);
    }

    public List<TaskResponseDTO> listarPorStatus(TaskStatus status) {
        User usuario = getUsuarioLogado();
        return taskRepository.findByOwnerAndStatus(usuario, status)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private TaskResponseDTO toResponseDTO(Task task) {
        return new TaskResponseDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getDueDate()
        );
    }
}
