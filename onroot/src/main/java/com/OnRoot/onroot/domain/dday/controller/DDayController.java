package com.OnRoot.onroot.domain.dday.controller;

import com.OnRoot.onroot.domain.dday.dto.DDayRequest;
import com.OnRoot.onroot.domain.dday.dto.DDayResponse;
import com.OnRoot.onroot.domain.dday.service.DDayService;
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

@Tag(name = "D-Day", description = "D-Day 생성/조회/수정/삭제 API")
@RestController
@RequestMapping("/api/ddays")
@RequiredArgsConstructor
public class DDayController {

    private final DDayService ddayService;

    @Operation(
            summary = "D-Day 생성",
            description = "새로운 D-Day를 등록합니다.\n\n" +
                    "- `title`: D-Day 제목 (필수)\n" +
                    "- `targetDate`: 목표 날짜 (필수, yyyy-MM-dd)\n\n" +
                    "응답의 `dDay` 필드는 오늘로부터 목표일까지 남은 일수입니다. 양수면 미래, 음수면 이미 지난 날짜입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "D-Day 생성 성공"),
            @ApiResponse(responseCode = "400", description = "필수 값 누락",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"title은 필수 입력값입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DDayResponse createDDay(@Valid @RequestBody DDayRequest request) {
        return ddayService.createDDay(request, getCurrentUser());
    }

    @Operation(
            summary = "D-Day 목록 조회",
            description = "현재 사용자의 모든 D-Day를 목표일 오름차순으로 반환합니다. `dDay` 값은 매 조회 시 현재 날짜 기준으로 재계산됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping
    public List<DDayResponse> getDDays() {
        return ddayService.getDDays(getCurrentUser());
    }

    @Operation(
            summary = "D-Day 수정",
            description = "D-Day의 제목과 목표일을 수정합니다. 두 필드 모두 필수입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "필수 값 누락",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"title은 필수 입력값입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "D-Day를 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"해당 D-Day를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{ddayId}")
    public DDayResponse updateDDay(@PathVariable Long ddayId, @Valid @RequestBody DDayRequest request) {
        return ddayService.updateDDay(ddayId, request, getCurrentUser());
    }

    @Operation(
            summary = "D-Day 삭제",
            description = "D-Day를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "D-Day를 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"해당 D-Day를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/{ddayId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDDay(@PathVariable Long ddayId) {
        ddayService.deleteDDay(ddayId, getCurrentUser());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
