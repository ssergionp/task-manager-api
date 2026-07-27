package com.ssergionp.taskmanagerapi.controller;

import com.ssergionp.taskmanagerapi.dto.TaskRequestDTO;
import com.ssergionp.taskmanagerapi.dto.TaskResponseDTO;
import com.ssergionp.taskmanagerapi.model.TaskStatus;
import com.ssergionp.taskmanagerapi.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> criar(@Valid @RequestBody TaskRequestDTO dto) {
        TaskResponseDTO criada = taskService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> listarTodas() {
        return ResponseEntity.ok(taskService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody TaskRequestDTO dto) {
        return ResponseEntity.ok(taskService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        taskService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskResponseDTO>> listarPorStatus(@PathVariable TaskStatus status) {
        return ResponseEntity.ok(taskService.listarPorStatus(status));
    }
}
