package com.ssergionp.taskmanagerapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskRequestDTO {

    private String title;
    private String description;
    private LocalDate dueDate;
}
