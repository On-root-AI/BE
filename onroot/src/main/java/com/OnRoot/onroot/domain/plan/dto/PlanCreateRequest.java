package com.OnRoot.onroot.domain.plan.dto;

import java.time.LocalDate;

public record PlanCreateRequest(
        String title,
        String category,
        LocalDate targetDate,
        String memo,
        Long examScheduleId
) {}
