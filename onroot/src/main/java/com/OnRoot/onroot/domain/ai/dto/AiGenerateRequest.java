package com.OnRoot.onroot.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "AI 학습 계획 생성 요청")
public record AiGenerateRequest(
        @Schema(description = "사용자 학습 요청 내용 (시험명, 목표 기간, 가용 시간 등 자유 입력)",
                example = "정처기 필기 6개월 안에 합격하고 싶어, 평일 2시간 주말 5시간 가능해")
        @NotBlank String userInput
) {}
