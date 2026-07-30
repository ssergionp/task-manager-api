package com.ssergionp.taskmanagerapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequestDTO {

    @NotBlank(message = "O refresh token é obrigatório")
    private String refreshToken;
}
