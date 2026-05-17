package com.OnRoot.onroot.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 메시지", example = "오류 메시지") String message
) {}
