package com.ssergionp.taskmanagerapi.controller;

import com.ssergionp.taskmanagerapi.dto.PagedResponseDTO;
import com.ssergionp.taskmanagerapi.dto.TaskRequestDTO;
import com.ssergionp.taskmanagerapi.dto.TaskResponseDTO;
import com.ssergionp.taskmanagerapi.model.TaskStatus;
import com.ssergionp.taskmanagerapi.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Tarefas", description = "Gerenciamento de tarefas (CRUD)")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Criar uma nova tarefa")
    @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<TaskResponseDTO> criar(@Valid @RequestBody TaskRequestDTO dto) {
        TaskResponseDTO criada = taskService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @Operation(summary = "Listar todas as tarefas (paginado)")
    @GetMapping
    public ResponseEntity<PagedResponseDTO<TaskResponseDTO>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(taskService.listarTodas(page, size));
    }

    @Operation(summary = "Buscar tarefa por ID")
    @ApiResponse(responseCode = "200", description = "Tarefa encontrada")
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar uma tarefa existente")
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody TaskRequestDTO dto) {
        return ResponseEntity.ok(taskService.atualizar(id, dto));
    }

    @Operation(summary = "Remover uma tarefa")
    @ApiResponse(responseCode = "204", description = "Tarefa removida com sucesso")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        taskService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar tarefas filtradas por status (paginado)")
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponseDTO<TaskResponseDTO>> listarPorStatus(
            @PathVariable TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(taskService.listarPorStatus(status, page, size));
    }

    @Operation(summary = "Listar todas as tarefas de todos os usuários (somente ADMIN)")
    @GetMapping("/admin/all")
    public ResponseEntity<List<TaskResponseDTO>> listarTodasComoAdmin() {
        return ResponseEntity.ok(taskService.listarTodasComoAdmin());
    }
}
