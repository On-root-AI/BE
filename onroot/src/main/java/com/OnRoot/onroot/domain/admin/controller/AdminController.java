package com.OnRoot.onroot.domain.admin.controller;

import com.OnRoot.onroot.domain.dday.dto.DDayResponse;
import com.OnRoot.onroot.domain.dday.repository.DDayRepository;
import com.OnRoot.onroot.domain.plan.dto.PlanResponse;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.domain.streak.dto.StreakResponse;
import com.OnRoot.onroot.domain.streak.repository.StreakRepository;
import com.OnRoot.onroot.domain.task.dto.TaskResponse;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.domain.user.dto.UserResponse;
import com.OnRoot.onroot.domain.user.repository.UserRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin", description = "전체 데이터 조회 API (관리자 전용)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final DDayRepository ddayRepository;
    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final StreakRepository streakRepository;

    public record WithUser<T>(Long userId, T data) {}

    @Operation(summary = "전체 유저 조회", description = "모든 유저 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"인증이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Operation(summary = "전체 D-Day 조회", description = "모든 유저의 D-Day 목록을 userId와 함께 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"인증이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/ddays")
    public List<WithUser<DDayResponse>> getAllDDays() {
        return ddayRepository.findAll().stream()
                .map(d -> new WithUser<>(d.getUser().getId(), DDayResponse.from(d)))
                .toList();
    }

    @Operation(summary = "전체 Plan 조회", description = "모든 유저의 플랜 목록을 userId와 함께 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"인증이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/plans")
    public List<WithUser<PlanResponse>> getAllPlans() {
        return planRepository.findAll().stream()
                .map(p -> new WithUser<>(p.getUser().getId(), PlanResponse.from(p)))
                .toList();
    }

    @Operation(summary = "전체 Task 조회", description = "모든 유저의 태스크 목록을 userId와 함께 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"인증이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/tasks")
    public List<WithUser<TaskResponse>> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(t -> new WithUser<>(t.getPlan().getUser().getId(), TaskResponse.from(t)))
                .toList();
    }

    @Operation(summary = "전체 Streak 조회", description = "모든 유저의 스트릭 정보를 userId와 함께 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"인증이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/streaks")
    public List<WithUser<StreakResponse>> getAllStreaks() {
        return streakRepository.findAll().stream()
                .map(s -> new WithUser<>(s.getUser().getId(), StreakResponse.from(s)))
                .toList();
    }
}
