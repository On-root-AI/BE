package com.OnRoot.onroot.domain.task.controller;

import com.OnRoot.onroot.domain.task.dto.TaskCreateRequest;
import com.OnRoot.onroot.domain.task.dto.TaskResponse;
import com.OnRoot.onroot.domain.task.dto.TaskUpdateRequest;
import com.OnRoot.onroot.domain.task.service.TaskService;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Task", description = "학습 태스크 생성/조회/수정/완료/삭제 API")
@RestController
@RequestMapping("/api/plans/{planId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "태스크 생성",
            description = "특정 플랜에 태스크를 추가합니다.\n\n" +
                    "- `title`: 태스크 제목 (필수)\n" +
                    "- `scheduledDate`: 예정일 (필수, yyyy-MM-dd)\n" +
                    "- `orderIndex`: 순서 인덱스 (0부터 시작)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "태스크 생성 성공"),
            @ApiResponse(responseCode = "400", description = "필수 값 누락",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"title은 필수 입력값입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "플랜을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"해당 플랜을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable Long planId, @Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(planId, request, getCurrentUser());
    }

    @Operation(
            summary = "태스크 목록 조회",
            description = "특정 플랜에 속한 모든 태스크를 orderIndex 오름차순으로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "플랜을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"해당 플랜을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping
    public List<TaskResponse> getTasks(@PathVariable Long planId) {
        return taskService.getTasks(planId, getCurrentUser());
    }

    @Operation(
            summary = "태스크 수정",
            description = "태스크의 제목, 예정일, 순서를 부분 수정합니다. 보내지 않은 필드는 기존 값을 유지합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "플랜 또는 태스크를 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"해당 태스크를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{taskId}")
    public TaskResponse updateTask(@PathVariable Long planId, @PathVariable Long taskId,
                                   @RequestBody TaskUpdateRequest request) {
        return taskService.updateTask(planId, taskId, request, getCurrentUser());
    }

    @Operation(
            summary = "태스크 완료 처리",
            description = "태스크를 완료 상태로 변경합니다. 완료 시각(`completedAt`)이 현재 시간으로 기록됩니다.\n\n" +
                    "이미 완료된 태스크에 다시 호출하면 완료 시각이 갱신됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료 처리 성공"),
            @ApiResponse(responseCode = "404", description = "플랜 또는 태스크를 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"해당 태스크를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{taskId}/complete")
    public TaskResponse completeTask(@PathVariable Long planId, @PathVariable Long taskId) {
        return taskService.completeTask(planId, taskId, getCurrentUser());
    }

    @Operation(
            summary = "태스크 삭제",
            description = "태스크를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "플랜 또는 태스크를 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"해당 태스크를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long planId, @PathVariable Long taskId) {
        taskService.deleteTask(planId, taskId, getCurrentUser());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
