package com.OnRoot.onroot.domain.plan.dto;

import com.OnRoot.onroot.domain.plan.entity.PlanStatus;
import java.time.LocalDate;

public record PlanUpdateRequest(
        String title,
        String category,
        LocalDate targetDate,
        PlanStatus status
) {}
