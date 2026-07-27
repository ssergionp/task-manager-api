package com.ssergionp.taskmanagerapi.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskRequestDTO {

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
    private String title;

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
    private String description;

    @FutureOrPresent(message = "A data de vencimento não pode estar no passado")
    private LocalDate dueDate;
}
