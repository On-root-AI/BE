package com.OnRoot.onroot.domain.plan.dto;

import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.plan.entity.PlanStatus;
import com.OnRoot.onroot.domain.task.dto.TaskResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PlanDetailResponse(
        Long id,
        String title,
        String category,
        LocalDate targetDate,
        PlanStatus status,
        LocalDateTime createdAt,
        List<TaskResponse> tasks
) {
    public static PlanDetailResponse from(Plan plan, List<TaskResponse> tasks) {
        return new PlanDetailResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getCategory(),
                plan.getTargetDate(),
                plan.getStatus(),
                plan.getCreatedAt(),
                tasks
        );
    }
}
