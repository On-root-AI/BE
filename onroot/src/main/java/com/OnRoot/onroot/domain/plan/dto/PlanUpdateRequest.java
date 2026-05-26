package com.OnRoot.onroot.domain.plan.dto;

import java.time.LocalDate;

import com.OnRoot.onroot.domain.plan.entity.PlanStatus;

public record PlanUpdateRequest(
        String title,
        String category,
        LocalDate targetDate,
        String memo,
        PlanStatus status
) {}
