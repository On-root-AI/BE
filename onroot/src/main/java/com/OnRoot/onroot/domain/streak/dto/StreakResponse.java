package com.OnRoot.onroot.domain.streak.dto;

import com.OnRoot.onroot.domain.streak.entity.Streak;
import java.time.LocalDate;

public record StreakResponse(
        Long id,
        int currentStreak,
        LocalDate lastActivityDate
) {
    public static StreakResponse from(Streak streak) {
        return new StreakResponse(streak.getId(), streak.getCurrentStreak(), streak.getLastActivityDate());
    }
}
