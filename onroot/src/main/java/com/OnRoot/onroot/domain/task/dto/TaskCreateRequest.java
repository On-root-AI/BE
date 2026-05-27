package com.OnRoot.onroot.domain.task.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskCreateRequest(
        @NotBlank String title,
        @NotNull LocalDate scheduledDate,
        int orderIndex,
        String memo
) {}
