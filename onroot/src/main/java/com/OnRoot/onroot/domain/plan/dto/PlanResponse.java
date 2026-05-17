package com.OnRoot.onroot.domain.plan.dto;

import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.plan.entity.PlanStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlanResponse(
        Long id,
        String title,
        String category,
        LocalDate targetDate,
        PlanStatus status,
        LocalDateTime createdAt
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getCategory(),
                plan.getTargetDate(),
                plan.getStatus(),
                plan.getCreatedAt()
        );
    }
}
