package com.OnRoot.onroot.domain.examschedule.dto;

import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;

import java.time.LocalDate;

public record ExamScheduleResponse(
        Long id,
        String examName,
        String subject,
        LocalDate applicationStart,
        LocalDate applicationEnd,
        LocalDate examDate,
        LocalDate resultDate
) {
    public static ExamScheduleResponse from(ExamSchedule examSchedule) {
        return new ExamScheduleResponse(
                examSchedule.getId(),
                examSchedule.getExamName(),
                examSchedule.getSubject(),
                examSchedule.getApplicationStart(),
                examSchedule.getApplicationEnd(),
                examSchedule.getExamDate(),
                examSchedule.getResultDate()
        );
    }
}
