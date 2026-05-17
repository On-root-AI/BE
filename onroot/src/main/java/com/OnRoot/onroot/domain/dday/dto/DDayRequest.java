package com.OnRoot.onroot.domain.dday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DDayRequest(
        @NotBlank String title,
        @NotNull LocalDate targetDate
) {}
