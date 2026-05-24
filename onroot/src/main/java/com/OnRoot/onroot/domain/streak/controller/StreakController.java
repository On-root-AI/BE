package com.OnRoot.onroot.domain.streak.controller;

import com.OnRoot.onroot.domain.streak.dto.StreakResponse;
import com.OnRoot.onroot.domain.streak.service.StreakService;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Streak", description = "학습 스트릭 조회 API")
@RestController
@RequestMapping("/api/streaks")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;

    @Operation(
            summary = "스트릭 조회",
            description = "현재 사용자의 학습 스트릭 정보를 반환합니다.\n\n" +
                    "- `currentStreak`: 현재 연속 학습 일수\n" +
                    "- `lastActivityDate`: 마지막 학습 활동 날짜\n\n" +
                    "스트릭 기록이 없는 경우 `currentStreak: 0`으로 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"인증이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping
    public StreakResponse getStreak() {
        return streakService.getStreak(getCurrentUser());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
