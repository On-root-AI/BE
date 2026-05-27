package com.OnRoot.onroot.domain.task.dto;

import java.time.LocalDate;

public record TaskUpdateRequest(
        String title,
        LocalDate scheduledDate,
        Integer orderIndex,
        String memo
) {}
