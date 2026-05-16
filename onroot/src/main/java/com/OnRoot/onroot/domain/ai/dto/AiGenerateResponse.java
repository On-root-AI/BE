package com.OnRoot.onroot.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "AI 학습 계획 생성 응답")
public record AiGenerateResponse(
        @Schema(description = "AI 생성 로그 ID") Long logId,
        @Schema(description = "생성된 Plan ID") Long planId,
        @Schema(description = "계획 제목", example = "정보처리기사 필기 3개월 합격 플랜") String title,
        @Schema(description = "평일 하루 가용 학습 시간 (시간 단위)") Integer weekdayHours,
        @Schema(description = "주말 하루 가용 학습 시간 (시간 단위)") Integer weekendHours,
        @Schema(description = "생성된 학습 태스크 목록") List<TaskDto> tasks
) {
    @Schema(description = "학습 태스크")
    public record TaskDto(
            @Schema(description = "태스크 ID") Long id,
            @Schema(description = "태스크 제목", example = "데이터베이스 기초 개념 학습") String title,
            @Schema(description = "학습 예정일", example = "2026-05-20") LocalDate scheduledDate,
            @Schema(description = "순서 인덱스 (1부터 시작)") int orderIndex
    ) {}
}
