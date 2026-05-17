package com.OnRoot.onroot.domain.dday.dto;

import com.OnRoot.onroot.domain.dday.entity.DDay;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record DDayResponse(
        Long id,
        String title,
        LocalDate targetDate,
        long dDay,
        LocalDateTime createdAt
) {
    public static DDayResponse from(DDay dday) {
        long dDayValue = ChronoUnit.DAYS.between(LocalDate.now(), dday.getTargetDate());
        return new DDayResponse(
                dday.getId(),
                dday.getTitle(),
                dday.getTargetDate(),
                dDayValue,
                dday.getCreatedAt()
        );
    }
}
