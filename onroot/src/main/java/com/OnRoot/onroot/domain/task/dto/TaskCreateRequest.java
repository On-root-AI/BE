package com.OnRoot.onroot.domain.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank String title,
        @NotNull LocalDate scheduledDate,
        int orderIndex
) {}
