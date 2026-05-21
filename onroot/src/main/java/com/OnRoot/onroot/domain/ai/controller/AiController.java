package com.OnRoot.onroot.domain.ai.controller;

import com.OnRoot.onroot.domain.ai.dto.AiGenerateRequest;
import com.OnRoot.onroot.domain.ai.dto.AiGenerateResponse;
import com.OnRoot.onroot.domain.ai.service.AiService;
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

@Tag(name = "AI", description = "AI 학습 계획 생성 API")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(
            summary = "AI 학습 계획 생성",
            description = "userInput에서 시험명을 자동으로 추출(약어 포함)하여 DB에서 가장 가까운 시험 일정을 찾고, Gemini AI로 학습 계획(Plan + Tasks)을 생성합니다.\n\n" +
                    "**약어 예시**: 정처기 → 정보처리기사, 정산기 → 정보처리산업기사, 전기기사, 전기기능사\n\n" +
                    "시험 일정 매칭 실패 시에도 일반 학습 계획은 생성됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "계획 생성 성공"),
            @ApiResponse(responseCode = "400", description = "userInput 누락",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"userInput은 필수 입력값입니다.\"}"))),
            @ApiResponse(responseCode = "502", description = "Gemini API 호출 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"AI 서비스 호출에 실패했습니다. 잠시 후 다시 시도해주세요.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public AiGenerateResponse generate(@Valid @RequestBody AiGenerateRequest request) {
        return aiService.generate(request, getCurrentUser());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
