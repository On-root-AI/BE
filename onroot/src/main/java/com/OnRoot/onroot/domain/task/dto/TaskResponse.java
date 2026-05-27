package com.OnRoot.onroot.domain.task.dto;

import com.OnRoot.onroot.domain.task.entity.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        Long planId,
        String title,
        LocalDate scheduledDate,
    LocalDateTime completedAt,
    int orderIndex,
    String memo
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getPlan().getId(),
                task.getTitle(),
                task.getScheduledDate(),
        task.getCompletedAt(),
        task.getOrderIndex(),
        task.getMemo()
        );
    }
}
