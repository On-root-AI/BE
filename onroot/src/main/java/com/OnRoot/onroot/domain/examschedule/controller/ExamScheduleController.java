package com.OnRoot.onroot.domain.examschedule.controller;

import com.OnRoot.onroot.domain.examschedule.dto.ExamScheduleResponse;
import com.OnRoot.onroot.domain.examschedule.service.ExamScheduleService;
import com.OnRoot.onroot.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ExamSchedule", description = "Q-Net 시험 일정 조회/동기화 API")
@RestController
@RequestMapping("/api/exam-schedules")
@RequiredArgsConstructor
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    @Operation(
            summary = "시험 일정 전체 조회",
            description = "저장된 모든 Q-Net 시험 일정을 반환합니다."
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
    public List<ExamScheduleResponse> getAll() {
        return examScheduleService.getAll();
    }

    @Operation(
            summary = "시험 일정 Q-Net 동기화",
            description = "Q-Net API를 호출하여 최신 시험 일정을 DB에 동기화합니다. 기존 데이터를 삭제 후 새로 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "동기화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"인증이 필요합니다.\"}"))),
            @ApiResponse(responseCode = "502", description = "Q-Net API 호출 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"외부 서비스 호출에 실패했습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sync() {
        examScheduleService.sync();
    }
}
